# Implementation plan

Read [requirements](REQUIREMENTS.md) and [API research](API_RESEARCH.md) first. This is a sequence of independently reviewable tasks, not permission to claim unverified API behavior.

## Existing project

The repository contains a Compose starter in `app`, namespace/application ID `id.my.chromazone.leetify_widget`, minSdk 24, compile/target SDK 37, version catalog, Gradle wrapper, and placeholder tests. Declared AGP is 9.5.1 and Kotlin is 2.2.10. These are observed configuration values; toolchain resolution/build compatibility has not been checked. Preserve them unless a verified build problem requires a targeted change.

## Structure and data flow

Keep one application module. Start with small packages under the existing namespace:

- `data`: HTTP client, nullable DTOs, profile repository, settings and credential storage.
- `ui`: Compose setup/settings and current fetch status.
- `widget`: stats widget/receiver, optional Premier chart widget/receiver, and rendering helpers.
- `sync`: refresh worker and shared scheduling functions.

Flow: settings -> unique refresh work -> one profile request -> conservative mapping -> update all widget instances. The foreground setup flow can use the same repository. Construct dependencies directly; do not introduce a DI framework, extra modules, or generic domain layers.

Persistent settings contain the account ID, encrypted key, refresh configuration, and non-stat fetch/backoff metadata. API DTOs and chart pixels stay in memory during fetch/render. Avoid Glance preference state containing stats. A widget session that restarts must fetch again or show unavailable. Verify how each host retains RemoteViews and image data; resolve provider permission before release. Do not silently turn this into disk caching to simplify process death.

## Git workflow

The current research/planning branch is `feat/project-planning`. Use a new `feat/<feature-name>` branch for each feature below; `feature/` is also accepted. Never use a `codex/` prefix. Start from the agreed integration base containing completed prerequisites. Keep dependent branches based on their prerequisite until integration is settled; do not automatically merge or push branches.

Commit every completed task listed within a feature, after its relevant validation. Do not hold the whole feature or project for one final commit. Use the existing Git identity, stage only task-owned files, and include actual check results in the handoff.

## 1. Build baseline — `feat/android-foundation`

Prerequisite: none beyond these planning documents.

Tasks, each with its own commit:

1. Verify installed JDK, Gradle daemon criteria, SDK 37, and existing dependency resolution. Run the untouched project's build/tests/lint; document actual results. Change build configuration only when evidence requires it.
2. Add compatible stable dependencies for Glance, WorkManager, settings, and one HTTP/JSON stack (prefer OkHttp plus kotlinx.serialization if no existing alternative). Pin through the version catalog. Add INTERNET permission; retain cleartext restrictions.

Verify: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. Separate infrastructure failures from code failures; do not upgrade every dependency to resolve one issue.

## 2. API and metric mapping — `feat/leetify-profile`

Prerequisite: foundation.

1. Implement the public profile request, finite timeouts, owner-supplied authorization, key validation, and tolerant parsing. No automatic history or per-match fan-out.
2. Map only required stats and latest five unique outcomes. Preserve null/sign/zero, sort parsed dates, and keep unknown states explicit. Add synthetic fixtures and targeted tests for missing fields, unknown outcomes, malformed timestamps, and invalid responses.
3. Implement typed fetch failures and bounded retry/backoff behavior. Cover unauthorized, unavailable profile, rate limiting, offline, server failure, and malformed payloads. Check Retry-After handling without retaining bodies.

Verify: deterministic local tests with a fake HTTP server or equivalent boundary; no live credentials in fixtures. Compare owner stats manually only when credentials are supplied locally. Record unresolved scale semantics rather than guessing conversions.

## 3. Setup and credentials — `feat/account-setup`

Prerequisite: profile access.

1. Replace the greeting with SteamID64/key setup, input validation, verification result, and settings/reset controls.
2. Add settings persistence and Keystore-backed key encryption. Update backup/data extraction rules to exclude secrets. Mask key input and avoid sensitive logs.
3. Add account-generation/invalidation handling so switching/resetting cannot publish an old in-flight result. Clear rendered widget content as part of the change flow once widgets exist.

Verify: valid/invalid IDs, optional key, key replacement/removal, inaccessible profile, reset, and switch-during-fetch. Review backup rules and logs. Use focused tests for validation, encryption failure recovery, and account invalidation.

## 4. Core home-screen widget — `feat/stats-widget`

Prerequisite: setup and metric mapping.

1. Implement Glance widget/receiver, manifest/provider metadata, previews, setup action, and responsive compact/expanded layouts.
2. Render four core stats, five W/L/D outcomes, missing-data states, fetch time, refresh action, and source/attribution links. Obtain original attribution assets before release; do not invent or modify a logo.
3. Wire account reset/change to replace all instances, and implement process-death/offline/error rendering without durable API state.

Verify: emulator/device launcher add/remove, two instances, resize, light/dark backgrounds, font scaling, missing ranks, empty history, and process restart. Check API 24 and a current supported Android version. Do not substitute Compose activity previews for widget host validation.

## 5. Scheduled refresh — `feat/widget-refresh`

Prerequisite: stats widget.

1. Add one unique hourly WorkManager job with network constraints while at least one widget exists. Trigger setup/add-widget refresh and cancel scheduling after last removal.
2. Route manual refresh through coalesced work, shared cooldown, and Retry-After/backoff. One refresh updates both stats and chart instances when available.
3. Handle privacy/access loss by clearing transient state and overwriting all widgets. Handle transient errors explicitly; no last-known-good archive by default.

Verify: WorkManager tests for deduplication, cancellation, backoff, and old-account races; device checks for reboot, Doze, manual refresh, and network loss. Explain deferred scheduling in UI rather than claiming hourly precision.

## 6. Premier evidence and chart — `feat/premier-chart`

Prerequisite: working stats widget/refresh and a locally configured owner profile.

The [19 November 2026 evidence decision](API_RESEARCH.md#premier-chart-feasibility) defers chart implementation. Release preparation can proceed with the existing stats widget.

1. Resolve rank type, rating timing/units, season handling, and acceptable rendering retention against actual owner data and official UI/provider evidence. Commit a short evidence/decision update without personal responses or secrets. If unsupported, record the deferred decision and stop chart implementation here.
2. When verified, map only actual Premier points in the current response; deduplicate/sort by match timestamp, skip invalid points, and expose a clear insufficient-data state. Test mixed ladders, missing rank, one point, duplicate dates, flat values, and season gaps. Never infer seasons or reconstruct history.
3. Render a separate responsive Glance chart widget using a small Canvas-generated bitmap held in memory. Include time labels, latest rating, accessible text summary, refresh/source links, and history coverage. Do not add an interactive chart library for a launcher bitmap.

Verify: known points match official values; time spacing is correct; equal values and two-point series render safely; fewer than two points show an explanation; resized widgets do not clip. Distinguish latest historical point from current profile rating.

## 7. Release preparation — `feat/release-readiness`

Prerequisite: stats/refresh completed; chart completed or explicitly deferred.

1. Finalize independent display name/icon, official attribution artwork/links, no-affiliation wording, and short setup/privacy documentation.
2. Resolve provider render-retention questions; audit API data persistence, key handling, backups, logging, and removal flows. Confirm no stored stats/bitmaps/history.
3. Fix only issues from acceptance checks and document supported versions/known limitations. Produce a debug APK for local review if requested; signing/store publication needs a separate request.

Verify: unit tests, `assembleDebug`, `lintDebug`, available instrumentation checks, and the full requirements checklist. Report device-only or blocked checks honestly. Commit each completed remediation separately, without empty commits for checks that changed no files.

## Done for this planning task

- Official endpoint/schema and policy research recorded.
- Requirements and measurable acceptance criteria recorded.
- Feature branches, task commits, prerequisites, and validation steps defined.
- Root agent instructions record the user's Git workflow and installed coding skills.
- No Android feature implementation, build verification, API authentication, or provider approval is claimed by these documents.
