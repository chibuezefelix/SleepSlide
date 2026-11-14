package com.opxl.sleepslide.domain.service

import com.opxl.sleepslide.domain.model.Domain
import com.opxl.sleepslide.domain.repository.PurchaseResult
import com.opxl.sleepslide.domain.repository.RestoreResult
import kotlinx.coroutines.flow.StateFlow

interface PurchaseService {

    val entitlement: StateFlow<Domain.Entitlement> // free or premium

    /** Buys the lifetime package of the current offering. */
    suspend fun purchase(): PurchaseResult

    suspend fun restore(): RestoreResult

    suspend fun refresh()

    suspend fun premiumOffer(): Domain.PremiumOffer?

    fun isPremium(): Boolean
}
