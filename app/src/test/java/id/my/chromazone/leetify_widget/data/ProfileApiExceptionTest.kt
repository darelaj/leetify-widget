package id.my.chromazone.leetify_widget.data

import org.junit.Assert.*
import org.junit.Test

class ProfileApiExceptionTest {
    @Test
    fun retryAfterAcceptsSecondsAndHttpDateWithoutShorteningServerDelay() {
        assertEquals(121_000L, retryAtMillis("120", 1_000L))
        assertEquals(0L, retryAtMillis("Thu, 01 Jan 1970 00:00:00 GMT", 1_000L))
        assertEquals(120_000L, retryAtMillis("Thu, 01 Jan 1970 00:02:00 GMT", 1_000L))
        val error = ProfileApiException(ProfileFailure.RATE_LIMITED, retryAtMillis("3600", 1_000L))
        assertEquals(3_600_000L, error.retryDelayMillis(1, 1_000L))
        assertEquals(3_590_000L, error.retryDelayMillis(1, 11_000L))
    }

    @Test
    fun invalidRetryHeadersFallBackAndHugeDelaysDoNotOverflow() {
        for (value in listOf(null, "", "-1", "bad", "1.5", "9999999999999999999999",
            "Thu, 01 Jan 1970 00:02:00 GMT trailing")) {
            assertNull(retryAtMillis(value, 1_000L))
        }
        assertEquals(Long.MAX_VALUE, retryAtMillis(Long.MAX_VALUE.toString(), 100_000L))
        assertEquals(60_000L, ProfileApiException(ProfileFailure.RATE_LIMITED, 0L).retryDelayMillis(1, 1_000L))
    }

    @Test
    fun retryableFailuresStopAfterThreeTotalAttempts() {
        for (failure in ProfileFailure.entries) {
            val error = ProfileApiException(failure)
            val retryable = failure in setOf(ProfileFailure.RATE_LIMITED, ProfileFailure.SERVER,
                ProfileFailure.NETWORK, ProfileFailure.TIMEOUT)
            assertEquals(if (retryable) 60_000L else null, error.retryDelayMillis(1, 0))
            assertEquals(if (retryable) 120_000L else null, error.retryDelayMillis(2, 0))
            assertNull(error.retryDelayMillis(3, 0))
            assertNull(error.retryDelayMillis(4, 0))
        }
    }
}
