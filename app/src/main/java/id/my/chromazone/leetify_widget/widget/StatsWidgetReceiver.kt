package id.my.chromazone.leetify_widget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.work.WorkManager

// Native lifecycle dispatch; Glance composes snapshots without a long-lived widget session.
class StatsWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdates.enqueue(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        WidgetUpdates.enqueue(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WidgetUpdates.REFRESH_ACTION) WidgetUpdates.enqueue(context)
        else super.onReceive(context, intent)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WidgetUpdates.WORK_NAME)
    }
}
