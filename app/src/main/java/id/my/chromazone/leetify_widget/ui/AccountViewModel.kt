package id.my.chromazone.leetify_widget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.chromazone.leetify_widget.data.AccountCredentials
import id.my.chromazone.leetify_widget.data.AccountStorage
import id.my.chromazone.leetify_widget.data.AccountStorageException
import id.my.chromazone.leetify_widget.data.LeetifyApi
import id.my.chromazone.leetify_widget.data.ProfileApiException
import id.my.chromazone.leetify_widget.data.VerifiedAccountProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class AccountViewModel(
    private val api: LeetifyApi,
    private val storage: AccountStorage,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val onAccountChanged: suspend (VerifiedAccountProfile?) -> Unit = {},
) : ViewModel() {
    private val mutableState = MutableStateFlow(AccountState())
    val state = mutableState.asStateFlow()
    private var verification: Job? = null
    @Volatile
    private var generation = 0L
    private val writes = Mutex()

    init {
        viewModelScope.launch {
            try {
                val saved = withContext(io) { storage.load() }
                mutableState.value = AccountState(
                    steamId = saved?.steamId.orEmpty(),
                    loading = false,
                    configured = saved != null,
                    hasSavedKey = saved?.apiKey != null,
                )
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
        val input = state.value
        if (input.loading || input.busy) return

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
            try {
                withContext(io) {
                    verifyAndSave(input, steamId, key, ticket)
                }
            } catch (_: AccountStorageException) {
                showFailure(ticket, AccountMessage.STORAGE_FAILURE)
            } catch (error: ProfileApiException) {
                showFailure(ticket, error.failure.toAccountMessage())
            }
        }
    }

    private suspend fun verifyAndSave(
        input: AccountState,
        steamId: String,
        key: String,
        ticket: Long,
    ) {
        val saved = storage.load()
        val effectiveKey = key.ifEmpty { null } ?: saved?.apiKey?.takeIf {
            !input.removeSavedKey && saved.steamId == steamId
        }
        if (effectiveKey != null) api.validateApiKey(effectiveKey)
        currentCoroutineContext().ensureActive()
        val profile = api.fetchProfile(steamId, effectiveKey)
        val fetchedAt = System.currentTimeMillis()
        currentCoroutineContext().ensureActive()

        writes.withLock {
            if (ticket != generation) return@withLock
            val revision = storage.save(AccountCredentials(steamId, effectiveKey))
            withContext(NonCancellable) {
                withContext(Dispatchers.Main.immediate) {
                    if (ticket == generation) {
                        mutableState.value = state.value.copy(
                            busy = false,
                            apiKey = "",
                            message = AccountMessage.VERIFIED,
                            configured = true,
                            hasSavedKey = effectiveKey != null,
                            removeSavedKey = false,
                        )
                    }
                }
                onAccountChanged(VerifiedAccountProfile(revision, profile, fetchedAt))
            }
        }
    }

    private fun showFailure(ticket: Long, message: AccountMessage) {
        if (ticket == generation) {
            mutableState.value = state.value.copy(busy = false, apiKey = "", message = message)
        }
    }

    fun reset() {
        if (state.value.loading) return
        invalidate()
        mutableState.value = AccountState()
        viewModelScope.launch {
            try {
                // Finish deletion even if the activity's ViewModel is cleared while resetting.
                withContext(io + NonCancellable) {
                    writes.withLock {
                        storage.clear()
                        onAccountChanged(null)
                    }
                }
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
