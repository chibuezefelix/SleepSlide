package com.opxl.sleepslide.domain.repository

import com.opxl.sleepslide.domain.model.Domain
import kotlinx.coroutines.flow.Flow

interface PurchaseRepository {

    fun observeEntitlement(): Flow<Domain.Entitlement>

    suspend fun getEntitlement(): Domain.Entitlement

    suspend fun purchase(productId: String): PurchaseResult

    suspend fun restorePurchases(): RestoreResult

    suspend fun syncEntitlement()
}

sealed interface PurchaseResult {
    data class Success(val entitlement: Domain.Entitlement) : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failure(val reason: String, val cause: Throwable? = null) : PurchaseResult
}

sealed interface RestoreResult {
    data class Success(val tier: Domain.EntitlementTier) : RestoreResult
    data object NothingToRestore : RestoreResult
    data class Failure(val reason: String, val cause: Throwable? = null) : RestoreResult
}