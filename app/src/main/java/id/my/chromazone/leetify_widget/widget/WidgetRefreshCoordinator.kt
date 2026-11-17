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
    suspend fun refresh() {
        val account = try {
            storage.load()
        } catch (_: AccountStorageException) {
            AccountMutationLock.mutex.withLock { publish(WidgetSnapshot(status = WidgetStatus.STORAGE_ERROR)) }
            return
        }
        if (account == null) {
            AccountMutationLock.mutex.withLock {
                if (storage.load() == null) publish(WidgetSnapshot())
            }
            return
        }
        if (!publishIfCurrent(account, WidgetSnapshot(status = WidgetStatus.LOADING))) return
        val snapshot = try {
            WidgetSnapshot(WidgetStatus.READY, fetch(account), nowMillis())
        } catch (error: ProfileApiException) {
            WidgetSnapshot(status = WidgetStatus.ERROR, failure = error.failure)
        }
        currentCoroutineContext().ensureActive()
        publishIfCurrent(account, snapshot)
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
