package com.opxl.sleepslide.data.purchase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opxl.sleepslide.domain.model.Domain.EntitlementTier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

// Its own file, not user_preferences_ds: "Reset preferences" clears that one, and a reset
// must not make a paying user look FREE until the network answers.
private val Context.entitlementStore: DataStore<Preferences> by preferencesDataStore(
    name = "entitlement_cache_ds"
)

/**
 * Last tier RevenueCat confirmed, so a premium user isn't treated as FREE at cold start or
 * offline. A lifetime unlock never expires, so the snapshot stays valid until RevenueCat
 * says otherwise (a refund). Editable on a rooted device — the next sync overwrites it.
 */
@Singleton
class EntitlementCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class Snapshot(val tier: EntitlementTier, val purchasedAt: Long?)

    private object Keys {
        val IS_PREMIUM   = booleanPreferencesKey("is_premium")
        val PURCHASED_AT = longPreferencesKey("purchased_at")
    }

    /** Null when nothing has been confirmed on this install yet. */
    suspend fun read(): Snapshot? {
        val prefs = context.entitlementStore.data.first()
        val isPremium = prefs[Keys.IS_PREMIUM] ?: return null
        return Snapshot(
            tier        = if (isPremium) EntitlementTier.PREMIUM else EntitlementTier.FREE,
            purchasedAt = prefs[Keys.PURCHASED_AT],
        )
    }

    suspend fun write(snapshot: Snapshot) {
        context.entitlementStore.edit {
            it[Keys.IS_PREMIUM] = snapshot.tier == EntitlementTier.PREMIUM
            val purchasedAt = snapshot.purchasedAt
            if (purchasedAt != null) it[Keys.PURCHASED_AT] = purchasedAt else it.remove(Keys.PURCHASED_AT)
        }
    }
}
