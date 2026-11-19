package id.my.chromazone.leetify_widget

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.net.ConnectivityManager
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import androidx.work.NetworkType
import androidx.work.WorkInfo
import id.my.chromazone.leetify_widget.data.AccountCredentials
import id.my.chromazone.leetify_widget.data.PreferencesAccountStorage
import id.my.chromazone.leetify_widget.data.accountDataStore
import id.my.chromazone.leetify_widget.data.PlayerProfile
import id.my.chromazone.leetify_widget.data.AccountMutationLock
import id.my.chromazone.leetify_widget.widget.StatsWidgetReceiver
import id.my.chromazone.leetify_widget.widget.WidgetSnapshot
import id.my.chromazone.leetify_widget.widget.WidgetStatus
import id.my.chromazone.leetify_widget.widget.WidgetUpdates
import id.my.chromazone.leetify_widget.widget.WidgetRefreshSchedule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
class StatsWidgetHostTest {
    @Test fun bindsTwoWidgetsResizesAndClearsBothWithoutStoringStats() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        val context = base.createConfigurationContext(Configuration(base.resources.configuration).apply { fontScale = 1.3f })
        val manager = AppWidgetManager.getInstance(context)
        val host = AppWidgetHost(context, 4242)
        val provider = ComponentName(context, StatsWidgetReceiver::class.java)
        val ids = listOf(host.allocateAppWidgetId(), host.allocateAppWidgetId())
        val views = mutableListOf<android.appwidget.AppWidgetHostView>()
        instrumentation.uiAutomation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)
        try {
            for (id in ids) assertTrue(manager.bindAppWidgetIdIfAllowed(id, provider))
            assertTrue(WidgetUpdates.ids(context).toList().containsAll(ids))
            WidgetRefreshSchedule.ensureHourly(context)
            WidgetRefreshSchedule.ensureHourly(context)
            val hourly = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WidgetRefreshSchedule.PERIODIC_WORK_NAME).get()
                .filterNot { it.state.isFinished }
            assertEquals(1, hourly.size)
            assertEquals(3_600_000L, hourly.single().periodicityInfo!!.repeatIntervalMillis)
            assertEquals(NetworkType.CONNECTED, hourly.single().constraints.requiredNetworkType)
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 220)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 220)
            }
            for ((index, id) in ids.withIndex()) {
                val instanceOptions = Bundle(options)
                if (index == 1) {
                    instanceOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280)
                    instanceOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 280)
                    instanceOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 260)
                    instanceOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 260)
                }
                manager.updateAppWidgetOptions(id, instanceOptions)
            }
            instrumentation.runOnMainSync {
                for (id in ids) views += host.createView(context, id, manager.getAppWidgetInfo(id))
            }
            host.startListening()
            awaitText(views, "Set up your account")
            // Let the real setup update finish before composing a synthetic snapshot.
            awaitCondition("Initial setup work did not finish") {
                WorkManager.getInstance(context).getWorkInfosForUniqueWork(WidgetUpdates.WORK_NAME)
                    .get().none { !it.state.isFinished }
            }
            val snapshot = WidgetSnapshot(WidgetStatus.READY,
                PlayerProfile("76561190000000000", "Synthetic", 61.3, 70.0, -2.12, 19309.0, emptyList()), 0L)
            AccountMutationLock.mutex.withLock {
                WidgetUpdates.renderAll(context, snapshot)
                awaitText(views, "61.3")
                instrumentation.runOnMainSync {
                    for (view in views) assertTrue(textOf(view).contains("19,309"))
                    val view = views.first()
                    val density = context.resources.displayMetrics.density
                    val width = (200 * density).toInt()
                    val height = (220 * density).toInt()
                    view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                    view.layout(0, 0, width, height)
                    for (action in textViews(view).filter { it.text.toString() in listOf("Refresh", "View on Leetify") }) {
                        val bounds = Rect()
                        action.getDrawingRect(bounds)
                        view.offsetDescendantRectToMyCoords(action, bounds)
                        assertTrue("Action clipped: ${action.text}", bounds.bottom <= height - view.paddingBottom)
                    }
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, "leetify-widget-host.png")
                        put(MediaStore.Downloads.MIME_TYPE, "image/png")
                    }
                    val uri = checkNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
                    context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }
            WidgetUpdates.accountChanged(context)
            awaitText(views, "Set up your account")
            instrumentation.runOnMainSync {
                for (view in views) assertFalse(textOf(view).contains("61.3"))
            }
            awaitCondition("Reset work did not finish") {
                WorkManager.getInstance(context).getWorkInfosForUniqueWork(WidgetUpdates.WORK_NAME)
                    .get().none { !it.state.isFinished }
            }
            val wifiWasOn = shell("settings get global wifi_on").trim() == "1"
            val dataWasOn = shell("settings get global mobile_data").trim() == "1"
            val storage = PreferencesAccountStorage(context.accountDataStore)
            try {
                shell("svc wifi disable")
                shell("svc data disable")
                val connectivity = context.getSystemService(ConnectivityManager::class.java)
                awaitCondition("Emulator did not disconnect") { connectivity.activeNetwork == null }
                storage.save(AccountCredentials("76561190000000000", null))
                WidgetUpdates.accountChanged(context)
                WidgetUpdates.manualRefresh(context)
                awaitText(views, "Refresh queued")
                val workManager = WorkManager.getInstance(context)
                val pending = workManager.getWorkInfosForUniqueWork(WidgetUpdates.WORK_NAME).get()
                    .single { !it.state.isFinished }
                repeat(3) {
                    context.sendBroadcast(Intent(context, StatsWidgetReceiver::class.java).setAction(WidgetUpdates.REFRESH_ACTION))
                    WidgetUpdates.enqueue(context)
                }
                kotlinx.coroutines.delay(1_000)
                val active = workManager.getWorkInfosForUniqueWork(WidgetUpdates.WORK_NAME).get()
                    .filterNot { it.state.isFinished }
                assertEquals(listOf(pending.id), active.map { it.id })
                assertEquals(WorkInfo.State.ENQUEUED, active.single().state)
                assertEquals(0, active.single().runAttemptCount)
                assertEquals(NetworkType.CONNECTED, active.single().constraints.requiredNetworkType)
                instrumentation.runOnMainSync {
                    for (view in views) assertFalse(textOf(view).contains("61.3"))
                }
            } finally {
                storage.clear()
                WidgetUpdates.accountChanged(context)
                WorkManager.getInstance(context).cancelUniqueWork(WidgetUpdates.WORK_NAME).result.get()
                if (wifiWasOn) shell("svc wifi enable")
                if (dataWasOn) shell("svc data enable")
            }
        } finally {
            WorkManager.getInstance(context).cancelUniqueWork(WidgetUpdates.WORK_NAME).result.get()
            for (id in ids) host.deleteAppWidgetId(id)
            host.stopListening()
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
        assertFalse(WidgetUpdates.ids(context).any { it in ids })
        repeat(100) {
            val active = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WidgetRefreshSchedule.PERIODIC_WORK_NAME).get()
                .any { !it.state.isFinished }
            if (!active) return@runBlocking
            kotlinx.coroutines.delay(50)
        }
        fail("Removing the last widget did not cancel hourly work")
    }

    private suspend fun awaitText(views: List<View>, expected: String) {
        repeat(100) {
            val ready = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                views.all { textOf(it).contains(expected) }
            }
            if (ready) return
            kotlinx.coroutines.delay(50)
        }
        fail("Widget host did not receive expected content: $expected")
    }

    private suspend fun awaitCondition(message: String, ready: () -> Boolean) {
        repeat(200) {
            if (ready()) return
            kotlinx.coroutines.delay(50)
        }
        fail(message)
    }

    private fun shell(command: String): String {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        return ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
            .bufferedReader().use { it.readText() }
    }

    private fun textOf(view: View): String = when (view) {
        is TextView -> view.text.toString()
        is ViewGroup -> (0 until view.childCount).joinToString(" ") { textOf(view.getChildAt(it)) }
        else -> ""
    }

    private fun textViews(view: View): List<TextView> = when (view) {
        is TextView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { textViews(view.getChildAt(it)) }
        else -> emptyList()
    }
}
