package id.my.chromazone.leetify_widget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.my.chromazone.leetify_widget.data.LeetifyApi
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
) {
    override fun toString() = "AccountState(busy=$busy, message=$message)"
}

internal enum class AccountMessage {
    VERIFIED, INVALID_ID, INVALID_KEY_FORMAT, UNAUTHORIZED, ACCESS_DENIED,
    UNAVAILABLE, RATE_LIMITED, SERVER, NETWORK, TIMEOUT, INVALID_RESPONSE, HTTP_ERROR,
}

internal fun validateAccountInput(steamId: String, apiKey: String): AccountMessage? = when {
    !steamId.matches(Regex("[0-9]{17}")) -> AccountMessage.INVALID_ID
    apiKey.any { it !in '!'..'~' } -> AccountMessage.INVALID_KEY_FORMAT
    else -> null
}

internal class AccountViewModel(private val api: LeetifyApi) : ViewModel() {
    private val mutableState = MutableStateFlow(AccountState())
    val state = mutableState.asStateFlow()
    private var verification: Job? = null

    fun editSteamId(value: String) {
        verification?.cancel()
        mutableState.value = state.value.copy(steamId = value, busy = false, message = null)
    }

    fun editApiKey(value: String) {
        verification?.cancel()
        mutableState.value = state.value.copy(apiKey = value, busy = false, message = null)
    }

    fun verify() {
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
            val message = withContext(Dispatchers.IO) {
                try {
                    if (key.isNotEmpty()) api.validateApiKey(key)
                    api.fetchProfile(steamId, key.ifEmpty { null })
                    AccountMessage.VERIFIED
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
            mutableState.value = state.value.copy(busy = false, apiKey = "", message = message)
        }
    }

    fun reset() {
        verification?.cancel()
        mutableState.value = AccountState()
    }
}
