package id.my.chromazone.leetify_widget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// Native lifecycle dispatch; Glance composes snapshots without a long-lived widget session.
open class StatsWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdates.enqueue(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        WidgetUpdates.enqueue(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetUpdates.REFRESH_ACTION) {
            super.onReceive(context, intent)
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(8_000) { WidgetUpdates.manualRefresh(context.applicationContext) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDisabled(context: Context) {
        if (WidgetUpdates.ids(context).isEmpty()) WidgetRefreshSchedule.cancel(context)
    }
}
