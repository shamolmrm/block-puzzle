package com.puzzleblockblast.game.core.model.ads

/**
 * Ad frequency rules (docs/MONETIZATION.md). Interstitials only ever appear at a natural break
 * (between games or levels, never during play), never for players who removed ads or haven't
 * consented to ad requests, never in the first breaks of a session, and at most every few breaks
 * and minutes. Rewarded ads are always the player's choice and are capped per game.
 */
data class AdPolicy(
    /** Breaks at the start of each session that never show an interstitial. */
    val graceBreaks: Int = 2,
    /** Show at most one interstitial per this many breaks. */
    val breaksBetweenInterstitials: Int = 3,
    /** …and at most one per this many milliseconds. */
    val minMillisBetweenInterstitials: Long = 3 * 60_000L,
    /** Rewarded undos per game (on top of the free one). */
    val rewardedUndosPerGame: Int = 2,
    /** Rewarded hints per game or level attempt. */
    val rewardedHintsPerGame: Int = 2,
) {
    init {
        require(graceBreaks >= 0 && breaksBetweenInterstitials >= 1 && minMillisBetweenInterstitials >= 0)
        require(rewardedUndosPerGame >= 0 && rewardedHintsPerGame >= 0)
    }
}

/** Decides, at each break, whether an interstitial may show. One instance per app session. */
class InterstitialPacer(private val policy: AdPolicy = AdPolicy()) {
    private var breaksThisSession = 0
    private var breaksSinceLastAd = 0
    private var lastShownAtMillis: Long? = null

    /** Records a break and returns whether an interstitial may show now. */
    fun onBreak(nowMillis: Long, adsRemoved: Boolean, canRequestAds: Boolean): Boolean {
        breaksThisSession++
        breaksSinceLastAd++
        if (adsRemoved || !canRequestAds) return false
        if (breaksThisSession <= policy.graceBreaks) return false
        if (breaksSinceLastAd < policy.breaksBetweenInterstitials) return false
        val last = lastShownAtMillis
        return last == null || nowMillis - last >= policy.minMillisBetweenInterstitials
    }

    /** Call only when an interstitial was actually shown. */
    fun onShown(nowMillis: Long) {
        breaksSinceLastAd = 0
        lastShownAtMillis = nowMillis
    }
}
