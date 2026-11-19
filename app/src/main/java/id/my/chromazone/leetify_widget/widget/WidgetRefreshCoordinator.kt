package id.my.chromazone.leetify_widget.widget

import id.my.chromazone.leetify_widget.data.AccountCredentials
import id.my.chromazone.leetify_widget.data.AccountMutationLock
import id.my.chromazone.leetify_widget.data.AccountStorage
import id.my.chromazone.leetify_widget.data.AccountStorageException
import id.my.chromazone.leetify_widget.data.PlayerProfile
import id.my.chromazone.leetify_widget.data.ProfileApiException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock

internal class WidgetRefreshCoordinator(
    private val storage: AccountStorage,
    private val fetch: suspend (AccountCredentials) -> PlayerProfile,
    private val publish: suspend (WidgetSnapshot) -> Unit,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun refresh(expectedRevision: String? = null): ProfileApiException? {
        val account = try {
            storage.load()
        } catch (_: AccountStorageException) {
            AccountMutationLock.mutex.withLock { publish(WidgetSnapshot(status = WidgetStatus.STORAGE_ERROR)) }
            return null
        }
        if (account == null) {
            AccountMutationLock.mutex.withLock {
                if (storage.load() == null) publish(WidgetSnapshot())
            }
            return null
        }
        if (expectedRevision != null && account.revision != expectedRevision) return null
        if (!publishIfCurrent(account, WidgetSnapshot(status = WidgetStatus.LOADING))) return null
        var failure: ProfileApiException? = null
        val snapshot = try {
            WidgetSnapshot(WidgetStatus.READY, fetch(account), nowMillis())
        } catch (error: ProfileApiException) {
            failure = error
            WidgetSnapshot(status = WidgetStatus.ERROR, failure = error.failure)
        }
        currentCoroutineContext().ensureActive()
        return if (publishIfCurrent(account, snapshot)) failure else null
    }

    private suspend fun publishIfCurrent(account: AccountCredentials, snapshot: WidgetSnapshot): Boolean =
        AccountMutationLock.mutex.withLock {
            val current = try {
                storage.load()
            } catch (_: AccountStorageException) {
                publish(WidgetSnapshot(status = WidgetStatus.STORAGE_ERROR))
                return@withLock false
            }
            if (current == null || current.revision != account.revision ||
                current.steamId != account.steamId || current.apiKey != account.apiKey
            ) return@withLock false
            publish(snapshot)
            true
        }
}
