package com.opxl.sleepslide.data.purchase


import android.app.Activity
import com.opxl.sleepslide.di.IoDispatcher
import com.opxl.sleepslide.di.IsTestingMode
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.PurchaseError
import com.opxl.sleepslide.domain.repository.PurchaseResult
import com.opxl.sleepslide.domain.repository.RestoreResult
import com.opxl.sleepslide.domain.service.PurchaseService
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Runs the purchase itself (it needs an Activity); all entitlement state lives in
 * [PurchaseRepositoryImpl]. The result lands there too, so a purchase still unlocks if the
 * screen that started it is gone by the time Play answers.
 */
@Singleton
class PurchaseServiceImpl @Inject constructor(
    private val purchaseRepository: PurchaseRepositoryImpl,
    private val purchases: Purchases,
    @IsTestingMode private val isTestingMode: Boolean,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PurchaseService {

    override val entitlement: StateFlow<Domain.Entitlement> = purchaseRepository.entitlement

    private var activityRef: WeakReference<Activity>? = null

    fun bindActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun unbindActivity() {
        activityRef = null
    }

    override suspend fun purchase(): PurchaseResult = withContext(io) {

        // Testing mode is PREMIUM from launch — nothing to buy
        if (isTestingMode) return@withContext PurchaseResult.Success(entitlement.value)

        val activity = activityRef?.get()
            ?: return@withContext PurchaseResult.Failure(PurchaseError.UNKNOWN)

        val pkg = try {
            purchaseRepository.lifetimePackage()
        } catch (e: PurchaseException) {
            return@withContext PurchaseResult.Failure(e.error, e)
        }

        // Null means Play says this account already owns the unlock
        val result = suspendCancellableCoroutine<PurchaseResult?> { cont ->
            purchases.purchase(
                purchaseParams = PurchaseParams.Builder(activity, pkg).build(),
                callback = object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        val updated = purchaseRepository.apply(customerInfo)
                        cont.resume(
                            if (updated.tier == Domain.EntitlementTier.PREMIUM) PurchaseResult.Success(updated)
                            // Charged, but the product isn't attached to the entitlement
                            else PurchaseResult.Failure(PurchaseError.NOT_UNLOCKED)
                        )
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        cont.resume(
                            when {
                                userCancelled -> PurchaseResult.Cancelled
                                error.code == PurchasesErrorCode.PaymentPendingError -> PurchaseResult.Pending
                                error.code == PurchasesErrorCode.ProductAlreadyPurchasedError -> null
                                else -> PurchaseResult.Failure(error.code.toPurchaseError())
                            }
                        )
                    }
                },
            )
        }

        // Owned but the unlock never reached us (reinstall, new device) — restore instead.
        result ?: when (val restored = purchaseRepository.restorePurchases()) {
            is RestoreResult.Success       -> PurchaseResult.Success(entitlement.value)
            is RestoreResult.Failure       -> PurchaseResult.Failure(restored.error)
            RestoreResult.NothingToRestore -> PurchaseResult.Failure(PurchaseError.NOT_UNLOCKED)
        }
    }

    override suspend fun restore(): RestoreResult = purchaseRepository.restorePurchases()

    override suspend fun refresh() = purchaseRepository.syncEntitlement()

    override suspend fun premiumOffer(): Domain.PremiumOffer? = purchaseRepository.premiumOffer()

    override fun isPremium(): Boolean =
        entitlement.value.tier == Domain.EntitlementTier.PREMIUM
}
