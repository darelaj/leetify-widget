# Scheduled widget refresh

Implemented on `feat/widget-refresh`, 19 November 2026. This completes implementation-plan milestone 5; the Premier chart remains subject to its evidence gate.

## Scheduling

While at least one stats widget exists, a unique WorkManager periodic dispatcher runs about hourly, starting after an hour. It enqueues the same unique one-time refresh used by add, resize, and manual actions. `KEEP` coalesces overlapping requests. A successful account verification passes the fresh profile to the widgets in memory after checking the saved account revision and cancelling obsolete one-time work. This avoids a second request and updates the widgets even if Android delays their scheduled work. Last-widget removal cancels both unique jobs.

Both requests require `NetworkType.CONNECTED`. WorkManager execution is inexact: connectivity, Doze, battery saving, and Android scheduling can delay it. There is no exact alarm or foreground service. The account screen explains these limits. WorkManager owns job persistence/rescheduling; reboot and force-stop behavior have not been checked on a device in this milestone.

## Cooldown and retries

- A 60-second manual cooldown is shared across all instances and includes recent actual refresh attempts. Repeated taps during a cooldown leave the existing display/work alone.
- An accepted tap replaces rendered stats with setup or a queued message before enqueuing. Offline work stays queued until connectivity is available. Receiver work is bounded to eight seconds and performs no HTTP request.
- Each refresh request permits at most three claimed HTTP attempts. Network, timeout, server, and rate-limit failures retry with 60/120-second minimum delays, extended by `Retry-After` when provided. Other failures finish without automatic retries within that refresh cycle.
- WorkManager uses linear backoff starting at 60 seconds. The worker checks the persisted deadline before each HTTP attempt; wakeups before that deadline defer work without consuming another HTTP attempt. Android may execute later than the deadline.
- Claimed attempts count even if the process stops before completion. Re-executing completed work cannot restart its attempt budget. A new hourly/manual refresh cycle may try again after applicable cooldown/server waits.

`widget_refresh.preferences_pb` contains only random work/configuration revisions, attempt counts, completion flags, and timing metadata. It contains no SteamID, API key, response, metric, history, or bitmap. It is excluded from legacy backup, cloud backup, and device transfer. Account reset removes local attempt/manual metadata while retaining a known server wait, so reconfiguration cannot bypass it. Stale completion cannot change a newer request's retry state.

## Access loss and retention

Every attempted refresh first renders loading and then either a fresh profile or an explicit failure across all instances. Error snapshots never contain a profile. Unauthorized, denied, unavailable, malformed, and transient failures clear previous stats when encountered; a missing profile does not establish privacy status. Revision checks prevent obsolete success or failure from being published after an account change/reset. Cancellation propagates rather than becoming a retryable API error.

No durable stats cache was added. If Android defers background work while offline, the launcher may retain its previous rendering and fetch timestamp until an update can run. Accepted manual refresh and account reset replace it explicitly. This launcher-retained rendering still requires provider clarification before release.

## Verification

The checks for this milestone use synthetic data only:

- JVM tests cover concurrent manual taps, persisted cooldown and retry deadlines after reopening DataStore, deadline boundaries, three-attempt exhaustion, interrupted attempts, permanent failures, stale completion, and reset preserving server waits.
- Coordinator tests cover every typed failure with a profile-free error snapshot, unchanged exception/Retry-After propagation, mismatched revisions, reset during success/failure, and cancellation.
- API 37 instrumentation binds two actual AppWidgetHost instances, verifies a single hourly network-constrained job despite repeated scheduling, checks both rendered snapshots/reset, and verifies cancellation after the last removal.
- The host test temporarily disables emulator Wi-Fi/mobile data, configures a synthetic account, and exercises direct/manual broadcast refreshes. It checks one unchanged queued refresh ID, zero worker starts while disconnected, and replacement of old stats. It clears the synthetic account and restores connectivity afterward.

The initial host check failed because it cancelled an initial Glance update immediately before composing synthetic content. It now waits for that real setup update to finish before testing publication; both the focused rerun and the full suite passed before the cooldown task was committed.

Final checks: 38 JVM tests and all six API 37 instrumentation tests passed, including the offline/coalescing host scenario. Debug assembly and lint passed with 0 errors and the same 14 existing warnings. Git whitespace checks and local documentation links passed.

Live owner/API rate limiting and access revocation, API 24, Doze, reboot/force-stop, backup transport, and third-party launcher interactions remain unrun. Persisted metadata checks establish retry policy behavior, not a real server's enforcement. See [Android work requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) and [unique work](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work) for platform scheduling behavior.
