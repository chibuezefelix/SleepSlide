package com.opxl.sleepslide.data.purchase


import android.util.Log
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.di.IoDispatcher
import com.opxl.sleepslide.di.IsTestingMode
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.model.Domain.EntitlementTier
import com.opxl.sleepslide.domain.repository.PurchaseError
import com.opxl.sleepslide.domain.repository.PurchaseRepository
import com.opxl.sleepslide.domain.repository.RestoreResult
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class PurchaseRepositoryImpl @Inject constructor(
    private val purchases: Purchases,
    private val cache: EntitlementCache,
    @IsTestingMode private val isTestingMode: Boolean,
    @IoDispatcher private val io: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope,
) : PurchaseRepository {

    companion object {
        private const val TAG = "PurchaseRepository"
        /** Decision 4: first launch, no cache, no network → FREE after this long. */
        const val RESOLVE_TIMEOUT_MS = 2_000L
    }

    private val _entitlement = MutableStateFlow(EntitlementReducer.initial(isTestingMode, purchases.appUserID))

    override val entitlement: StateFlow<Domain.Entitlement> = _entitlement.asStateFlow()

    private val cacheLock = Mutex()

    init {
        // Fires for every CustomerInfo the SDK sees: our own purchase/restore calls, its
        // foreground refresh (stale after 5 min), and pending payments or refunds landing later.
        purchases.updatedCustomerInfoListener = UpdatedCustomerInfoListener { apply(it) }
        scope.launch { loadCache() }
        scope.launch {
            delay(RESOLVE_TIMEOUT_MS)
            _entitlement.update(EntitlementReducer::fallback)
        }
    }

    override suspend fun awaitResolved(): Domain.Entitlement =
        // The fallback above resolves the state on time; the extra second only guards against a stuck init.
        withTimeoutOrNull(RESOLVE_TIMEOUT_MS + 1_000L) { _entitlement.first { it.isResolved } }
            ?: _entitlement.value

    override suspend fun syncEntitlement() = withContext(io) {
        suspendCancellableCoroutine { cont ->
            purchases.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    apply(customerInfo)
                    cont.resume(Unit)
                }
                // Keep whatever tier we have — offline must never downgrade a paying user.
                override fun onError(error: PurchasesError) {
                    Log.w(TAG, "CustomerInfo fetch failed: ${error.code}")
                    cont.resume(Unit)
                }
            })
        }
    }

    override suspend fun restorePurchases(): RestoreResult = withContext(io) {

        // In testing mode, restore always returns success with PREMIUM
        if (isTestingMode) {
            _entitlement.update { it.copy(tier = EntitlementTier.PREMIUM, isRestored = true, isResolved = true) }
            return@withContext RestoreResult.Success(EntitlementTier.PREMIUM)
        }

        suspendCancellableCoroutine<RestoreResult> { cont ->
            purchases.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val tier = apply(customerInfo, isRestore = true).tier
                    cont.resume(
                        if (tier == EntitlementTier.PREMIUM) RestoreResult.Success(tier)
                        else RestoreResult.NothingToRestore
                    )
                }
                override fun onError(error: PurchasesError) {
                    cont.resume(RestoreResult.Failure(error.code.toPurchaseError()))
                }
            })
        }
    }

    override suspend fun premiumOffer(): Domain.PremiumOffer? =
        runCatching { lifetimePackage() }
            .onFailure { Log.w(TAG, "No lifetime package: ${it.message}") }
            .getOrNull()
            ?.let { Domain.PremiumOffer(formattedPrice = it.product.price.formatted) }

    /**
     * The lifetime package of the current offering. Looked up by package type, never by
     * product ID, so the Play product can be renamed in the dashboard without an app release.
     */
    internal suspend fun lifetimePackage(): Package = withContext(io) {
        suspendCancellableCoroutine { cont ->
            purchases.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    val pkg = offerings.current?.lifetime
                    if (pkg != null) cont.resume(pkg)
                    else cont.resumeWithException(
                        PurchaseException(PurchaseError.PRODUCT_UNAVAILABLE, "Current offering has no lifetime package")
                    )
                }
                override fun onError(error: PurchasesError) {
                    cont.resumeWithException(PurchaseException(error.code.toPurchaseError(), error.message))
                }
            })
        }
    }

    /** Merges a RevenueCat answer into the state and persists it. Returns the new state. */
    internal fun apply(customerInfo: CustomerInfo, isRestore: Boolean = false): Domain.Entitlement {
        val confirmed = customerInfo.toConfirmedTier()
        val updated = _entitlement.updateAndGet {
            EntitlementReducer.fromRevenueCat(it, confirmed, purchases.appUserID, isTestingMode, isRestore)
        }
        if (!isTestingMode) {
            scope.launch(io) {
                // Writes the state as of now, under a lock, so a late write can't restore an older tier.
                cacheLock.withLock {
                    val latest = _entitlement.value
                    runCatching { cache.write(EntitlementCache.Snapshot(latest.tier, latest.purchasedAt)) }
                        .onFailure { Log.e(TAG, "Entitlement cache write failed", it) }
                }
            }
        }
        return updated
    }

    private suspend fun loadCache() {
        if (isTestingMode) return
        val cached = runCatching { cache.read() }
            .onFailure { Log.e(TAG, "Entitlement cache read failed", it) }
            .getOrNull()
        _entitlement.update { EntitlementReducer.fromCache(it, cached) }
    }
}

internal class PurchaseException(val error: PurchaseError, message: String) : Exception(message)
