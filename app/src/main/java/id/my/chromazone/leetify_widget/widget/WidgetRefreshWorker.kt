package id.my.chromazone.leetify_widget.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import id.my.chromazone.leetify_widget.data.LeetifyApi
import id.my.chromazone.leetify_widget.data.PreferencesAccountStorage
import id.my.chromazone.leetify_widget.data.accountDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (WidgetUpdates.ids(applicationContext).isEmpty()) return@withContext Result.success()
        val api = LeetifyApi()
        WidgetRefreshCoordinator(
            storage = PreferencesAccountStorage(applicationContext.accountDataStore),
            fetch = { api.fetchProfile(it.steamId, it.apiKey) },
            publish = { WidgetUpdates.renderAll(applicationContext, it) },
        ).refresh()
        Result.success()
    }
}
