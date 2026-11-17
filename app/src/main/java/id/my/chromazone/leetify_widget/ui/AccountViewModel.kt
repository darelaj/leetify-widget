package id.my.chromazone.leetify_widget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.chromazone.leetify_widget.data.LeetifyApi
import id.my.chromazone.leetify_widget.data.AccountCredentials
import id.my.chromazone.leetify_widget.data.AccountStorage
import id.my.chromazone.leetify_widget.data.AccountStorageException
import id.my.chromazone.leetify_widget.data.ProfileApiException
import id.my.chromazone.leetify_widget.data.ProfileFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AccountViewModel(
    private val api: LeetifyApi,
    private val storage: AccountStorage,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AccountState())
    val state = mutableState.asStateFlow()
    private var verification: Job? = null
    @Volatile private var generation = 0L
    private val writes = Mutex()

    init {
        viewModelScope.launch {
            try {
                val saved = withContext(io) { storage.load() }
                mutableState.value = AccountState(steamId = saved?.steamId.orEmpty(), loading = false,
                    configured = saved != null, hasSavedKey = saved?.apiKey != null)
            } catch (_: AccountStorageException) {
                mutableState.value = AccountState(loading = false, message = AccountMessage.STORAGE_FAILURE)
            }
        }
    }

    fun editSteamId(value: String) {
        if (state.value.loading) return
        invalidate()
        mutableState.value = state.value.copy(steamId = value, busy = false, message = null)
    }

    fun editApiKey(value: String) {
        if (state.value.loading) return
        invalidate()
        mutableState.value = state.value.copy(apiKey = value, busy = false, message = null)
    }

    fun verify() {
        if (state.value.loading || state.value.busy) return
        val input = state.value
        val steamId = input.steamId.trim()
        val key = input.apiKey.trim()
        val invalid = validateAccountInput(steamId, key)
        if (invalid != null) {
            mutableState.value = input.copy(message = invalid)
            return
        }
        invalidate()
        val ticket = generation
        mutableState.value = input.copy(busy = true, message = null)
        verification = viewModelScope.launch {
            var savedHasKey = input.hasSavedKey
            val message = withContext(io) {
                try {
                    val saved = storage.load()
                    val effectiveKey = key.ifEmpty { null } ?: saved?.apiKey?.takeIf {
                        !input.removeSavedKey && saved.steamId == steamId
                    }
                    if (effectiveKey != null) api.validateApiKey(effectiveKey)
                    currentCoroutineContext().ensureActive()
                    api.fetchProfile(steamId, effectiveKey)
                    currentCoroutineContext().ensureActive()
                    writes.withLock {
                        if (ticket != generation) return@withContext null
                        storage.save(AccountCredentials(steamId, effectiveKey))
                    }
                    savedHasKey = effectiveKey != null
                    AccountMessage.VERIFIED
                } catch (_: AccountStorageException) {
                    AccountMessage.STORAGE_FAILURE
                } catch (error: ProfileApiException) {
                    when (error.failure) {
                        ProfileFailure.UNAUTHORIZED -> AccountMessage.UNAUTHORIZED
                        ProfileFailure.ACCESS_DENIED -> AccountMessage.ACCESS_DENIED
                        ProfileFailure.PROFILE_UNAVAILABLE -> AccountMessage.UNAVAILABLE
                        ProfileFailure.RATE_LIMITED -> AccountMessage.RATE_LIMITED
                        ProfileFailure.SERVER -> AccountMessage.SERVER
                        ProfileFailure.NETWORK -> AccountMessage.NETWORK
                        ProfileFailure.TIMEOUT -> AccountMessage.TIMEOUT
                        ProfileFailure.INVALID_RESPONSE -> AccountMessage.INVALID_RESPONSE
                        ProfileFailure.HTTP_ERROR -> AccountMessage.HTTP_ERROR
                    }
                }
            }
            if (ticket != generation || message == null) return@launch
            mutableState.value = state.value.copy(busy = false, apiKey = "", message = message,
                configured = message == AccountMessage.VERIFIED || state.value.configured,
                hasSavedKey = savedHasKey,
                removeSavedKey = if (message == AccountMessage.VERIFIED) false else state.value.removeSavedKey)
        }
    }

    fun reset() {
        if (state.value.loading) return
        invalidate()
        mutableState.value = AccountState()
        viewModelScope.launch {
            try {
                // Finish deletion even if the activity's ViewModel is cleared while resetting.
                withContext(io + NonCancellable) { writes.withLock { storage.clear() } }
                mutableState.value = AccountState(loading = false)
            } catch (_: AccountStorageException) {
                mutableState.value = AccountState(loading = false, message = AccountMessage.STORAGE_FAILURE)
            }
        }
    }

    fun removeSavedKey() {
        if (state.value.loading) return
        invalidate()
        mutableState.value = state.value.copy(apiKey = "", busy = false, removeSavedKey = true, message = null)
    }

    private fun invalidate() {
        generation++
        verification?.cancel()
    }
}
