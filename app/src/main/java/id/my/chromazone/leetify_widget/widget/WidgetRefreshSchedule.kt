package id.my.chromazone.leetify_widget.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal object WidgetRefreshSchedule {
    const val PERIODIC_WORK_NAME = "stats-widget-hourly"

    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun ensureHourly(context: Context) {
        if (WidgetUpdates.ids(context).isEmpty()) return
        val request = PeriodicWorkRequestBuilder<WidgetScheduleWorker>(1, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    fun enqueue(context: Context, replace: Boolean = false) {
        if (WidgetUpdates.ids(context).isEmpty()) return
        ensureHourly(context)
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setConstraints(network)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WidgetUpdates.WORK_NAME,
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(PERIODIC_WORK_NAME)
        manager.cancelUniqueWork(WidgetUpdates.WORK_NAME)
    }
}
