package id.my.chromazone.leetify_widget.data

import java.io.IOException

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
