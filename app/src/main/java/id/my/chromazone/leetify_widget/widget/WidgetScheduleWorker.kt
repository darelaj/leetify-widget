package id.my.chromazone.leetify_widget.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetScheduleWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        if (WidgetUpdates.ids(applicationContext).isEmpty()) {
            WidgetRefreshSchedule.cancel(applicationContext)
        } else {
            WidgetRefreshSchedule.enqueue(applicationContext)
        }
        return Result.success()
    }
}
