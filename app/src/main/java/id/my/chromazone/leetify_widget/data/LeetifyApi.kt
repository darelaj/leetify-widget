package id.my.chromazone.leetify_widget.data

import androidx.annotation.WorkerThread
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal class LeetifyApi(private val client: OkHttpClient = defaultClient()) {
    // Blocking by design: call from a worker or Dispatchers.IO, never the UI thread.
    @WorkerThread
    fun fetchProfile(steamId: String, apiKey: String? = null): JsonObject {
        require(steamId.matches(Regex("[0-9]{17}"))) { "Enter a 17-digit SteamID64" }
        return request("v3/profile", apiKey, steamId) { response ->
            val body = response.body
            if (body.contentLength() > MAX_BODY_BYTES) throw IOException("Response too large")
            val source = body.source()
            if (source.request(MAX_BODY_BYTES + 1)) throw IOException("Response too large")
            try {
                Json.parseToJsonElement(source.readUtf8()) as? JsonObject
                    ?: throw IOException("Expected a profile object")
            } catch (_: SerializationException) {
                throw IOException("Invalid profile response")
            }
        }
    }

    @WorkerThread
    fun validateApiKey(apiKey: String) {
        require(apiKey.isNotBlank()) { "Enter an API key" }
        request("api-key/validate", apiKey) { }
    }

    private fun <T> request(
        path: String,
        apiKey: String?,
        steamId: String? = null,
        read: (okhttp3.Response) -> T,
    ): T {
        val key = apiKey?.trim()?.takeIf { it.isNotEmpty() }
        require(key == null || key.all { it in '!'..'~' }) { "Invalid API key format" }
        val url = BASE_URL.toHttpUrl().newBuilder().addPathSegments(path).apply {
            if (steamId != null) addQueryParameter("steam64_id", steamId)
        }.build()
        val request = Request.Builder().url(url).header("Accept", "application/json")
            .header("Cache-Control", "no-store").apply {
                if (key != null) header("Authorization", "Bearer $key")
            }.build()
        return client.newCall(request).execute().use { response ->
            if (response.code != 200) throw IOException("API request failed (${response.code})")
            read(response)
        }
    }

    companion object {
        private const val BASE_URL = "https://api-public.cs-prod.leetify.com/"
        private const val MAX_BODY_BYTES = 1_048_576L

        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
            .cache(null)
            .build()
    }
}
