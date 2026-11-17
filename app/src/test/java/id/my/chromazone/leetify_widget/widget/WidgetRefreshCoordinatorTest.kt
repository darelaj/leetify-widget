package id.my.chromazone.leetify_widget.widget

import id.my.chromazone.leetify_widget.data.AccountCredentials
import id.my.chromazone.leetify_widget.data.AccountStorage
import id.my.chromazone.leetify_widget.data.PlayerProfile
import id.my.chromazone.leetify_widget.data.ProfileApiException
import id.my.chromazone.leetify_widget.data.ProfileFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WidgetRefreshCoordinatorTest {
    private val id = "76561190000000000"
    private val profile = PlayerProfile(id, "Synthetic", 60.0, 0.0, -2.12, null, emptyList())

    @Test fun fetchesOnceAndPublishesOnlyTransientSnapshots() = runTest {
        val storage = MemoryStorage(AccountCredentials(id, null, "first"))
        val snapshots = mutableListOf<WidgetSnapshot>()
        var calls = 0
        WidgetRefreshCoordinator(storage, { calls++; profile }, { snapshots += it }, { 123L }).refresh()
        assertEquals(1, calls)
        assertEquals(listOf(WidgetStatus.LOADING, WidgetStatus.READY), snapshots.map { it.status })
        assertEquals(123L, snapshots.last().fetchedAtMillis)
        assertSame(profile, snapshots.last().profile)
    }

    @Test fun unsetAccountDoesNotFetchAndFailuresNeverContainOldStats() = runTest {
        val storage = MemoryStorage(null)
        val snapshots = mutableListOf<WidgetSnapshot>()
        WidgetRefreshCoordinator(storage, { error("No request expected") }, { snapshots += it }).refresh()
        assertEquals(WidgetStatus.SETUP, snapshots.single().status)
        storage.saved = AccountCredentials(id, null, "first")
        for (failure in listOf(ProfileFailure.NETWORK, ProfileFailure.PROFILE_UNAVAILABLE, ProfileFailure.RATE_LIMITED)) {
            snapshots.clear()
            WidgetRefreshCoordinator(storage, { throw ProfileApiException(failure) }, { snapshots += it }).refresh()
            assertEquals(WidgetStatus.ERROR, snapshots.last().status)
            assertEquals(failure, snapshots.last().failure)
            assertNull(snapshots.last().profile)
        }
    }

    @Test fun resetOrReconfiguredSameAccountCannotPublishOldResponse() = runTest {
        for (reset in listOf(true, false)) {
            val storage = MemoryStorage(AccountCredentials(id, null, "first"))
            val snapshots = mutableListOf<WidgetSnapshot>()
            WidgetRefreshCoordinator(storage, {
                storage.saved = if (reset) null else AccountCredentials(id, null, "second")
                profile
            }, { snapshots += it }).refresh()
            assertEquals(listOf(WidgetStatus.LOADING), snapshots.map { it.status })
        }
    }

    private class MemoryStorage(var saved: AccountCredentials?) : AccountStorage {
        override suspend fun load() = saved
        override suspend fun save(account: AccountCredentials) { saved = account }
        override suspend fun clear() { saved = null }
    }
}
