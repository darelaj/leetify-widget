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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class AccountState(
    val steamId: String = "",
    val apiKey: String = "",
    val busy: Boolean = false,
    val message: AccountMessage? = null,
    val loading: Boolean = true,
    val configured: Boolean = false,
    val hasSavedKey: Boolean = false,
    val removeSavedKey: Boolean = false,
) {
    override fun toString() = "AccountState(busy=$busy, message=$message)"
}

internal enum class AccountMessage {
    VERIFIED, STORAGE_FAILURE, INVALID_ID, INVALID_KEY_FORMAT, UNAUTHORIZED, ACCESS_DENIED,
    UNAVAILABLE, RATE_LIMITED, SERVER, NETWORK, TIMEOUT, INVALID_RESPONSE, HTTP_ERROR,
}

internal fun validateAccountInput(steamId: String, apiKey: String): AccountMessage? = when {
    !steamId.matches(Regex("[0-9]{17}")) -> AccountMessage.INVALID_ID
    apiKey.any { it !in '!'..'~' } -> AccountMessage.INVALID_KEY_FORMAT
    else -> null
}

internal class AccountViewModel(private val api: LeetifyApi, private val storage: AccountStorage) : ViewModel() {
    private val mutableState = MutableStateFlow(AccountState())
    val state = mutableState.asStateFlow()
    private var verification: Job? = null

    init {
        viewModelScope.launch {
            try {
                val saved = withContext(Dispatchers.IO) { storage.load() }
                mutableState.value = AccountState(steamId = saved?.steamId.orEmpty(), loading = false,
                    configured = saved != null, hasSavedKey = saved?.apiKey != null)
            } catch (_: AccountStorageException) {
                mutableState.value = AccountState(loading = false, message = AccountMessage.STORAGE_FAILURE)
            }
        }
    }

    fun editSteamId(value: String) {
        if (state.value.loading) return
        verification?.cancel()
        mutableState.value = state.value.copy(steamId = value, busy = false, message = null)
    }

    fun editApiKey(value: String) {
        if (state.value.loading) return
        verification?.cancel()
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
        verification?.cancel()
        mutableState.value = input.copy(busy = true, message = null)
        verification = viewModelScope.launch {
            var savedHasKey = input.hasSavedKey
            val message = withContext(Dispatchers.IO) {
                try {
                    val saved = storage.load()
                    val effectiveKey = key.ifEmpty { null } ?: saved?.apiKey?.takeIf {
                        !input.removeSavedKey && saved.steamId == steamId
                    }
                    if (effectiveKey != null) api.validateApiKey(effectiveKey)
                    api.fetchProfile(steamId, effectiveKey)
                    storage.save(AccountCredentials(steamId, effectiveKey))
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
            mutableState.value = state.value.copy(busy = false, apiKey = "", message = message,
                configured = message == AccountMessage.VERIFIED || state.value.configured,
                hasSavedKey = savedHasKey,
                removeSavedKey = if (message == AccountMessage.VERIFIED) false else state.value.removeSavedKey)
        }
    }

    fun reset() {
        if (state.value.loading) return
        verification?.cancel()
        mutableState.value = AccountState()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { storage.clear() }
                mutableState.value = AccountState(loading = false)
            } catch (_: AccountStorageException) {
                mutableState.value = AccountState(loading = false, message = AccountMessage.STORAGE_FAILURE)
            }
        }
    }

    fun removeSavedKey() {
        if (state.value.loading) return
        verification?.cancel()
        mutableState.value = state.value.copy(apiKey = "", busy = false, removeSavedKey = true, message = null)
    }
}
