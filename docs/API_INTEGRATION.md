# Profile integration

Implemented on `feat/leetify-profile`, 18 November 2026. The official Swagger initialization/schema was rechecked before implementation; the endpoint and field mappings in [API research](API_RESEARCH.md) still apply.

## Calling contract

`data.LeetifyApi` exposes blocking, worker-thread-only methods:

- `fetchProfile(steamId, apiKey = null)` returns a transient `PlayerProfile`.
- `validateApiKey(apiKey)` checks the key endpoint; success returns normally. Validation is explicit so background refresh does not spend an extra request revalidating every time.

The setup/settings feature must call these away from the main thread (for example, `Dispatchers.IO`), handle validation errors and typed API failures, and invalidate old-account results. Blocking calls are bounded by their timeout; coroutine cancellation alone does not immediately interrupt them. No activity or widget calls the API yet.

Production defaults: HTTPS public API only, 10-second connection / 15-second read / 20-second whole-call timeouts, no disk cache, `Cache-Control: no-store`, connection-failure retries disabled, and redirects disabled. No logging interceptors, persistent responses, history calls, or embedded keys. Profile bodies are bounded to 1 MiB even with unknown Content-Length. Responses close on success or failure; raw error bodies and underlying exceptions are not exposed in typed errors.

SteamID64 stays a string and must contain exactly 17 ASCII digits. Keys are optional for fetch; supplied keys must be nonblank for validation and printable ASCII without whitespace after trimming. Invalid local inputs raise a fixed-message `IllegalArgumentException`, without making a request.

## Mapping

The mapper reads JSON objects directly and extracts only the fields needed by the app. Extra fields are ignored; absent/null/non-numeric/non-finite ratings become unavailable. Provider numbers retain their original scale and sign; formatting is left to the future UI. An explicit mismatching profile ID is rejected.

Matches are sorted by parsed absolute timestamp descending, deduplicated by ID (newest occurrence wins), and limited to five. Records with missing/blank IDs are skipped. Invalid/missing dates remain null and sort after valid dates, retaining source order among ties. Only `win`, `loss`, and `draw` map to known outcomes; other values remain unknown. This includes all returned ladders; no Premier filtering or rank history is inferred.

Kotlin's `Instant` handles source date-time offsets without adding Android API 26-only `java.time` calls. Empty/malformed root payloads fail explicitly; partial recognized profiles can still contain unavailable fields.

## Failures and retries

`ProfileApiException.failure` distinguishes unauthorized (401), access denied (403), profile unavailable (404 or an explicit non-public privacy mode), rate limited (429), server failure (5xx), other HTTP status, network failure, timeout, and invalid response. A network failure does not prove the device is offline, and a 404 does not prove profile privacy. Unrecognized non-public privacy modes are withheld conservatively. No raw response body, request URL, key, or original exception cause is attached.

`retryDelayMillis(failedAttempt, nowMillis)` gives scheduling guidance, not an automatic sleep/retry loop. Failed attempts are one-based: network, timeout, rate limit, and server failures permit two further attempts after at least 60 and 120 seconds. Attempt three ends retrying. Other failures do not retry. Rate-limit/server responses can extend those delays through Retry-After seconds or HTTP dates. The caller must honor these delays and the attempt cap when WorkManager is implemented; it must not restart attempt counting on each retry. No scheduler is introduced in this feature.

## Verification and limits

Tests use synthetic JSON and an OkHttp interceptor as the HTTP boundary; no Leetify account, API key, or personal response was used. They cover endpoint/header selection, optional keys, local input rejection, malformed/oversized/unknown-length bodies, exact stat fields, timestamp ordering, duplicate matches, unknown outcomes, null/invalid numbers, identity mismatch, HTTP/network/timeout failures, privacy handling, and bounded Retry-After decisions.

The earlier foundation's single placeholder test remains unchanged. No setup UI, launcher widget, live API behavior, device connectivity, or provider permission has been verified by these tests. Premier chart semantics remain unresolved.

Validation on 18 November 2026: `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` passed. All 16 tests passed (15 integration/mapping/error tests and the starter's one placeholder). Lint reported 0 errors and the same 13 existing warnings. Git whitespace checks passed. The final verification reused unchanged build outputs where applicable; compilation and lint also passed in the preceding full run after the production changes.
