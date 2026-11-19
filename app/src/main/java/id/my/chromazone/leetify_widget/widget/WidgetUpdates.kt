package id.my.chromazone.leetify_widget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.compose
import id.my.chromazone.leetify_widget.data.AccountMutationLock
import id.my.chromazone.leetify_widget.data.AccountStorageException
import id.my.chromazone.leetify_widget.data.PreferencesAccountStorage
import id.my.chromazone.leetify_widget.data.accountDataStore
import kotlinx.coroutines.sync.withLock

internal object WidgetUpdates {
    const val WORK_NAME = "stats-widget-refresh"
    const val REFRESH_ACTION = "id.my.chromazone.leetify_widget.REFRESH_STATS"

    fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, StatsWidgetReceiver::class.java))

    fun enqueue(context: Context) {
        WidgetRefreshSchedule.enqueue(context)
    }

    suspend fun accountChanged(context: Context) {
        if (ids(context).isEmpty()) return
        AccountMutationLock.mutex.withLock {
            val snapshot = try {
                if (PreferencesAccountStorage(context.accountDataStore).load() == null) WidgetSnapshot()
                else WidgetSnapshot(status = WidgetStatus.LOADING)
            } catch (_: AccountStorageException) {
                WidgetSnapshot(status = WidgetStatus.STORAGE_ERROR)
            }
            renderAll(context, snapshot)
        }
        WidgetRefreshSchedule.enqueue(context, replace = true)
    }

    suspend fun renderAll(context: Context, snapshot: WidgetSnapshot) {
        val manager = AppWidgetManager.getInstance(context)
        for (id in ids(context)) {
            if (manager.getAppWidgetInfo(id) == null) continue
            try {
                // Fresh composition IDs avoid collisions with a cancelled refresh's Glance cleanup.
                val views = StatsWidget(snapshot).compose(context, options = manager.getAppWidgetOptions(id))
                manager.updateAppWidget(id, views)
            } catch (error: IllegalArgumentException) {
                if (manager.getAppWidgetInfo(id) != null) throw error
            }
        }
    }
}
