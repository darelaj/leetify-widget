package id.my.chromazone.leetify_widget.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

class StatsWidget internal constructor(
    private val snapshot: WidgetSnapshot = WidgetSnapshot(),
    private val shortOnly: Boolean = false,
) : GlanceAppWidget() {
    override val stateDefinition = null
    override val sizeMode = SizeMode.Responsive(
        if (shortOnly) setOf(shortSize) else setOf(shortSize, DpSize(200.dp, 236.dp), DpSize(280.dp, 260.dp)),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { StatsWidgetContent(snapshot) } }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { GlanceTheme { StatsWidgetContent(WidgetSnapshot()) } }
    }
}

private val shortSize = DpSize(200.dp, 180.dp)
