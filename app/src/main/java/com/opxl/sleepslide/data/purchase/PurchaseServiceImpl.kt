package com.opxl.sleepslide.data.purchase


import android.app.Activity
import com.opxl.sleepslide.di.ApplicationScope
import com.opxl.sleepslide.di.IoDispatcher
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.PurchaseRepository
import com.opxl.sleepslide.domain.repository.PurchaseResult
import com.opxl.sleepslide.domain.repository.RestoreResult
import com.opxl.sleepslide.domain.service.PurchaseService
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PurchaseServiceImpl @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
    private val purchases: Purchases,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PurchaseService {

    companion object {
        private const val OFFERING_ID     = "default"
        private const val PACKAGE_ID      = "sleepdrift_unlock_all"
    }

    private val _entitlement = MutableStateFlow(
        Domain.Entitlement(tier = Domain.EntitlementTier.FREE, revenueCatUserId = purchases.appUserID)
    )
    override val entitlement: StateFlow<Domain.Entitlement> = _entitlement.asStateFlow()

    private var activityRef: WeakReference<Activity>? = null

    init {
        scope.launch { refresh() }
    }

    fun bindActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun unbindActivity() {
        activityRef = null
    }

    override suspend fun purchase(productId: String): PurchaseResult = withContext(io) {
        val activity = activityRef?.get()
            ?: return@withContext PurchaseResult.Failure("No Activity bound — call bindActivity() first")

        val pkg = runCatching { fetchPackage(productId) }.getOrNull()
            ?: return@withContext PurchaseResult.Failure("Product not found: $productId")

        suspendCancellableCoroutine<PurchaseResult> { cont ->
            purchases.purchase(
                purchaseParams = PurchaseParams.Builder(activity, pkg).build(),
                callback = object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo ) {
                        val updated = _entitlement.value.copy(
                            tier             = customerInfo.toEntitlementTier(),
                            purchasedAt      = System.currentTimeMillis(),
                            revenueCatUserId = purchases.appUserID,
                        )
                        _entitlement.value = updated
                        cont.resume(PurchaseResult.Success(updated))
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        cont.resume(
                            if (userCancelled) PurchaseResult.Cancelled
                            else PurchaseResult.Failure(error.message)
                        )
                    }
                },
            )
        }
    }

    override suspend fun restore(): RestoreResult =
        purchaseRepository.restorePurchases().also { result ->
            if (result is RestoreResult.Success) {
                _entitlement.value = _entitlement.value.copy(
                    tier       = result.tier,
                    isRestored = true,
                )
            }
        }

    override suspend fun refresh() = withContext(io) {
        runCatching {
            purchaseRepository.syncEntitlement()
            _entitlement.value = purchaseRepository.getEntitlement()
        }
    }

    override fun isPremium(): Boolean =
        _entitlement.value.tier == Domain.EntitlementTier.PREMIUM


    private suspend fun fetchPackage(productId: String): Package =
        suspendCancellableCoroutine { cont ->
            purchases.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                    val pkg = offerings.current?.availablePackages
                        ?.firstOrNull { it.identifier == productId }
                        ?: offerings.getOffering(OFFERING_ID)?.availablePackages?.firstOrNull()
                    if (pkg != null) cont.resume(pkg)
                    else cont.cancel(Exception("No matching package for $productId"))
                }
                override fun onError(error: PurchasesError) {
                    cont.cancel(Exception(error.message))
                }
            })
        }

    private fun CustomerInfo.toEntitlementTier(): Domain.EntitlementTier =
        if (entitlements["premium"]?.isActive == true) Domain.EntitlementTier.PREMIUM
        else Domain.EntitlementTier.FREE
}

