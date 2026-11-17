package id.my.chromazone.leetify_widget.ui

import androidx.annotation.StringRes
import id.my.chromazone.leetify_widget.R
import id.my.chromazone.leetify_widget.data.ProfileFailure

internal fun ProfileFailure.toAccountMessage(): AccountMessage = when (this) {
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

@StringRes
internal fun AccountMessage.textResource(): Int = when (this) {
    AccountMessage.VERIFIED -> R.string.account_verified
    AccountMessage.STORAGE_FAILURE -> R.string.storage_failure
    AccountMessage.INVALID_ID -> R.string.invalid_steam_id
    AccountMessage.INVALID_KEY_FORMAT -> R.string.invalid_key_format
    AccountMessage.UNAUTHORIZED -> R.string.unauthorized
    AccountMessage.ACCESS_DENIED -> R.string.access_denied
    AccountMessage.UNAVAILABLE -> R.string.profile_unavailable
    AccountMessage.RATE_LIMITED -> R.string.rate_limited
    AccountMessage.SERVER -> R.string.server_failure
    AccountMessage.NETWORK -> R.string.network_failure
    AccountMessage.TIMEOUT -> R.string.request_timeout
    AccountMessage.INVALID_RESPONSE -> R.string.invalid_response
    AccountMessage.HTTP_ERROR -> R.string.http_failure
}
