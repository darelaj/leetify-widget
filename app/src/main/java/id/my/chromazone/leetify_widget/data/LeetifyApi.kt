package id.my.chromazone.leetify_widget.data

import androidx.annotation.WorkerThread
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal class LeetifyApi(
    private val client: OkHttpClient = defaultClient(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    // Blocking by design: call from a worker or Dispatchers.IO, never the UI thread.
    @WorkerThread
    fun fetchProfile(steamId: String, apiKey: String? = null): PlayerProfile {
        require(steamId.matches(Regex("[0-9]{17}"))) { "Enter a 17-digit SteamID64" }
        return request("v3/profile", apiKey, steamId) { response ->
            val body = response.body
            if (body.contentLength() > MAX_BODY_BYTES) throw ProfileApiException(ProfileFailure.INVALID_RESPONSE)
            val source = body.source()
            if (source.request(MAX_BODY_BYTES + 1)) throw ProfileApiException(ProfileFailure.INVALID_RESPONSE)
            try {
                val profile = Json.parseToJsonElement(source.readUtf8()) as? JsonObject
                    ?: throw ProfileApiException(ProfileFailure.INVALID_RESPONSE)
                val privacyMode = profile["privacy_mode"].text()
                if (privacyMode != null && privacyMode != "public") {
                    throw ProfileApiException(ProfileFailure.PROFILE_UNAVAILABLE)
                }
                profile.toPlayerProfile(steamId)
            } catch (_: SerializationException) {
                throw ProfileApiException(ProfileFailure.INVALID_RESPONSE)
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
        try {
            return client.newCall(request).execute().use { response ->
                if (response.code != 200) {
                    val failure = when (response.code) {
                        401 -> ProfileFailure.UNAUTHORIZED
                        403 -> ProfileFailure.ACCESS_DENIED
                        404 -> ProfileFailure.PROFILE_UNAVAILABLE
                        429 -> ProfileFailure.RATE_LIMITED
                        in 500..599 -> ProfileFailure.SERVER
                        else -> ProfileFailure.HTTP_ERROR
                    }
                    val retryAt = if (failure == ProfileFailure.RATE_LIMITED || failure == ProfileFailure.SERVER) {
                        retryAtMillis(response.header("Retry-After"), nowMillis())
                    } else null
                    throw ProfileApiException(failure, retryAt)
                }
                read(response)
            }
        } catch (error: ProfileApiException) {
            throw error
        } catch (_: InterruptedIOException) {
            throw ProfileApiException(ProfileFailure.TIMEOUT)
        } catch (_: IOException) {
            throw ProfileApiException(ProfileFailure.NETWORK)
        }
    }

    companion object {
        private const val BASE_URL = "https://api-public.cs-prod.leetify.com/"
        private const val MAX_BODY_BYTES = 1_048_576L

        fun forAccountSetup() = LeetifyApi(defaultClient(callTimeoutSeconds = 8))

        private fun defaultClient(callTimeoutSeconds: Long = 20) = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(callTimeoutSeconds, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
            .cache(null)
            .build()
    }
}
