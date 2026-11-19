package id.my.chromazone.leetify_widget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.compose
import androidx.work.WorkManager
import androidx.work.await
import id.my.chromazone.leetify_widget.data.AccountMutationLock
import id.my.chromazone.leetify_widget.data.AccountStorageException
import id.my.chromazone.leetify_widget.data.PreferencesAccountStorage
import id.my.chromazone.leetify_widget.data.VerifiedAccountProfile
import id.my.chromazone.leetify_widget.data.accountDataStore
import kotlinx.coroutines.sync.withLock
import java.io.IOException

internal object WidgetUpdates {
    const val WORK_NAME = "stats-widget-refresh"
    const val REFRESH_ACTION = "id.my.chromazone.leetify_widget.REFRESH_STATS"

    fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, StatsWidgetReceiver::class.java))

    fun enqueue(context: Context) {
        WidgetRefreshSchedule.enqueue(context)
    }

    suspend fun accountChanged(context: Context, verified: VerifiedAccountProfile? = null) {
        val needsRefresh = AccountMutationLock.mutex.withLock {
            val snapshot = try {
                val current = PreferencesAccountStorage(context.accountDataStore).load()
                if (verified != null && current?.revision != verified.accountRevision) return@withLock false
                RefreshMetadataStore(context.refreshDataStore).resetAccount()
                when {
                    current == null -> WidgetSnapshot()
                    verified != null -> WidgetSnapshot(WidgetStatus.READY, verified.profile, verified.fetchedAtMillis)
                    else -> WidgetSnapshot(status = WidgetStatus.WAITING)
                }
            } catch (_: AccountStorageException) {
                WidgetSnapshot(status = WidgetStatus.STORAGE_ERROR)
            } catch (_: IOException) {
                WidgetSnapshot(status = WidgetStatus.STORAGE_ERROR)
            }
            if (verified != null) WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME).await()
            renderAll(context, snapshot)
            verified == null
        }
        if (needsRefresh) WidgetRefreshSchedule.enqueue(context, replace = true)
        else WidgetRefreshSchedule.ensureHourly(context)
    }

    suspend fun manualRefresh(context: Context) {
        if (ids(context).isEmpty()) return
        WidgetRefreshSchedule.ensureHourly(context)
        var accepted = false
        try {
            AccountMutationLock.mutex.withLock {
                accepted = RefreshMetadataStore(context.refreshDataStore).claimManual(System.currentTimeMillis())
                if (!accepted) return@withLock
                val snapshot = if (PreferencesAccountStorage(context.accountDataStore).load() == null) WidgetSnapshot()
                    else WidgetSnapshot(status = WidgetStatus.WAITING)
                renderAll(context, snapshot)
            }
        } catch (_: AccountStorageException) {
            AccountMutationLock.mutex.withLock { renderAll(context, WidgetSnapshot(status = WidgetStatus.STORAGE_ERROR)) }
        } catch (_: IOException) {
            AccountMutationLock.mutex.withLock { renderAll(context, WidgetSnapshot(status = WidgetStatus.STORAGE_ERROR)) }
        } finally {
            if (accepted) WidgetRefreshSchedule.enqueue(context)
        }
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
