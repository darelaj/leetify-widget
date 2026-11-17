package id.my.chromazone.leetify_widget.ui

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
