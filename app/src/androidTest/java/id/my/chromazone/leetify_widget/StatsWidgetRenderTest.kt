package id.my.chromazone.leetify_widget

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.compose
import androidx.test.platform.app.InstrumentationRegistry
import id.my.chromazone.leetify_widget.data.MatchOutcome
import id.my.chromazone.leetify_widget.data.PlayerProfile
import id.my.chromazone.leetify_widget.data.RecentMatch
import id.my.chromazone.leetify_widget.widget.StatsWidget
import id.my.chromazone.leetify_widget.widget.WidgetSnapshot
import id.my.chromazone.leetify_widget.widget.WidgetStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class StatsWidgetRenderTest {
    @Test fun compactAndExpandedSnapshotsRenderSourceStatsInBothThemes() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        for (dark in listOf(false, true)) {
            val base = instrumentation.targetContext
            val configuration = Configuration(base.resources.configuration).apply {
                uiMode = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                fontScale = 1.3f
            }
            val context = base.createConfigurationContext(configuration)
            for (size in listOf(DpSize(200.dp, 220.dp), DpSize(280.dp, 260.dp))) {
                val snapshot = WidgetSnapshot(WidgetStatus.READY,
                    PlayerProfile("76561190000000000", "Synthetic", 61.3, 0.0, -2.12, null,
                        listOf(RecentMatch("synthetic", null, null, MatchOutcome.DRAW))), 0L)
                val views = StatsWidget(snapshot).compose(context, size = size)
                instrumentation.runOnMainSync {
                    val root = views.apply(context, FrameLayout(context))
                    val text = textOf(root)
                    assertTrue(text.contains("Aim"))
                    assertTrue(text.contains("Utility"))
                    assertTrue(text.contains("61.3"))
                    assertTrue(text.contains("-2.12"))
                    assertTrue(text.contains("—"))
                    assertTrue(text.contains("D"))
                    assertTrue(text.contains("View on Leetify"))
                }
            }
        }
    }

    @Test fun setupSnapshotOffersSettingsWithoutStats() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val views = StatsWidget().compose(context, size = DpSize(200.dp, 220.dp))
        instrumentation.runOnMainSync {
            val text = textOf(views.apply(context, FrameLayout(context)))
            assertTrue(text.contains("Set up your account"))
            assertTrue(text.contains("Account settings"))
            assertFalse(text.contains("Fetched"))
        }
    }

    private fun textOf(view: View): String = when (view) {
        is TextView -> view.text.toString()
        is ViewGroup -> (0 until view.childCount).joinToString(" ") { textOf(view.getChildAt(it)) }
        else -> ""
    }
}
