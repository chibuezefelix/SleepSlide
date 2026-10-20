package com.opxl.sleepslide.data.observer

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.observer.EntitlementObserver
import com.opxl.sleepslide.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementObserverImpl @Inject constructor(
    private val purchaseRepository: PurchaseRepository,
) : EntitlementObserver {

    override val entitlement: StateFlow<Domain.Entitlement> = purchaseRepository.entitlement

    override val tier: Flow<Domain.EntitlementTier> =
        purchaseRepository.entitlement
            .filter { it.isResolved }
            .map { it.tier }
            .distinctUntilChanged()

    override val isPremium: Flow<Boolean> =
        tier.map { it == Domain.EntitlementTier.PREMIUM }.distinctUntilChanged()

    override suspend fun awaitResolved(): Domain.Entitlement = purchaseRepository.awaitResolved()
}
