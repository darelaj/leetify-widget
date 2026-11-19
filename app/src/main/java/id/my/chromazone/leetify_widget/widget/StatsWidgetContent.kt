package id.my.chromazone.leetify_widget.widget

import android.content.Intent
import android.content.res.Configuration
import androidx.core.net.toUri
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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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
    val compactLargeText = !expanded && context.resources.configuration.fontScale >= 1.2f
    val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    Column(
        modifier = GlanceModifier.fillMaxSize().appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground).cornerRadius(20.dp)
            .padding(if (compactLargeText) 8.dp else 10.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                context.getString(R.string.widget_title),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = when {
                    expanded -> 19.sp
                    compactLargeText -> 13.sp
                    else -> 14.sp
                },
                    fontWeight = FontWeight.Bold),
            )
            Image(
                provider = ImageProvider(if (dark) R.drawable.leetify_badge_white else R.drawable.leetify_badge_black),
                contentDescription = context.getString(R.string.widget_attribution),
                modifier = (if (compactLargeText) GlanceModifier.size(84.dp, 35.dp) else GlanceModifier.size(96.dp, 40.dp))
                    .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, "https://leetify.com/".toUri()))),
            )
        }
        val profile = snapshot.profile
        if (snapshot.status == WidgetStatus.READY && profile != null) {
            Spacer(GlanceModifier.height(if (compactLargeText) 2.dp else 4.dp))
            if (compactLargeText) {
                Column(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                    CompactMetric(R.string.metric_aim, profile.aim, 1)
                    CompactMetric(R.string.metric_utility, profile.utility, 1)
                    CompactMetric(R.string.metric_leetify, profile.leetifyRating, 2)
                    CompactMetric(R.string.metric_premier, profile.premierRating, 0)
                }
            } else {
                Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    Metric(R.string.metric_aim, profile.aim, 1, expanded, GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(6.dp))
                    Metric(R.string.metric_utility, profile.utility, 1, expanded, GlanceModifier.defaultWeight())
                }
                Spacer(GlanceModifier.height(6.dp))
                Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    Metric(R.string.metric_leetify, profile.leetifyRating, 2, expanded, GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(6.dp))
                    Metric(R.string.metric_premier, profile.premierRating, 0, expanded, GlanceModifier.defaultWeight())
                }
            }
            Spacer(GlanceModifier.height(if (compactLargeText) 2.dp else 6.dp))
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    context.getString(if (profile.recentMatches.isEmpty()) R.string.widget_no_matches else R.string.widget_recent),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = if (expanded) 12.sp else 10.sp),
                )
                for (match in profile.recentMatches) {
                    val color = when (match.outcome) {
                        MatchOutcome.WIN -> if (dark) Color(0xFF8CE99A) else Color(0xFF17652C)
                        MatchOutcome.LOSS -> if (dark) Color(0xFFFFA8A8) else Color(0xFFB02020)
                        else -> null
                    }
                    Box(GlanceModifier.padding(start = 2.dp)) {
                        Text(match.outcome.widgetLabel(), modifier = GlanceModifier
                            .background(GlanceTheme.colors.surfaceVariant).cornerRadius(6.dp)
                            .padding(horizontal = 3.dp, vertical = 2.dp),
                            style = TextStyle(color = color?.let { ColorProvider(it) } ?: GlanceTheme.colors.onSurface,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
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
                WidgetStatus.WAITING -> R.string.widget_waiting
                WidgetStatus.STORAGE_ERROR -> R.string.storage_failure
                else -> snapshot.failure?.toAccountMessage()?.textResource() ?: R.string.http_failure
            }
            Column(GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                Text(context.getString(message), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
            }
        }
        Spacer(GlanceModifier.height(if (compactLargeText) 2.dp else 4.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(context.getString(R.string.widget_refresh),
                modifier = GlanceModifier.background(GlanceTheme.colors.primaryContainer).cornerRadius(8.dp)
                    .padding(horizontal = 8.dp, vertical = if (compactLargeText) 2.dp else 4.dp).clickable(actionSendBroadcast(
                    Intent(context, StatsWidgetReceiver::class.java).setAction(WidgetUpdates.REFRESH_ACTION))),
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            val destination = if (profile != null) {
                Intent(Intent.ACTION_VIEW, "https://leetify.com/app/profile/${profile.steamId}".toUri())
            } else Intent(context, MainActivity::class.java)
            Text(context.getString(if (profile != null) R.string.widget_source else R.string.widget_settings),
                modifier = GlanceModifier.clickable(actionStartActivity(destination)),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun CompactMetric(@StringRes label: Int, value: Double?, decimals: Int) {
    val band = if (label == R.string.metric_premier) premierRatingBand(value) else null
    if (band != null) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(LocalContext.current.getString(label), modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 11.sp))
            PremierRatingBadge(formatWidgetRating(value, decimals), band, 11)
        }
        return
    }
    Text(
        text = "${LocalContext.current.getString(label)}: ${formatWidgetRating(value, decimals)}",
        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 11.sp),
    )
}

@Composable
private fun Metric(@StringRes label: Int, value: Double?, decimals: Int, expanded: Boolean, modifier: GlanceModifier) {
    Column(
        modifier.fillMaxHeight().background(GlanceTheme.colors.surfaceVariant).cornerRadius(10.dp)
            .padding(horizontal = if (expanded) 10.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(LocalContext.current.getString(label), maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = if (expanded) 12.sp else 10.sp))
        val fontSize = if (expanded) 23 else 16
        val band = if (label == R.string.metric_premier) premierRatingBand(value) else null
        if (band != null) {
            val badgeSize = if (expanded && LocalSize.current.width < 320.dp) 19 else fontSize
            PremierRatingBadge(formatWidgetRating(value, decimals), band, badgeSize)
        } else {
            Text(formatWidgetRating(value, decimals), maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold))
        }
    }
}
