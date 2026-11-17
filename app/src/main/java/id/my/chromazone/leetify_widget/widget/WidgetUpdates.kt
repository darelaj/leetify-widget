package id.my.chromazone.leetify_widget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.compose
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

internal object WidgetUpdates {
    const val WORK_NAME = "stats-widget-refresh"
    const val REFRESH_ACTION = "id.my.chromazone.leetify_widget.REFRESH_STATS"

    fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, StatsWidgetReceiver::class.java))

    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
        )
    }

    suspend fun renderAll(context: Context, snapshot: WidgetSnapshot) {
        val manager = AppWidgetManager.getInstance(context)
        val glanceManager = GlanceAppWidgetManager(context)
        for (id in ids(context)) {
            val views = StatsWidget(snapshot).compose(context, glanceManager.getGlanceIdBy(id))
            manager.updateAppWidget(id, views)
        }
    }
}
