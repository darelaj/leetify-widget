package id.my.chromazone.leetify_widget.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.chromazone.leetify_widget.data.AccountStorageException
import id.my.chromazone.leetify_widget.data.LeetifyApi
import id.my.chromazone.leetify_widget.data.PreferencesAccountStorage
import id.my.chromazone.leetify_widget.data.ProfileApiException
import id.my.chromazone.leetify_widget.data.accountDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (WidgetUpdates.ids(applicationContext).isEmpty()) return@withContext Result.success()
        val snapshot = try {
            val account = PreferencesAccountStorage(applicationContext.accountDataStore).load()
            if (account == null) {
                WidgetSnapshot()
            } else {
                WidgetUpdates.renderAll(applicationContext, WidgetSnapshot(status = WidgetStatus.LOADING))
                val profile = LeetifyApi().fetchProfile(account.steamId, account.apiKey)
                WidgetSnapshot(WidgetStatus.READY, profile, System.currentTimeMillis())
            }
        } catch (error: ProfileApiException) {
            WidgetSnapshot(status = WidgetStatus.ERROR, failure = error.failure)
        } catch (_: AccountStorageException) {
            WidgetSnapshot(status = WidgetStatus.STORAGE_ERROR)
        }
        WidgetUpdates.renderAll(applicationContext, snapshot)
        Result.success()
    }
}
