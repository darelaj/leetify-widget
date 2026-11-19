# Android stats widgets requirements

Status: ready for implementation planning; API and provider questions below are explicit gates. Baseline: 17 November 2026.

## Goal and scope

Monitor one owner's Counter-Strike stats from the Android home screen: Aim, Utility, Leetify Rating, current Premier rating, and recent win/loss results. Provide a separate Premier chart widget if verified historical data is available through the public API.

Use the existing single-module Kotlin/Compose app and retain minimum Android API 24. Public app branding must be independent of Leetify; proposed working display name is **Stat Peek**, to be finalized before release. Keep the existing application ID unless a distribution need requires changing it.

This task produces research and planning documents. Feature implementation follows the milestones in [the implementation plan](IMPLEMENTATION_PLAN.md). API evidence and source links live in [API research](API_RESEARCH.md).

## Functional requirements

| ID | Requirement | Acceptance criteria |
| --- | --- | --- |
| FR-01 | Configure one account | Accept a 17-digit SteamID64 as a string, reject invalid input locally, and verify API availability before displaying stats. Explain that a registered, accessible Leetify profile is needed. No Steam password is requested. |
| FR-02 | Configure API access | Allow an optional owner-supplied API key; validate it when supplied. Explain stricter limits without a key. Mask it, allow replacement/removal, and never log or package it. |
| FR-03 | Show core stats | Widget displays Aim, Utility, Leetify Rating, and Premier from the exact profile fields in API research. Missing/null fields read unavailable; zero and negative values are not discarded. No invented averages or undocumented scale changes. |
| FR-04 | Show recent results | Show up to ten unique matches in expanded widgets and up to five in compact widgets, newest first, using W/L/D text plus color. Unknown outcomes use a neutral marker. Show only the results supplied by the profile and label an empty list explicitly. Label the window as recent matches returned by the API; do not imply Premier-only results. |
| FR-05 | Refresh | Offer manual refresh and default hourly background refresh while widgets exist. Display the last successful fetch time and distinguish it from match time. Do not promise exact scheduling or live game updates. |
| FR-06 | Handle failures | Distinguish setup required, refreshing, no matches, unavailable/private profile, invalid key, offline, rate limited, and server/response failure where evidence permits. Do not infer privacy from every 404. Show retry/settings actions without showing stale stats as current. |
| FR-07 | Support home-screen use | Provide a resizable stats widget with a 4×3 default and a shorter 4×2 layout on launchers with sufficient cell height. Preserve all four metrics, actions, and legible original attribution in both sizes. Multiple instances share the same configured account. Include Refresh and a View on Leetify link. |
| FR-08 | Separate chart widget | After the data gate passes, offer a distinct resizable widget targeting 4x2 or larger: Premier points from the current response, oldest to newest on a time axis, latest rating, date range, and limited-history label. At least two valid Premier points are required. Fewer points show an explanation. |
| FR-09 | Reset/change account | Cancel old work, drop transient responses, replace every widget's rendered content, and clear credentials/settings on reset. Account switching must not allow an in-flight old response to overwrite the new account. |
| FR-10 | Attribution | Use original provider attribution artwork linked to Leetify and a legible View on Leetify source link. Independent name/icon and clear unofficial status are required before release. |

## Quality and platform requirements

- Use Jetpack Glance for widget UI and Compose for setup/settings. Keep widget composables separate from ordinary Compose UI.
- Use WorkManager with network constraints, one unique periodic job for the configured account, and coalesced manual refreshes. Cancel periodic work when no widget remains. Android may defer work in Doze or battery saver.
- Proposed manual refresh cooldown: 60 seconds shared across instances, adjustable if actual API guidance requires it. On 429 honor Retry-After when present and back off when absent; never retry indefinitely or fan out per match.
- Do not persist API stats/history or HTTP bodies. Persist only configuration, non-sensitive scheduling metadata, and a protected owner key. Render transiently; the unavoidable launcher-retained display needs provider clarification before release. Do not add a last-known-good offline cache without provider permission.
- Protect stored keys using Android Keystore-backed encryption; exclude credentials from backup/data extraction. No analytics or crash logs containing identifiers, keys, headers, or response bodies.
- Cancel or invalidate obsolete refreshes when settings change. Treat a confirmed access revocation as a reason to clear API-derived state and replace widget content.
- Use accessible contrast, text indicators alongside colors, useful content descriptions, and legible scaled text. Test dark/light launchers, resizing, and process death on real widget hosts.
- Tolerate absent fields, unknown JSON properties, unknown outcome strings, malformed timestamps, and missing ranks without crashing. HTTPS only; configure finite request timeouts and bounded responses/retries.
- Remain a single app module with direct dependency construction. Add only needed networking/JSON, Glance, WorkManager, and settings dependencies after checking compatibility. No backend is required for the personal MVP.

## Premier chart gate

Before FR-08 implementation, verify the Premier rank discriminator, units, timing (before/after match), and season handling using the owner's live response and official UI or provider clarification. Current API docs do not establish them. Do not use a numeric threshold to guess Premier, mix ladder types, or fabricate missing points. No local daily rating archive is permitted under the default design.

If the API cannot provide valid points, deliver the stats widget and a documented deferred chart milestone. Do not publish a misleading chart to satisfy the feature checkbox.

## Out of scope

Multiple accounts per widget, Steam OAuth or vanity resolution, live match tracking, notifications, coaching, social comparisons, FACEIT charts, web backends, cloud sync, and app-store publication are outside the initial implementation.

## Completion checks

1. Owner can configure their profile and add at least two widget instances that show the same verified source values.
2. Manual and background refresh paths update all instances without duplicate requests; failed requests render an explicit state.
3. Reset/account change clears old content and prevents late responses from reappearing.
4. Pure mapping/formatting/error tests pass, plus build and lint checks; widget behavior is checked on API 24 and a current supported Android version.
5. Chart acceptance is demonstrated with verified Premier points, or its data gate remains explicitly deferred.
6. Attribution, key protection, and render-retention questions are resolved before release.
