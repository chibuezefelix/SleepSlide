package com.opxl.sleepslide.domain.observer

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface EntitlementObserver {

    val entitlement: StateFlow<Domain.Entitlement>

    val tier: Flow<Domain.EntitlementTier>

    val isPremium: Flow<Boolean>
}