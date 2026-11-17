package id.my.chromazone.leetify_widget.data

import java.io.IOException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal enum class ProfileFailure {
    UNAUTHORIZED, ACCESS_DENIED, PROFILE_UNAVAILABLE, RATE_LIMITED,
    SERVER, HTTP_ERROR, NETWORK, TIMEOUT, INVALID_RESPONSE,
}

// Deliberately excludes response bodies, request URLs, credentials, and raw causes.
internal class ProfileApiException(
    val failure: ProfileFailure,
    val retryAtMillis: Long? = null,
) : IOException(failure.name) {
    /** Failed attempts are one-based; at most three total attempts, scheduled by the caller. */
    fun retryDelayMillis(failedAttempt: Int, nowMillis: Long): Long? {
        require(failedAttempt >= 1)
        if (failedAttempt >= 3 || failure !in setOf(
                ProfileFailure.RATE_LIMITED, ProfileFailure.SERVER,
                ProfileFailure.NETWORK, ProfileFailure.TIMEOUT,
            )
        ) return null
        val backoff = 60_000L * failedAttempt
        val serverDelay = retryAtMillis?.let { if (it > nowMillis) it - nowMillis else 0 } ?: 0
        return maxOf(backoff, serverDelay)
    }
}

internal fun retryAtMillis(header: String?, nowMillis: Long): Long? {
    val value = header?.trim() ?: return null
    if (value.matches(Regex("[0-9]+"))) {
        val seconds = value.toLongOrNull() ?: return null
        val delay = seconds.coerceAtMost(Long.MAX_VALUE / 1000) * 1000
        return if (nowMillis > Long.MAX_VALUE - delay) Long.MAX_VALUE else nowMillis + delay
    }
    val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("GMT")
    }
    val position = ParsePosition(0)
    val date = format.parse(value, position) ?: return null
    return date.time.takeIf { position.index == value.length }
}
