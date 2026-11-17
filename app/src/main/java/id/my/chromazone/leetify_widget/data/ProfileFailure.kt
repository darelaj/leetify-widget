package id.my.chromazone.leetify_widget.data

internal enum class ProfileFailure {
    UNAUTHORIZED, ACCESS_DENIED, PROFILE_UNAVAILABLE, RATE_LIMITED,
    SERVER, HTTP_ERROR, NETWORK, TIMEOUT, INVALID_RESPONSE,
}
