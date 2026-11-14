package com.opxl.sleepslide.data.purchase

import com.opxl.sleepslide.domain.model.Domain.EntitlementTier.FREE
import com.opxl.sleepslide.domain.model.Domain.EntitlementTier.PREMIUM
import com.opxl.sleepslide.domain.repository.PurchaseError
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesErrorCode
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class EntitlementReducerTest {

    private val unresolved = EntitlementReducer.initial(isTestingMode = false, userId = "u1")
    private val premiumSnapshot = EntitlementCache.Snapshot(PREMIUM, purchasedAt = 42L)

    @Test
    fun `cold start is unresolved, testing mode is resolved premium`() {
        assertFalse(unresolved.isResolved)
        assertEquals(FREE, unresolved.tier)

        val testing = EntitlementReducer.initial(isTestingMode = true, userId = "u1")
        assertTrue(testing.isResolved)
        assertEquals(PREMIUM, testing.tier)
    }

    @Test
    fun `cached premium resolves a paying user before the network answers`() {
        val state = EntitlementReducer.fromCache(unresolved, premiumSnapshot)
        assertTrue(state.isResolved)
        assertEquals(PREMIUM, state.tier)
        assertEquals(42L, state.purchasedAt)
    }

    @Test
    fun `no cache leaves the state unresolved`() {
        assertFalse(EntitlementReducer.fromCache(unresolved, null).isResolved)
    }

    @Test
    fun `a late cache read never overrides RevenueCat`() {
        val refunded = EntitlementReducer.fromRevenueCat(unresolved, ConfirmedTier(FREE, null), "u1", isTestingMode = false)
        assertEquals(FREE, EntitlementReducer.fromCache(refunded, premiumSnapshot).tier)
    }

    @Test
    fun `fallback is FREE only when nothing answered`() {
        assertEquals(FREE, EntitlementReducer.fallback(unresolved).tier)
        assertTrue(EntitlementReducer.fallback(unresolved).isResolved)

        val cached = EntitlementReducer.fromCache(unresolved, premiumSnapshot)
        assertEquals(PREMIUM, EntitlementReducer.fallback(cached).tier)
    }

    @Test
    fun `RevenueCat downgrades cached premium on refund and clears restored flag`() {
        val restored = EntitlementReducer.fromRevenueCat(
            unresolved, ConfirmedTier(PREMIUM, 1L), "u1", isTestingMode = false, isRestore = true,
        )
        assertTrue(restored.isRestored)

        val refunded = EntitlementReducer.fromRevenueCat(restored, ConfirmedTier(FREE, null), "u1", isTestingMode = false)
        assertEquals(FREE, refunded.tier)
        assertFalse(refunded.isRestored)
        assertNull(refunded.purchasedAt)
    }

    @Test
    fun `restored flag survives later refreshes`() {
        val restored = EntitlementReducer.fromRevenueCat(
            unresolved, ConfirmedTier(PREMIUM, 1L), "u1", isTestingMode = false, isRestore = true,
        )
        val refreshed = EntitlementReducer.fromRevenueCat(restored, ConfirmedTier(PREMIUM, 1L), "u1", isTestingMode = false)
        assertTrue(refreshed.isRestored)
    }

    @Test
    fun `testing mode ignores RevenueCat FREE`() {
        val testing = EntitlementReducer.initial(isTestingMode = true, userId = "u1")
        val state = EntitlementReducer.fromRevenueCat(testing, ConfirmedTier(FREE, null), "u2", isTestingMode = true)
        assertEquals(PREMIUM, state.tier)
        assertEquals("u2", state.revenueCatUserId)
    }

    @Test
    fun `CustomerInfo maps the active entitlement to premium`() {
        val date = Date(1_000L)
        val active = mockk<CustomerInfo> {
            every { entitlements[ENTITLEMENT_ID] } returns mockk {
                every { isActive } returns true
                every { originalPurchaseDate } returns date
            }
        }
        assertEquals(ConfirmedTier(PREMIUM, 1_000L), active.toConfirmedTier())

        val inactive = mockk<CustomerInfo> {
            every { entitlements[ENTITLEMENT_ID] } returns mockk { every { isActive } returns false }
        }
        assertEquals(FREE, inactive.toConfirmedTier().tier)

        val missing = mockk<CustomerInfo> { every { entitlements[ENTITLEMENT_ID] } returns null }
        assertEquals(FREE, missing.toConfirmedTier().tier)
    }

    @Test
    fun `error codes map to user-facing reasons`() {
        assertEquals(PurchaseError.NETWORK, PurchasesErrorCode.NetworkError.toPurchaseError())
        assertEquals(PurchaseError.STORE_PROBLEM, PurchasesErrorCode.StoreProblemError.toPurchaseError())
        assertEquals(PurchaseError.NOT_ALLOWED, PurchasesErrorCode.PurchaseNotAllowedError.toPurchaseError())
        assertEquals(PurchaseError.PRODUCT_UNAVAILABLE, PurchasesErrorCode.ProductNotAvailableForPurchaseError.toPurchaseError())
        assertEquals(PurchaseError.ALREADY_IN_PROGRESS, PurchasesErrorCode.OperationAlreadyInProgressError.toPurchaseError())
        assertEquals(PurchaseError.UNKNOWN, PurchasesErrorCode.UnknownError.toPurchaseError())
    }
}
