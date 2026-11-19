# Leetify API research

Checked: 17 November 2026. This document describes the public contract, not a successful request against the owner's account. No credentials or personal profile were supplied.

## Official sources

- [Swagger documentation](https://api-public-docs.cs-prod.leetify.com/)
- [Swagger initialization with embedded OpenAPI schema](https://api-public-docs.cs-prod.leetify.com/swagger-ui-init.js)
- [Developer guidelines](https://leetify.com/blog/leetify-api-developer-guidelines/)
- [January 2026 API privacy update](https://leetify.com/blog/privacy-updates-to-our-api-and-profiles/)
- [API key setup](https://leetify.com/app/developer)

## Access

Base URL: `https://api-public.cs-prod.leetify.com`.

Use a developer key in `Authorization: Bearer <key>` or `_leetify_key`. The docs permit keyless requests but warn of stricter limiting; they publish no numeric request quota. Prefer an owner-supplied key for this personal app. Do not embed a shared developer secret in an APK.

`GET /api-key/validate` is described in the documentation introduction: 200 means valid, 401 means absent/invalid, 500 means server error. Other endpoint error bodies/statuses are not specified by the embedded schema; implement defensive handling without promising a particular error contract.

Profile endpoints accept `steam64_id` or `id` (Leetify user UUID) as query parameters. Send exactly one. MVP input can be SteamID64 only; nickname and vanity URL resolution would need extra work outside this API.

The privacy announcement restricts public API data to registered Leetify users. Availability also follows profile privacy. A missing response must not be interpreted as zero stats or a nonexistent Steam account.

## Endpoints worth using

| Request | Response | Intended use |
| --- | --- | --- |
| `GET /v3/profile?steam64_id=<id>` | `ProfileResponse` | One request supplies the stats widget and recent outcomes |
| `GET /v3/profile/matches?steam64_id=<id>` | Array of `MatchDetailsResponse` | Optional detailed history; not necessary for MVP |
| `GET /v2/matches/{gameId}` | `MatchDetailsResponse` | Optional single-match detail |
| `GET /v2/matches/{dataSource}/{dataSourceId}` | `MatchDetailsResponse` | Optional source-specific lookup |

No pagination parameters, history completeness guarantee, profile aggregation window, numeric rate limit, or dedicated Premier timeline endpoint are documented. Do not call history a complete lifetime record.

## Mapping the requested stats

| User-facing value | JSON path in profile | Handling |
| --- | --- | --- |
| Aim | `rating.aim` | Preserve the 0–100 scale; do not add a percent sign |
| Utility | `rating.utility` | Preserve the supplied score and metric name |
| Leetify Rating | `ranks.leetify` | Nullable; preserve sign and scale |
| Current Premier rating | `ranks.premier` | Nullable; unavailable is distinct from zero |
| Recent wins/losses | `recent_matches[].outcome` | Sort by `finished_at`, newest first; render draw/unknown separately |
| Match context | `recent_matches[].id`, `map_name`, `finished_at` | Deduplicate by ID; retain source timestamps |
| Candidate chart points | `recent_matches[].rank`, `rank_type`, `finished_at` | Semantics must be verified before plotting |

Do not confuse overall `ranks.leetify` with side-specific `rating.ct_leetify` / `t_leetify` or per-match `leetify_rating`. The examples contain inconsistent-looking per-match scales and rank types. Do not derive a conversion factor or average a new overall rating from them. Verify formatting against the same account in Leetify before implementing any conversion.

The schema declares nullable ranks and some match ratings. It does not consistently declare required properties, and an example contains a null `rank_type` despite its string declaration. Parse conservatively: absent/null values become unavailable, extra fields are ignored, identifiers remain strings, and malformed numeric/date values must not crash a widget.

## Premier chart feasibility

**Decision (19 November 2026): defer the separate Premier chart widget.** The existing stats widget can display the current rating from `ranks.premier`. A dated series still needs stronger evidence.

The [current public Swagger schema](https://api-public-docs.cs-prod.leetify.com/swagger-ui-init.js) has no dedicated Premier history endpoint. `RecentMatch` gives `finished_at` and `data_source` alongside a numeric `rank` and a string `rank_type`. The schema's example uses `rank_type: "matchmaking"`, and the profile example includes a null rank type. Neither establishes the Premier discriminator. The schema also leaves the rank units, before/after timing, unranked values, and season association unspecified. The detailed match endpoints share a `PlayerStats` schema without Premier rank points. Leetify's Rank History UI separates Premier from other ladders and groups its summary by season, but the public schema does not establish how to map a recent-match row to that view.

No owner response was available for a match-by-match comparison. The [developer guidelines](https://leetify.com/blog/leetify-api-developer-guidelines/) ask integrations to refrain from storing API data. Sampled snapshots on disk would therefore need explicit provider permission, and acceptable launcher retention of rendered chart pixels remains unconfirmed.

Reopen the milestone when evidence is available:

1. Inspect an authorized owner's response in memory and compare several known Premier matches with the official UI.
2. Confirm Premier identification, point timing, units, missing/unranked values, and season behavior from reliable evidence; record the evidence without retaining personal payloads.
3. If valid, draw only points in the current response, with actual timestamps and an explicit limited-history label. Require at least two valid points. Do not mix FACEIT or competitive ranks into the series.
4. Keep the current rating in the stats widget while the separate timeline is deferred.

## Constraints that affect architecture

The guidelines request no storage of API data and deletion when previously available data disappears. They also require supplied attribution artwork, appropriate links, unchanged metric names/scales, and independent branding. Use a neutral public app name; the repository's existing name is not a release branding decision.

Default design: no Room database, JSON files, disk HTTP cache, persistent Glance stats state, saved API bitmaps, telemetry payloads, or historical snapshots. Keep responses only as transient processing state. Settings and refresh metadata may persist without stats. An Android launcher necessarily retains a rendered widget; confirm acceptable display retention and attribution treatment with Leetify before release. Do not describe transient rendering as confirmed permission to cache.

Clear app-owned API state and overwrite every widget on account changes, reset, and confirmed loss of access. On transient failures show an unavailable/retry state rather than retaining an offline stats archive. A durable last-known-good cache needs explicit provider permission and a documented expiry/deletion design first.

## Android implementation references

- [Jetpack Glance](https://developer.android.com/develop/ui/compose/glance): Kotlin widget UI; uses its own composables, not arbitrary Compose UI components.
- [WorkManager requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work): periodic work has a 15-minute minimum and inexact execution. Proposed default is 60 minutes plus manual refresh, not real-time monitoring.

Use a platform Canvas-rendered in-memory bitmap for a small static rank chart if its data gate passes. No chart dependency is needed initially. Validate launcher/process-death behavior without persisting the API response.

## Remaining evidence needed

- Owner's SteamID64 and optional personal API key, entered locally during implementation, never committed.
- Real authenticated responses for missing ranks, privacy restrictions, outcome vocabulary, and rate-limit behavior.
- Premier history semantics and exact per-match rating display rules.
- Provider clarification of widget render retention and any desired offline cache; approved attribution assets before release.
- Installed SDK/JDK and dependency compatibility checks. This research did not run an Android build.
