package id.my.chromazone.leetify_widget.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.chromazone.leetify_widget.data.LeetifyApi
import id.my.chromazone.leetify_widget.data.PreferencesAccountStorage
import id.my.chromazone.leetify_widget.data.AccountMutationLock
import id.my.chromazone.leetify_widget.data.AccountStorageException
import id.my.chromazone.leetify_widget.data.accountDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import java.io.IOException

class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (WidgetUpdates.ids(applicationContext).isEmpty()) {
            WidgetRefreshSchedule.cancel(applicationContext)
            return@withContext Result.success()
        }
        val storage = PreferencesAccountStorage(applicationContext.accountDataStore)
        val api = LeetifyApi()
        val coordinator = WidgetRefreshCoordinator(
            storage = storage,
            fetch = { api.fetchProfile(it.steamId, it.apiKey) },
            publish = { WidgetUpdates.renderAll(applicationContext, it) },
        )
        try {
            val account = storage.load()
            if (account == null) {
                coordinator.refresh()
                return@withContext Result.success()
            }
            val metadata = RefreshMetadataStore(applicationContext.refreshDataStore)
            when (val permit = metadata.startAttempt(id.toString(), account.revision, System.currentTimeMillis())) {
                RefreshPermit.Wait -> Result.retry()
                RefreshPermit.Finished -> Result.success()
                is AllowedRefresh -> {
                    val failure = coordinator.refresh(expectedRevision = account.revision)
                    currentCoroutineContext().ensureActive()
                    if (metadata.finishAttempt(id.toString(), account.revision, permit.attempt, failure, System.currentTimeMillis())) {
                        Result.retry()
                    } else Result.success()
                }
            }
        } catch (_: AccountStorageException) {
            storageFailure()
        } catch (_: IOException) {
            storageFailure()
        }
    }

    private suspend fun storageFailure(): Result {
        AccountMutationLock.mutex.withLock {
            WidgetUpdates.renderAll(applicationContext, WidgetSnapshot(status = WidgetStatus.STORAGE_ERROR))
        }
        return Result.failure()
    }
}
