# Stat Peek release readiness

Reviewed on 19 November 2026. Stat Peek is an independent Android widget for Counter-Strike stats. The supported build range starts at Android 7.0 (API 24). The current device tests use API 37. The separate Premier timeline is [deferred](API_RESEARCH.md#premier-chart-feasibility).

## Data and attribution review

- The app sends the SteamID64 and optional key directly to Leetify over HTTPS. The HTTP client disables redirects and its disk cache, sends `Cache-Control: no-store`, and converts API failures to typed errors without attaching request headers, URLs, or bodies. The production app has no logging calls or analytics integration.
- The two application DataStore files hold the account configuration and refresh metadata. The SteamID64 stays local as configuration except when sent to Leetify or used in the widget's source link. The optional key is encrypted with AES-GCM using Android Keystore. Legacy and current backup rules exclude both files, including from device transfer.
- The widget builds a transient model and publishes Android RemoteViews. Its Glance state definition is disabled. The code review found no app-owned disk cache of API responses, stats, history, or chart bitmaps. Android may retain the last rendered values and profile action until the widget is updated or removed.
- Reset removes the configuration and Keystore key and replaces the widget content. Account revision checks discard obsolete responses. A response that reports loss of access replaces the stats with an error. Removing the last widget cancels future refresh work.
- The widget bundles the [original attribution assets](ATTRIBUTION.md). The badge links to Leetify's homepage and the source action uses **View on Leetify**. The app label and launcher icon are original, and setup identifies the app as unofficial.

## Release gate

Provider clarification is pending. The [Leetify developer guidelines](https://leetify.com/blog/leetify-api-developer-guidelines/) ask integrations to refrain from storing API data. The question for Leetify is whether a home-screen widget may publish API values and a profile action as RemoteViews when the Android host can keep that last view while the app is stopped or offline. Stat Peek creates no app-owned copy and replaces the view on account reset, changes, and detected access loss. Ask whether this behavior is acceptable and whether a maximum display age or other removal behavior is required. Public release remains pending on that clarification.

## Acceptance coverage

The existing tests cover setup validation, key protection, nullable and malformed API data, rate limiting, cooldowns, stale account responses, and reset. API 37 tests exercise a real Android widget host, two bound instances, compact and expanded layouts, light and dark themes, larger text, offline queueing, and last-widget removal. Synthetic fixtures supply the test values.

The debug build, 38 unit tests, and lint passed with 0 errors and 14 existing warnings. APK inspection confirmed the Stat Peek label and both launcher icon configurations. The first API 37 run timed out on the host's initial setup view. The test now waits for binding and resizing work to settle and explicitly requests a refresh with both views listening. All six emulator tests then passed.

Owner verification against live Leetify values, real API revocation and rate limiting, an API 24 device, launcher picker interactions, process death, reboot, Doze, and backup transport remain unverified. The app shows the last successful fetch time when Android keeps an older rendering. Android can delay hourly refreshes, and the app supports a single account shared by all widget instances.
