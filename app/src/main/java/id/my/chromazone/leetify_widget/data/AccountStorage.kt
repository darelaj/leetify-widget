package id.my.chromazone.leetify_widget.data

internal interface AccountStorage {
    suspend fun load(): AccountCredentials?
    suspend fun save(account: AccountCredentials): String
    suspend fun clear()
}
