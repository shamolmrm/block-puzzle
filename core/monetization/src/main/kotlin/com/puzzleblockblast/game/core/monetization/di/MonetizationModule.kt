package com.puzzleblockblast.game.core.monetization.di

import com.puzzleblockblast.game.core.monetization.ads.AdMobGateway
import com.puzzleblockblast.game.core.monetization.ads.AdsGateway
import com.puzzleblockblast.game.core.monetization.billing.BillingGateway
import com.puzzleblockblast.game.core.monetization.billing.CachedEntitlements
import com.puzzleblockblast.game.core.monetization.billing.Entitlements
import com.puzzleblockblast.game.core.monetization.billing.PlayBillingGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** PurchaseVerifier and EntitlementSource are bound in core:firebase (server-backed or "none"). */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MonetizationModule {
    @Binds
    abstract fun bindAds(impl: AdMobGateway): AdsGateway

    @Binds
    abstract fun bindBilling(impl: PlayBillingGateway): BillingGateway

    @Binds
    abstract fun bindEntitlements(impl: CachedEntitlements): Entitlements
}
