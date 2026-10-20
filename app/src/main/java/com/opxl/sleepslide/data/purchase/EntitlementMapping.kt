package com.opxl.sleepslide.data.purchase

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.model.Domain.EntitlementTier
import com.opxl.sleepslide.domain.repository.PurchaseError
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesErrorCode

/** RevenueCat entitlement attached to the lifetime product in the dashboard. */
const val ENTITLEMENT_ID = "sleepslide_pro"

/** What RevenueCat says, before it is merged into the current state. */
internal data class ConfirmedTier(val tier: EntitlementTier, val purchasedAt: Long?)

internal fun CustomerInfo.toConfirmedTier(): ConfirmedTier {
    val info = entitlements[ENTITLEMENT_ID]
    return if (info?.isActive == true) {
        ConfirmedTier(EntitlementTier.PREMIUM, info.originalPurchaseDate.time)
    } else {
        ConfirmedTier(EntitlementTier.FREE, purchasedAt = null)
    }
}

/**
 * State transitions, kept pure so the ordering rules are unit-tested:
 * the cache and the timeout fallback only fill an unresolved state, RevenueCat always wins,
 * and testing mode is PREMIUM whatever RevenueCat says.
 */
internal object EntitlementReducer {

    fun initial(isTestingMode: Boolean, userId: String) = Domain.Entitlement(
        tier             = if (isTestingMode) EntitlementTier.PREMIUM else EntitlementTier.FREE,
        revenueCatUserId = userId,
        isResolved       = isTestingMode,
    )

    fun fromCache(current: Domain.Entitlement, cached: EntitlementCache.Snapshot?): Domain.Entitlement =
        if (current.isResolved || cached == null) current
        else current.copy(tier = cached.tier, purchasedAt = cached.purchasedAt, isResolved = true)

    /** No cache and no network in time — first, offline launch. */
    fun fallback(current: Domain.Entitlement): Domain.Entitlement =
        if (current.isResolved) current else current.copy(tier = EntitlementTier.FREE, isResolved = true)

    fun fromRevenueCat(
        current: Domain.Entitlement,
        confirmed: ConfirmedTier,
        userId: String,
        isTestingMode: Boolean,
        isRestore: Boolean = false,
    ): Domain.Entitlement {
        if (isTestingMode) return current.copy(revenueCatUserId = userId, isResolved = true)
        return current.copy(
            tier             = confirmed.tier,
            purchasedAt      = confirmed.purchasedAt,
            revenueCatUserId = userId,
            // Sticky once restored, cleared if the unlock is revoked (refund).
            isRestored       = confirmed.tier == EntitlementTier.PREMIUM && (current.isRestored || isRestore),
            isResolved       = true,
        )
    }
}

internal fun PurchasesErrorCode.toPurchaseError(): PurchaseError = when (this) {
    PurchasesErrorCode.NetworkError                        -> PurchaseError.NETWORK
    PurchasesErrorCode.StoreProblemError,
    PurchasesErrorCode.UnexpectedBackendResponseError,
    PurchasesErrorCode.UnknownBackendError                 -> PurchaseError.STORE_PROBLEM
    PurchasesErrorCode.PurchaseNotAllowedError,
    PurchasesErrorCode.InsufficientPermissionsError        -> PurchaseError.NOT_ALLOWED
    PurchasesErrorCode.ProductNotAvailableForPurchaseError,
    PurchasesErrorCode.ConfigurationError                  -> PurchaseError.PRODUCT_UNAVAILABLE
    PurchasesErrorCode.OperationAlreadyInProgressError     -> PurchaseError.ALREADY_IN_PROGRESS
    else                                                   -> PurchaseError.UNKNOWN
}
