package id.my.chromazone.leetify_widget.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import id.my.chromazone.leetify_widget.MainActivity
import id.my.chromazone.leetify_widget.R

class StatsWidget : GlanceAppWidget() {
    override val stateDefinition = null
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(180.dp, 140.dp), DpSize(280.dp, 220.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { StatsWidgetContent() } }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { GlanceTheme { StatsWidgetContent() } }
    }
}

@Composable
private fun StatsWidgetContent() {
    val context = LocalContext.current
    val expanded = LocalSize.current.width >= 280.dp
    Column(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground).cornerRadius(20.dp).padding(16.dp),
    ) {
        Text(context.getString(R.string.widget_title),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(8.dp))
        Text(context.getString(R.string.widget_setup),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
        if (expanded) {
            Spacer(GlanceModifier.height(8.dp))
            Text(context.getString(R.string.widget_preview_metrics),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(context.getString(R.string.widget_settings),
            modifier = GlanceModifier.clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
    }
}
