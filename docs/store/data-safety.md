# Play Data safety — suggested answers (DRAFT)

These answers are based on the code as of Phase 8, assuming Firebase and AdMob are enabled for release. Re-check them against the current Play form, and against the AdMob and Firebase SDK disclosures, before submitting. Google publishes what its SDKs collect.

**Does the app collect or share user data?** Yes.

**Is data encrypted in transit?** Yes. Firebase and AdMob use TLS.

**Can users request deletion?** Yes, by email. A fully automated in-app deletion option is a recommended follow-up.

| Data type | Collected | Shared | Purpose | Optional | Source |
|---|---|---|---|---|---|
| App activity → App interactions (gameplay events) | Yes | No | Analytics | Yes (Settings toggle) | Firebase Analytics |
| App activity → Other user-generated content (game progress backup) | Yes | No | App functionality | No (needed for backup) | Firestore |
| App info and performance → Crash logs, Diagnostics | Yes | No | App functionality, analytics | Yes (Settings toggle) | Crashlytics |
| Device or other IDs (advertising ID, app set ID) | Yes | Yes (ad network) | Advertising, fraud prevention | Consent where required | AdMob |
| Device or other IDs (Firebase installation / anonymous user id) | Yes | No | App functionality, analytics | No | Firebase |
| Financial info → Purchase history | Yes | No | App functionality (verify Remove ads) | Yes (only if purchasing) | Google Play / our server |
| Approximate location (IP-derived, by the ad SDK) | Check AdMob's current disclosure | — | Advertising | — | AdMob |

**Not collected:** name, email, contacts, photos, precise location, messages, health and fitness data.
