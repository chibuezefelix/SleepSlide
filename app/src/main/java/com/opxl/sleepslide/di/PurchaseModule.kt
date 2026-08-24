package com.opxl.sleepslide.di


import android.content.Context
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

    @Provides
    @Singleton
    fun providePurchases(
        @ApplicationContext context: Context,
        @RevenueCatApiKey apiKey: String,
    ): Purchases {
        Purchases.configure(
            PurchasesConfiguration.Builder(context, apiKey).build()
        )
        return Purchases.sharedInstance
    }
}
