package id.my.chromazone.leetify_widget.widget

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import id.my.chromazone.leetify_widget.MainActivity
import id.my.chromazone.leetify_widget.R
import id.my.chromazone.leetify_widget.data.MatchOutcome
import id.my.chromazone.leetify_widget.ui.textResource
import id.my.chromazone.leetify_widget.ui.toAccountMessage
import java.text.DateFormat
import java.util.Date

@Composable
internal fun StatsWidgetContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val expanded = LocalSize.current.width >= 280.dp
    val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    Column(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground).cornerRadius(20.dp).padding(10.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text(
                context.getString(R.string.widget_title),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            Image(
                provider = ImageProvider(if (dark) R.drawable.leetify_badge_white else R.drawable.leetify_badge_black),
                contentDescription = context.getString(R.string.widget_attribution),
                modifier = GlanceModifier.size(96.dp, 40.dp)
                    .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://leetify.com/")))),
            )
        }
        val profile = snapshot.profile
        if (snapshot.status == WidgetStatus.READY && profile != null) {
            Row(GlanceModifier.fillMaxWidth()) {
                Column(GlanceModifier.defaultWeight()) { Metric(R.string.metric_aim, profile.aim, 1, expanded) }
                Column(GlanceModifier.defaultWeight()) { Metric(R.string.metric_utility, profile.utility, 1, expanded) }
            }
            Row(GlanceModifier.fillMaxWidth()) {
                Column(GlanceModifier.defaultWeight()) { Metric(R.string.metric_leetify, profile.leetifyRating, 2, expanded) }
                Column(GlanceModifier.defaultWeight()) { Metric(R.string.metric_premier, profile.premierRating, 0, expanded) }
            }
            Row {
                Text(
                    context.getString(if (profile.recentMatches.isEmpty()) R.string.widget_no_matches else R.string.widget_recent),
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 10.sp),
                )
                for (match in profile.recentMatches) {
                    val color = when (match.outcome) {
                        MatchOutcome.WIN -> if (dark) Color(0xFF8CE99A) else Color(0xFF17652C)
                        MatchOutcome.LOSS -> if (dark) Color(0xFFFFA8A8) else Color(0xFFB02020)
                        else -> null
                    }
                    Text(match.outcome.widgetLabel(), modifier = GlanceModifier.padding(start = 4.dp),
                        style = TextStyle(color = color?.let { ColorProvider(it) } ?: GlanceTheme.colors.onSurface,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }
            }
            snapshot.fetchedAtMillis?.let {
                val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
                Text(context.getString(R.string.widget_updated, date),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 9.sp))
            }
        } else {
            val message = when (snapshot.status) {
                WidgetStatus.SETUP -> R.string.widget_setup
                WidgetStatus.LOADING -> R.string.widget_loading
                WidgetStatus.STORAGE_ERROR -> R.string.storage_failure
                else -> snapshot.failure?.toAccountMessage()?.textResource() ?: R.string.http_failure
            }
            Text(context.getString(message), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp))
        }
        Spacer(GlanceModifier.height(4.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Text(context.getString(R.string.widget_refresh),
                modifier = GlanceModifier.clickable(actionSendBroadcast(
                    Intent(context, StatsWidgetReceiver::class.java).setAction(WidgetUpdates.REFRESH_ACTION))),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            val destination = if (profile != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://leetify.com/app/profile/${profile.steamId}"))
            } else Intent(context, MainActivity::class.java)
            Text(context.getString(if (profile != null) R.string.widget_source else R.string.widget_settings),
                modifier = GlanceModifier.clickable(actionStartActivity(destination)),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun Metric(@StringRes label: Int, value: Double?, decimals: Int, expanded: Boolean) {
    Text(LocalContext.current.getString(label),
        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = if (expanded) 12.sp else 10.sp))
    Text(formatWidgetRating(value, decimals),
        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = if (expanded) 20.sp else 15.sp,
            fontWeight = FontWeight.Bold))
}
