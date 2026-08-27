package com.opxl.sleepslide.data.observer

import com.opxl.sleepslide.data.local.Local
import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.observer.EntitlementObserver
import com.opxl.sleepslide.domain.service.PurchaseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementObserverImpl @Inject constructor(
    private val purchaseService: PurchaseService,
) : EntitlementObserver {

    override val entitlement: StateFlow<Domain.Entitlement> = purchaseService.entitlement

    override val tier: Flow<Domain.EntitlementTier> =
        purchaseService.entitlement.map { it.tier }.distinctUntilChanged()

    override val isPremium: Flow<Boolean> =
        purchaseService.entitlement.map { it.tier == Domain.EntitlementTier.PREMIUM }.distinctUntilChanged()
}