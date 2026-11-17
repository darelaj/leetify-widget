package id.my.chromazone.leetify_widget.data

import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class LeetifyApiTest {
    private val steamId = "76561190000000000"

    @Test
    fun profileUsesOnePublicRequestAndOptionalOwnerKey() {
        val requests = mutableListOf<Request>()
        val api = api { request ->
            requests += request
            response(request, body = """{"rating":{"aim":61},"extra":"ignored"}""")
        }
        assertEquals(61.0, api.fetchProfile(steamId, "synthetic-key").aim!!, 0.0)
        api.fetchProfile(steamId)
        assertEquals(2, requests.size)
        assertEquals("https", requests[0].url.scheme)
        assertEquals("api-public.cs-prod.leetify.com", requests[0].url.host)
        assertEquals("/v3/profile", requests[0].url.encodedPath)
        assertEquals(setOf("steam64_id"), requests[0].url.queryParameterNames)
        assertEquals(steamId, requests[0].url.queryParameter("steam64_id"))
        assertEquals("Bearer synthetic-key", requests[0].header("Authorization"))
        assertEquals("no-store", requests[0].header("Cache-Control"))
        assertNull(requests[1].header("Authorization"))
    }

    @Test
    fun validatesKeyWithoutFetchingProfile() {
        val api = api { request ->
            assertEquals("/api-key/validate", request.url.encodedPath)
            assertEquals("Bearer synthetic-key", request.header("Authorization"))
            assertNull(request.url.query)
            response(request)
        }
        api.validateApiKey("synthetic-key")
    }

    @Test
    fun rejectsBadInputBeforeMakingRequest() {
        val api = api { error("No request expected") }
        assertThrows(IllegalArgumentException::class.java) { api.fetchProfile("123") }
        assertThrows(IllegalArgumentException::class.java) { api.fetchProfile(steamId, "bad\nkey") }
        assertThrows(IllegalArgumentException::class.java) { api.validateApiKey(" ") }
    }

    @Test
    fun rejectsMalformedAndOversizedBodiesWithoutExposingThem() {
        for (body in listOf("secret-not-json", "[]", "x".repeat(1_048_577))) {
            val api = api { response(it, body = body) }
            val error = assertThrows(IOException::class.java) { api.fetchProfile(steamId) }
            assertFalse(error.message.orEmpty().contains("secret-not-json"))
            assertEquals(ProfileFailure.INVALID_RESPONSE, (error as ProfileApiException).failure)
            assertNull(error.cause)
        }
    }

    @Test
    fun boundsStreamingBodiesWhenContentLengthIsUnknown() {
        val buffer = Buffer().writeUtf8("x".repeat(1_048_577))
        val body = object : ResponseBody() {
            override fun contentType() = null
            override fun contentLength() = -1L
            override fun source() = buffer
        }
        val api = api { response(it).newBuilder().body(body).build() }
        assertEquals(ProfileFailure.INVALID_RESPONSE,
            assertThrows(ProfileApiException::class.java) { api.fetchProfile(steamId) }.failure)
    }

    @Test
    fun mapsHttpFailuresWithoutRetryingOrReadingErrorBodies() {
        val failures = mapOf(
            401 to ProfileFailure.UNAUTHORIZED, 403 to ProfileFailure.ACCESS_DENIED,
            404 to ProfileFailure.PROFILE_UNAVAILABLE, 429 to ProfileFailure.RATE_LIMITED,
            503 to ProfileFailure.SERVER, 302 to ProfileFailure.HTTP_ERROR, 400 to ProfileFailure.HTTP_ERROR,
        )
        for ((status, expected) in failures) {
            var calls = 0
            val api = api {
                calls++
                response(it, status, "secret-error-body").newBuilder().header("Retry-After", "120").build()
            }
            val error = assertThrows(ProfileApiException::class.java) { api.fetchProfile(steamId) }
            assertEquals(expected, error.failure)
            assertEquals(1, calls)
            assertEquals(expected.name, error.message)
            assertNull(error.cause)
            if (status == 429 || status == 503) {
                assertEquals(121_000L, error.retryAtMillis)
            } else assertNull(error.retryAtMillis)
        }
    }

    @Test
    fun distinguishesTransportFailureFromTimeoutWithoutLeakingDetails() {
        for ((cause, expected) in listOf(
            IOException("secret-url-and-key") to ProfileFailure.NETWORK,
            SocketTimeoutException("secret-url-and-key") to ProfileFailure.TIMEOUT,
        )) {
            val error = assertThrows(ProfileApiException::class.java) {
                api { throw cause }.fetchProfile(steamId)
            }
            assertEquals(expected, error.failure)
            assertEquals(expected.name, error.message)
            assertNull(error.cause)
        }
    }

    @Test
    fun privateProfileIsUnavailableAndKeyValidationPreservesUnauthorized() {
        val unavailable = api { response(it, body = """{"privacy_mode":"private","rating":{"aim":90}}""") }
        assertEquals(ProfileFailure.PROFILE_UNAVAILABLE,
            assertThrows(ProfileApiException::class.java) { unavailable.fetchProfile(steamId) }.failure)
        val unauthorized = api { response(it, 401) }
        assertEquals(ProfileFailure.UNAUTHORIZED,
            assertThrows(ProfileApiException::class.java) { unauthorized.validateApiKey("synthetic-key") }.failure)
    }

    private fun api(reply: (Request) -> Response) = LeetifyApi(
        OkHttpClient.Builder().addInterceptor { reply(it.request()) }.build(),
        nowMillis = { 1_000L },
    )

    private fun response(request: Request, code: Int = 200, body: String = "") = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(code).message("Synthetic response")
        .body(body.toResponseBody()).build()
}
