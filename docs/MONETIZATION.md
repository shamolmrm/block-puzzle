# Ads and purchases

Status: **implemented in Phase 7 with test ads only. Nothing is live.** Real ad units, real products and the purchase-verification server each need your explicit approval. See "Release steps" below.

## Rules this follows (from the brief)

- **Test traffic only:** test ads only during development. `AdUnits` and the manifest use Google's published sample ids (`ca-app-pub-3940256099942544…`), and `AdMobGateway` refuses to start with anything else. Never click real ads.
- **Rewards wait for the SDK:** a reward is never promised before the ad's reward callback fires. `AdMobGateway.showRewarded` returns true only from `OnUserEarnedRewardListener`; closing the ad early grants nothing (tested).
- **Paid entitlements come from the server:** they're never trusted from the client. "Remove ads" is granted only by the `verifyPurchase` Cloud Function. The app reads `users/{uid}/entitlements/main`, which clients can't write (rules-tested).
- **Nothing real without approval:** no real purchases or production ad deployment until you approve them.

## AdMob configuration (real ids)

Ad ids are injected at build time from **git-ignored** files (`core/monetization/build.gradle.kts`):

| File | Used by | Contents |
|---|---|---|
| *(none)* | **Debug** builds, always | Google's sample test ids. `AdMobGateway` refuses to start a debug build with real ids. |
| `admob.properties` | **Release** builds | `appId=ca-app-pub-XXXX~YYYY`, `rewardedUnitId=ca-app-pub-XXXX/…`, `interstitialUnitId=ca-app-pub-XXXX/…` |
| `ads-debug.properties` | Debug only | `testDeviceIds=<hashed id from logcat>`, `umpDebugGeography=EEA` (for testing the GDPR form) |

- **Play bundle gate:** `gradlew :app:bundleRelease` **fails** when `admob.properties` is missing, so a bundle with test ads can't be uploaded by accident. Local release and benchmark APKs still build.
- **Ad content:** all requests use `MAX_AD_CONTENT_RATING_PG` (a general-audience puzzle game) and `TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE`. Revisit both if the target audience changes.

## GDPR / privacy messaging (you, in the AdMob console)

1. **GDPR message:** under Privacy & messaging, create a European regulations (GDPR) message for this app and publish it, with your privacy policy URL. Optionally add a US states message too.
2. **Ad partners:** choose the ad technology providers to list (Google defaults are fine to start).
3. **The app needs no code change.** UMP shows whichever message you publish.

**Verified on the OPPO test phone (debug, EEA debug geography):**
- The consent form appeared on first launch (`IABTCF_gdprApplies=1`), with Consent / Do not consent / Manage options.
- After "Do not consent", the app kept working with limited ads (test device) and Settings showed **Ad privacy choices**.

**Consent Mode v2 (Firebase Analytics):**
- `ad_storage`, `ad_user_data` and `ad_personalization` are always **denied**, because Analytics is never used for ads.
- `analytics_storage` follows "Share usage statistics".
- The manifest defaults are all denied until the player's settings load.

## Consent (UMP)

- **Before anything loads:** `MainActivity` runs Google's consent flow before any ad request, and the form shows only where it's required (e.g. EEA/UK).
- **SDK startup:** the Mobile Ads SDK starts only once `canRequestAds()` is true, on a background thread. On a real phone, starting it on the main thread froze the app for 4.3 s.
- **Settings:** shows "Ad privacy choices" when the consent rules require it.
- **Analytics:** Firebase Analytics doesn't collect the advertising ID (see FIREBASE.md).

## Ad placements and limits

Interstitials appear only at **natural breaks**: leaving a finished Classic game (Play again / Home) or a finished level (Next / Replay / Retry / Levels). Never during play. Limits are in `AdPolicy`, which is pure and unit-tested:

| Rule | Value |
|---|---|
| Grace breaks at the start of each session | 2 |
| At most one interstitial per | 3 breaks |
| …and per | 3 minutes |
| Players who bought "Remove ads" | never |
| Without consent | never |

Rewarded ads are **always opt-in**. They appear only when a free resource has run out, and stay available after "Remove ads":

| Where | Offer | Cap |
|---|---|---|
| Classic | "▶ Undo (ad)" when out of undos and there's a move to undo | 2 per game (total undos ≤ 3) |
| Classic | "▶ Hint (ad)" when out of hints | 2 per game (total hints ≤ 5) |
| Pack levels | "▶ Hint (ad)" | 2 per attempt |
| Daily Challenge | none | It stays identical for everyone |

## "Remove ads" (one-time, Google Play Billing)

1. **Offered only when it can be verified:** the store must return the product and a verification server must exist. Otherwise Settings shows "The store isn't available right now."
2. **Buy:** the purchase carries `obfuscatedAccountId = SHA-256(Firebase uid)`.
3. **Verify:** when Play reports `PURCHASED`, the app sends the token to `verifyPurchase`. A `PENDING` payment grants nothing.
4. **The server checks, in order:**
   1. signed in;
   2. known product;
   3. Play Developer API says it's purchased;
   4. the account tag matches;
   5. the token isn't already claimed by another player (`purchaseTokens/`);
   6. then it acknowledges and writes the entitlement.
5. **The app reads** the entitlement (Firestore listener). A local cache keeps an offline start ad-free, but only a server-verified value is ever cached.
6. **Restore:** "Restore purchases", and every app start, re-sends owned purchases for verification. This is idempotent.

**Known follow-up:** refunds and chargebacks aren't revoked automatically yet. That needs Real-time Developer Notifications or the Voided Purchases API on the server (Phase 8).

## Tests

| Area | Where | What |
|---|---|---|
| Pacing | `InterstitialPacerTest` | grace period, every 3rd break, 3-minute floor, never when removed or without consent |
| Rewards | `AdsControllerTest`, `GameViewModelTest`, `LevelGameViewModelTest` | reward only when earned; caps; no offers without consent; Daily excluded |
| Store UI | `SettingsViewModelTest` | buy only when offered; hidden once the server confirms |
| Server logic | `firebase/functions/test/verify.test.js` (6) | grant, idempotent restore, unknown/pending/cancelled refused, account mismatch, shared token, transient errors |
| Rules | `firebase/test/rules.test.mjs` | entitlements readable by the owner, writable by nobody; token claims closed |

## Release steps (need your explicit approval; none have been done)

1. **AdMob:** create an AdMob account and app, then replace the sample app id and units with real ones. Keep test devices registered, and never click your own ads.
2. **Play Console:** upload a build to internal testing, create the in-app product `remove_ads`, and add license testers. Test purchases are free for license testers.
3. **Google Cloud:** create a service account with access to the Play Developer API (View financial data, Manage orders) for the function.
4. **Firebase:** upgrade to the Blaze plan (Cloud Functions require it), then `firebase deploy --only functions,firestore:rules`.
5. **Store listing:** update the privacy policy and the Data safety form for ads, billing and analytics.
