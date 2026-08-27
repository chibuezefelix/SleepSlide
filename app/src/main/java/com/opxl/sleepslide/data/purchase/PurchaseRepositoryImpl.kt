package com.opxl.sleepslide.data.purchase


import com.opxl.sleepslide.di.IoDispatcher
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.model.Domain.EntitlementTier
import com.opxl.sleepslide.domain.repository.PurchaseRepository
import com.opxl.sleepslide.domain.repository.PurchaseResult
import com.opxl.sleepslide.domain.repository.RestoreResult
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PurchaseRepositoryImpl @Inject constructor(
    private val purchases: Purchases,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PurchaseRepository {

    companion object {
        const val ENTITLEMENT_ID = "premium"
        const val PRODUCT_ID     = "sleepdrift_unlock_all"
    }

    private val _entitlement = MutableStateFlow(
        Domain.Entitlement(
            tier = EntitlementTier.FREE,
            revenueCatUserId = purchases.appUserID,
        )
    )

    override fun observeEntitlement(): Flow<Domain.Entitlement> = _entitlement.asStateFlow()

    override suspend fun getEntitlement(): Domain.Entitlement = _entitlement.value

    override suspend fun purchase(productId: String): PurchaseResult =
        withContext(io) { PurchaseResult.Failure("Purchase flow requires Activity context — delegate to PurchaseService") }

    override suspend fun restorePurchases(): RestoreResult = withContext(io) {
        suspendCancellableCoroutine { cont ->
            purchases.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val tier = customerInfo.toEntitlementTier()
                    _entitlement.value = _entitlement.value.copy(tier = tier, isRestored = true)
                    cont.resume(
                        if (tier == EntitlementTier.PREMIUM) RestoreResult.Success(tier)
                        else RestoreResult.NothingToRestore
                    )
                }
                override fun onError(error: PurchasesError) {
                    cont.resume(RestoreResult.Failure(error.message))
                }
            })
        }
    }

    override suspend fun syncEntitlement() = withContext(io) {
            suspendCancellableCoroutine { cont ->
                purchases.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                    override fun onReceived(customerInfo: CustomerInfo) {
                        _entitlement.value = _entitlement.value.copy(
                            tier             = customerInfo.toEntitlementTier(),
                            revenueCatUserId = purchases.appUserID,
                        )
                        cont.resume(Unit)
                    }
                    override fun onError(error: PurchasesError) { cont.resume(Unit) }
                })


    }}


    private fun CustomerInfo.toEntitlementTier(): EntitlementTier =

        if (entitlements[ENTITLEMENT_ID]?.isActive == true) EntitlementTier.PREMIUM
        else EntitlementTier.FREE
}