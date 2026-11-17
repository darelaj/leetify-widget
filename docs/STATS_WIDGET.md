# Stats widget

Implemented on `feat/stats-widget`, 18 November 2026. Add **CS Stats** through an Android launcher's widget picker, then tap Account settings to configure the existing account form.

## What it shows

- Aim, Utility, Leetify Rating, and Premier, preserving source scales, signs, and unavailable values.
- Up to five recent match outcomes as W/L/D/? with color plus text. This is the API's recent-match window across sources, not Premier-only history.
- Successful fetch date/time, distinct from match finish time.
- Refresh, View on Leetify, and the original theme-appropriate attribution badge linked to Leetify. See [asset provenance](ATTRIBUTION.md).
- Setup, loading, no matches, unavailable metrics, authentication/access failures, network/timeouts, rate limits, server errors, invalid responses, and inaccessible settings states. Failure snapshots contain no old profile.

Compact and expanded Glance layouts target 200×220 dp and 280×260 dp. A bound-host screenshot showed clipped actions at the earlier 180 dp height, so the minimum was increased and the recommended launcher grid changed from 4×2 to 4×3. Actual grid dimensions vary by launcher. Native launcher padding and the unmodified attribution artwork are included in the sizing decision. Android 12+ gets preview layout/grid hints through versioned metadata; older hosts use legacy dimensions and the app icon fallback.

At 1.2× text scaling and above, the compact layout uses four single-line metric rows instead of two columns. This preserves the full metric names and action visibility. The bound-host test checks action bounds at 1.3× scaling.

## Refresh and retention

Each update is one unique, one-time WorkManager request shared by all widget instances. The worker makes one profile request and composes fresh Glance RemoteViews for every bound instance. Adding/resizing a widget and tapping Refresh enqueue work. There is no periodic schedule, cooldown, or automatic retry in this milestone; those belong to `feat/widget-refresh`.

The receiver uses native AppWidgetProvider lifecycle callbacks and Glance's one-shot `compose` API. It intentionally avoids Glance's long-lived session lifecycle and persistent Glance state. Composition uses fresh IDs to avoid collisions with cleanup from a cancelled refresh; publication still targets the actual bound widget IDs. Actions use explicit intents rather than composition-session lambdas.

No API response, stats snapshot, generated stats bitmap, or history is persisted by the app. Objects live only during fetch/compose/publication, and the widget has no application-wide stats cache. WorkManager persists work metadata, and Glance may maintain layout metadata, neither containing provider metrics. The launcher/system necessarily retains rendered RemoteViews; the existing provider clarification gate still applies before release. A process restart reads configuration and fetches again on the next update rather than restoring a stored stats model. The old launcher-rendered fetch time remains visible until an update happens; no real-time freshness is promised.

## Account changes and reset

Every saved configuration receives a new random revision, including re-saving the same account/key. Existing records without a revision remain readable and receive one on their next save. Revision is configuration metadata, not fetched API data.

Account writes and widget publication share a process-local mutex; HTTP requests do not hold it. Before publishing, the coordinator reloads the account and checks revision, ID, and key. A response from before reset/reconfiguration is discarded. Key encryption is inside the same mutation lock so reset cannot delete its Keystore key between encryption and save.

The production ViewModel callback replaces all rendered widgets with setup/loading after a successful save/reset, then replaces pending refresh work. Reset waits for this clearing step before completing. Last-widget removal cancels pending work. Deletion during composition skips the removed instance while continuing to update remaining ones.

## Validation boundaries

Unit tests cover numeric formatting, a single shared fetch, missing account, error snapshots, reset/same-account revision races, DataStore revision changes, and ViewModel integration callbacks, alongside the existing API/crypto tests. Emulator tests cover Glance rendering at both sizes in both themes with larger text, plus binding two real AppWidgetHost instances, size options, shared publication, reset clearing, and removal.

The bound-host test uses synthetic stats and an isolated host ID, deletes its widget instances, and exports a synthetic PNG for visual review. That test image is not a cached API response. It exercises the Android widget hosting service, not a third-party launcher's drag/drop picker UI.

Live owner/API validation, an API 24 device, reboot/force-stop behavior, third-party launcher interactions, and backup transport remain unrun. Hourly scheduling, manual refresh cooldown/backoff, the conditional Premier chart, release branding, and provider render-retention clarification remain subsequent work.

Final checks on 18 November 2026: 30 JVM tests and six API 37 instrumentation tests passed; debug assembly and lint passed with 0 errors and the same 14 existing warnings. Git whitespace checks passed. Synthetic bound-host output was visually inspected; the local artifact is ignored at `app/build/outputs/widget-host.png`.
