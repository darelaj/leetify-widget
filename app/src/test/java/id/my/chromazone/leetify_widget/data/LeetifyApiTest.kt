package id.my.chromazone.leetify_widget.data

import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
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
        assertTrue(api.fetchProfile(steamId, "synthetic-key").containsKey("rating"))
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
        }
    }

    private fun api(reply: (Request) -> Response) = LeetifyApi(
        OkHttpClient.Builder().addInterceptor { reply(it.request()) }.build(),
    )

    private fun response(request: Request, code: Int = 200, body: String = "") = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(code).message("Synthetic response")
        .body(body.toResponseBody()).build()
}
