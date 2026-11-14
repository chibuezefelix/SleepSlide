package com.opxl.sleepslide.di


import android.content.Context
import android.util.Log
import com.opxl.sleepslide.BuildConfig
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PurchaseModule {

    private const val TAG = "PurchaseModule"

    /**
     * The single place RevenueCat is configured. This has to be the provider, not
     * Application.onCreate(): Hilt injects SleepSlideApp's fields (PurchaseService →
     * Purchases) *before* the onCreate body runs, so anything done there is too late.
     * [apiKey] already resolves to the test key in testing mode (AppConfigModule).
     */
    @Provides
    @Singleton
    fun providePurchases(
        @ApplicationContext context: Context,
        @RevenueCatApiKey apiKey: String,
        @IsTestingMode isTestingMode: Boolean,
    ): Purchases {
        if (!Purchases.isConfigured) {
            if (BuildConfig.DEBUG || isTestingMode) Purchases.logLevel = LogLevel.DEBUG
            if (isTestingMode) Log.w(TAG, "⚠️  TESTING MODE — all users granted PREMIUM tier without purchase")
            Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
        }
        return Purchases.sharedInstance
    }
}
