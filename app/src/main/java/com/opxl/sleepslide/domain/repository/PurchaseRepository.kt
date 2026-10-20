package com.opxl.sleepslide.domain.repository

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.StateFlow

/**
 * The single source of truth for the entitlement. Starts unresolved, then takes the on-disk
 * snapshot, then whatever RevenueCat reports — purchases, restores, refunds, and pending
 * payments that clear later.
 */
interface PurchaseRepository {

    val entitlement: StateFlow<Domain.Entitlement>

    /** Suspends until the tier is known — at most a couple of seconds on a first, offline launch. */
    suspend fun awaitResolved(): Domain.Entitlement

    /** Re-fetches from RevenueCat. A network error never downgrades the cached tier. */
    suspend fun syncEntitlement()

    suspend fun restorePurchases(): RestoreResult

    /** Null when the offering can't be loaded (offline, store unavailable, misconfigured). */
    suspend fun premiumOffer(): Domain.PremiumOffer?
}

/** Why a purchase or restore failed — the UI picks the wording. */
enum class PurchaseError {
    NETWORK,
    STORE_PROBLEM,
    NOT_ALLOWED,
    PRODUCT_UNAVAILABLE,
    ALREADY_IN_PROGRESS,
    /** Play charged the user but the entitlement isn't active — usually a dashboard setup issue. */
    NOT_UNLOCKED,
    UNKNOWN,
}

sealed interface PurchaseResult {
    data class Success(val entitlement: Domain.Entitlement) : PurchaseResult
    data object Cancelled : PurchaseResult
    /** Cash / slow-card payment. Premium unlocks by itself when Play confirms it. */
    data object Pending : PurchaseResult
    data class Failure(val error: PurchaseError, val cause: Throwable? = null) : PurchaseResult
}

sealed interface RestoreResult {
    data class Success(val tier: Domain.EntitlementTier) : RestoreResult
    data object NothingToRestore : RestoreResult
    data class Failure(val error: PurchaseError, val cause: Throwable? = null) : RestoreResult
}
