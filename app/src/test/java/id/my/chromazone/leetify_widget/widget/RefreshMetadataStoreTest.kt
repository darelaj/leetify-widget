package id.my.chromazone.leetify_widget.widget

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import id.my.chromazone.leetify_widget.data.ProfileApiException
import id.my.chromazone.leetify_widget.data.ProfileFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RefreshMetadataStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private fun metadata(): RefreshMetadataStore = RefreshMetadataStore(
        PreferenceDataStoreFactory.create(scope = scope) { folder.root.resolve("refresh.preferences_pb") },
    )
    private suspend fun close() = scope.coroutineContext[Job]!!.cancelAndJoin()

    @Test fun concurrentManualTapsSharePersistedCooldown() = runBlocking {
        try {
            val store = metadata()
            val accepted = (1..8).map { async(Dispatchers.IO) { store.claimManual(1_000) } }.awaitAll()
            assertEquals(1, accepted.count { it })
            close()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val reopened = metadata()
            assertFalse(reopened.claimManual(60_999))
            assertTrue(reopened.claimManual(61_000))
        } finally { close() }
    }

    @Test fun retryAfterSurvivesRestartAndWaitingDoesNotConsumeAttempts() = runBlocking {
        try {
            val store = metadata()
            assertEquals(AllowedRefresh(1), store.startAttempt("work", "account", 1_000))
            assertTrue(store.finishAttempt("work", "account", 1,
                ProfileApiException(ProfileFailure.RATE_LIMITED, 601_000), 1_000))
            repeat(10) { assertEquals(RefreshPermit.Wait, store.startAttempt("work", "account", 600_999)) }
            close()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val reopened = metadata()
            assertEquals(AllowedRefresh(2), reopened.startAttempt("work", "account", 601_000))
            assertTrue(reopened.finishAttempt("work", "account", 2, ProfileApiException(ProfileFailure.SERVER), 601_000))
            assertEquals(RefreshPermit.Wait, reopened.startAttempt("work", "account", 720_999))
            assertEquals(AllowedRefresh(3), reopened.startAttempt("work", "account", 721_000))
            assertFalse(reopened.finishAttempt("work", "account", 3,
                ProfileApiException(ProfileFailure.RATE_LIMITED, 1_000_000), 721_000))
            assertEquals(RefreshPermit.Finished, reopened.startAttempt("work", "account", 1_000_000))
            assertEquals(RefreshPermit.Wait, reopened.startAttempt("next-work", "account", 999_999))
            assertEquals(AllowedRefresh(1), reopened.startAttempt("next-work", "account", 1_000_000))
        } finally { close() }
    }

    @Test fun interruptedAttemptStillConsumesBudgetAndSuccessfulWorkDoesNotRepeat() = runBlocking {
        try {
            val store = metadata()
            assertEquals(AllowedRefresh(1), store.startAttempt("work", "account", 1_000))
            assertEquals(RefreshPermit.Wait, store.startAttempt("work", "account", 60_999))
            assertEquals(AllowedRefresh(2), store.startAttempt("work", "account", 61_000))
            assertFalse(store.finishAttempt("work", "account", 2, null, 61_000))
            assertEquals(RefreshPermit.Finished, store.startAttempt("work", "account", 121_000))
            assertFalse(store.claimManual(120_999))
            assertEquals(AllowedRefresh(1), store.startAttempt("new", "account", 121_000))
        } finally { close() }
    }

    @Test fun resetRejectsStaleCompletionAndRetainsServerWait() = runBlocking {
        try {
            val store = metadata()
            store.startAttempt("old", "first", 1_000)
            store.finishAttempt("old", "first", 1, ProfileApiException(ProfileFailure.RATE_LIMITED, 600_000), 1_000)
            store.resetAccount()
            assertFalse(store.finishAttempt("old", "first", 1, ProfileApiException(ProfileFailure.SERVER), 2_000))
            assertFalse(store.claimManual(599_999))
            assertEquals(RefreshPermit.Wait, store.startAttempt("new", "second", 599_999))
            assertEquals(AllowedRefresh(1), store.startAttempt("new", "second", 600_000))
        } finally { close() }
    }

    @Test fun permanentFailuresDoNotRetryAndDeadlineDoesNotOverflow() = runBlocking {
        try {
            val store = metadata()
            store.startAttempt("work", "account", 1_000)
            assertFalse(store.finishAttempt("work", "account", 1, ProfileApiException(ProfileFailure.UNAUTHORIZED), 1_000))
            assertEquals(RefreshPermit.Finished, store.startAttempt("work", "account", 61_000))
            assertTrue(store.claimManual(Long.MAX_VALUE - 1))
            assertFalse(store.claimManual(Long.MAX_VALUE - 1))
            assertTrue(store.claimManual(Long.MAX_VALUE))
        } finally { close() }
    }

    @Test fun obsoleteWorkCannotChangeCurrentRetryTiming() = runBlocking {
        try {
            val store = metadata()
            store.startAttempt("old", "first", 1_000)
            assertEquals(AllowedRefresh(1), store.startAttempt("new", "second", 61_000))
            assertFalse(store.finishAttempt("old", "first", 1,
                ProfileApiException(ProfileFailure.RATE_LIMITED, 900_000), 62_000))
            assertFalse(store.finishAttempt("new", "second", 1, null, 62_000))
            assertTrue(store.claimManual(121_000))
        } finally { close() }
    }
}
