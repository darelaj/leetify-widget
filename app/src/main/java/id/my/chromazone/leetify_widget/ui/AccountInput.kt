package id.my.chromazone.leetify_widget.ui

internal fun validateAccountInput(steamId: String, apiKey: String): AccountMessage? = when {
    !steamId.matches(Regex("[0-9]{17}")) -> AccountMessage.INVALID_ID
    apiKey.any { it !in '!'..'~' } -> AccountMessage.INVALID_KEY_FORMAT
    else -> null
}
