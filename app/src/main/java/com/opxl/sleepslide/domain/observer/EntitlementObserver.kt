package com.opxl.sleepslide.domain.observer

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface EntitlementObserver {

    /** Raw state, including the unresolved cold-start value — check [Domain.Entitlement.isResolved]. */
    val entitlement: StateFlow<Domain.Entitlement>

    /** Resolved tiers only, so screens never flash locks at a paying user. */
    val tier: Flow<Domain.EntitlementTier>

    val isPremium: Flow<Boolean>

    /** For one-shot checks (gating a launch or a tap): waits out the unresolved window. */
    suspend fun awaitResolved(): Domain.Entitlement
}
