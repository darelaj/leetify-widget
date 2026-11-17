package id.my.chromazone.leetify_widget.ui

import id.my.chromazone.leetify_widget.data.AccountCredentials
import id.my.chromazone.leetify_widget.data.AccountStorage
import id.my.chromazone.leetify_widget.data.AccountStorageException
import id.my.chromazone.leetify_widget.data.LeetifyApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val id = "76561190000000000"
    private val otherId = "76561190000000001"
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun savesRetainsReplacesAndRemovesOwnerKey() = runTest(dispatcher) {
        val storage = MemoryStorage()
        val requests = mutableListOf<Request>()
        val model = model(storage) { requests += it; response(it) }
        advanceUntilIdle()
        model.editSteamId(id)
        model.editApiKey("first-key")
        model.verify()
        advanceUntilIdle()
        assertEquals(AccountMessage.VERIFIED, model.state.value.message)
        assertEquals("first-key", storage.saved!!.apiKey)
        assertEquals("", model.state.value.apiKey)
        assertTrue(model.state.value.hasSavedKey)
        requests.clear()
        model.verify()
        advanceUntilIdle()
        assertEquals("Bearer first-key", requests.last().header("Authorization"))
        model.editApiKey("replacement")
        model.verify()
        advanceUntilIdle()
        assertEquals("replacement", storage.saved!!.apiKey)
        model.removeSavedKey()
        model.verify()
        advanceUntilIdle()
        assertNull(storage.saved!!.apiKey)
        assertNull(requests.last().header("Authorization"))
    }

    @Test fun failedVerificationDoesNotReplaceSavedAccountOrSendKeyToAnotherAccount() = runTest(dispatcher) {
        val storage = MemoryStorage(AccountCredentials(id, "saved-key"))
        val model = model(storage) {
            assertNull(it.header("Authorization"))
            response(it, 404)
        }
        advanceUntilIdle()
        assertEquals(id, model.state.value.steamId)
        assertEquals("", model.state.value.apiKey)
        model.editSteamId(otherId)
        model.verify()
        advanceUntilIdle()
        assertEquals(AccountMessage.UNAVAILABLE, model.state.value.message)
        assertEquals(id, storage.saved!!.steamId)
    }

    @Test fun editDuringBlockingFetchDiscardsOldResponseBeforeSaving() = runTest(dispatcher) {
        val storage = MemoryStorage()
        lateinit var model: AccountViewModel
        model = model(storage) {
            model.editSteamId(otherId)
            response(it)
        }
        advanceUntilIdle()
        model.editSteamId(id)
        model.verify()
        advanceUntilIdle()
        assertNull(storage.saved)
        assertEquals(otherId, model.state.value.steamId)
        assertNull(model.state.value.message)
        assertFalse(model.state.value.busy)
    }

    @Test fun resetDuringBlockingFetchCannotRestoreAccount() = runTest(dispatcher) {
        val storage = MemoryStorage(AccountCredentials(id, null))
        lateinit var model: AccountViewModel
        model = model(storage) { model.reset(); response(it) }
        advanceUntilIdle()
        model.verify()
        advanceUntilIdle()
        assertNull(storage.saved)
        assertEquals("", model.state.value.steamId)
        assertFalse(model.state.value.configured)
    }

    @Test fun resetWaitsForInFlightPersistenceAndThenDeletesIt() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val storage = MemoryStorage().apply { beforeSave = { withContext(NonCancellable) { gate.await() } } }
        val model = model(storage, ::response)
        advanceUntilIdle()
        model.editSteamId(id)
        model.verify()
        runCurrent()
        model.reset()
        runCurrent()
        assertTrue(model.state.value.loading)
        gate.complete(Unit)
        advanceUntilIdle()
        assertNull(storage.saved)
        assertFalse(model.state.value.loading)
        assertFalse(model.state.value.configured)
    }

    @Test fun inaccessibleSettingsFailClosedAndResetRecovers() = runTest(dispatcher) {
        val storage = MemoryStorage().apply { unreadable = true }
        val model = model(storage) { error("No request expected") }
        advanceUntilIdle()
        assertEquals(AccountMessage.STORAGE_FAILURE, model.state.value.message)
        model.reset()
        advanceUntilIdle()
        assertNull(model.state.value.message)
        assertFalse(model.state.value.configured)
    }

    @Test fun notifiesWidgetIntegrationAfterSaveAndReset() = runTest(dispatcher) {
        val storage = MemoryStorage()
        var changes = 0
        val api = LeetifyApi(OkHttpClient.Builder().addInterceptor { response(it.request()) }.build())
        val model = AccountViewModel(api, storage, dispatcher, onAccountChanged = { changes++ })
        advanceUntilIdle()
        model.editSteamId(id)
        model.verify()
        advanceUntilIdle()
        assertEquals(1, changes)
        model.reset()
        advanceUntilIdle()
        assertEquals(2, changes)
        assertNull(storage.saved)
    }

    private fun model(storage: AccountStorage, reply: (Request) -> Response) = AccountViewModel(
        LeetifyApi(OkHttpClient.Builder().addInterceptor { reply(it.request()) }.build()), storage, dispatcher,
    )

    private fun response(request: Request, code: Int = 200) = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(code).message("Synthetic")
        .body("""{"ranks":{}}""".toResponseBody()).build()

    private class MemoryStorage(var saved: AccountCredentials? = null) : AccountStorage {
        var unreadable = false
        var beforeSave: suspend () -> Unit = {}
        override suspend fun load(): AccountCredentials? {
            if (unreadable) throw AccountStorageException()
            return saved
        }
        override suspend fun save(account: AccountCredentials) { beforeSave(); saved = account }
        override suspend fun clear() { saved = null; unreadable = false }
    }
}
