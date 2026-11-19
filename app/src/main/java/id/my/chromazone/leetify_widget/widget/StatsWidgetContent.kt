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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import id.my.chromazone.leetify_widget.MainActivity
import id.my.chromazone.leetify_widget.R
import id.my.chromazone.leetify_widget.data.MAX_RECENT_MATCHES
import id.my.chromazone.leetify_widget.data.MatchOutcome
import id.my.chromazone.leetify_widget.data.RecentMatch
import id.my.chromazone.leetify_widget.ui.textResource
import id.my.chromazone.leetify_widget.ui.toAccountMessage
import java.text.DateFormat
import java.util.Date

@Composable
internal fun StatsWidgetContent(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    val expanded = LocalSize.current.width >= 280.dp
    val compactLargeText = !expanded && context.resources.configuration.fontScale >= 1.2f
    val tightExpanded = expanded && LocalSize.current.width < 320.dp &&
        context.resources.configuration.fontScale >= 1.2f
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
            Spacer(GlanceModifier.width(6.dp))
            Image(
                provider = ImageProvider(if (dark) R.drawable.leetify_badge_white else R.drawable.leetify_badge_black),
                contentDescription = context.getString(R.string.widget_attribution),
                modifier = (if (compactLargeText) GlanceModifier.size(84.dp, 35.dp) else GlanceModifier.size(96.dp, 40.dp))
                    .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, "https://leetify.com/".toUri()))),
            )
        }
        val profile = snapshot.profile
        if (snapshot.status == WidgetStatus.READY && profile != null) {
            Spacer(GlanceModifier.height(if (compactLargeText || tightExpanded) 2.dp else 6.dp))
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
            Spacer(GlanceModifier.height(if (compactLargeText || tightExpanded) 2.dp else 6.dp))
            RecentResults(profile.recentMatches, expanded, dark)
            snapshot.fetchedAtMillis?.let {
                val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
                Text(context.getString(R.string.widget_updated, date),
                    maxLines = 1,
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
        Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(GlanceTheme.colors.surfaceVariant))
        val actionPadding = if (compactLargeText) 2.dp else 4.dp
        Row(GlanceModifier.fillMaxWidth().padding(top = actionPadding), verticalAlignment = Alignment.CenterVertically) {
            Text(context.getString(R.string.widget_refresh),
                modifier = GlanceModifier.background(GlanceTheme.colors.primaryContainer).cornerRadius(8.dp)
                    .padding(horizontal = 6.dp, vertical = actionPadding).clickable(actionSendBroadcast(
                    Intent(context, StatsWidgetReceiver::class.java).setAction(WidgetUpdates.REFRESH_ACTION))),
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            val destination = if (profile != null) {
                Intent(Intent.ACTION_VIEW, "https://leetify.com/app/profile/${profile.steamId}".toUri())
            } else Intent(context, MainActivity::class.java)
            Text(context.getString(if (profile != null) R.string.widget_source else R.string.widget_settings),
                modifier = GlanceModifier.padding(vertical = actionPadding)
                    .clickable(actionStartActivity(destination)),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun RecentResults(matches: List<RecentMatch>, expanded: Boolean, dark: Boolean) {
    val wide = expanded && LocalSize.current.width >= 320.dp
    val visibleMatches = matches.take(if (expanded) MAX_RECENT_MATCHES else 5)
    val label = LocalContext.current.getString(
        when {
            matches.isEmpty() -> R.string.widget_no_matches
            wide -> R.string.widget_recent
            else -> R.string.widget_recent_short
        },
    )
    val labelStyle = TextStyle(
        color = GlanceTheme.colors.onSurfaceVariant,
        fontSize = if (wide) 12.sp else 10.sp,
        fontWeight = FontWeight.Bold,
    )
    if (wide) {
        Column(GlanceModifier.fillMaxWidth()) {
            Text(label, maxLines = 1, style = labelStyle)
            if (matches.isNotEmpty()) {
                Spacer(GlanceModifier.height(3.dp))
                ResultChips(visibleMatches, wide, dark)
            }
        }
    } else {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = GlanceModifier.defaultWeight(), maxLines = 1, style = labelStyle)
            if (visibleMatches.isNotEmpty()) ResultChips(visibleMatches, wide, dark)
        }
    }
}

@Composable
private fun ResultChips(matches: List<RecentMatch>, wide: Boolean, dark: Boolean) {
    Row {
        for (match in matches) ResultChip(match.outcome, wide, dark)
    }
}

@Composable
private fun ResultChip(outcome: MatchOutcome, wide: Boolean, dark: Boolean) {
    val color = when (outcome) {
        MatchOutcome.WIN -> if (dark) Color(0xFF8CE99A) else Color(0xFF17652C)
        MatchOutcome.LOSS -> if (dark) Color(0xFFFFA8A8) else Color(0xFFB02020)
        else -> null
    }
    Box(GlanceModifier.padding(end = if (wide) 3.dp else 2.dp)) {
        Text(
            outcome.widgetLabel(),
            modifier = GlanceModifier.width(if (wide) 22.dp else 18.dp)
                .background(GlanceTheme.colors.surfaceVariant).cornerRadius(6.dp)
                .padding(vertical = if (wide) 3.dp else 1.dp),
            maxLines = 1,
            style = TextStyle(
                color = color?.let { ColorProvider(it) } ?: GlanceTheme.colors.onSurface,
                fontSize = if (wide) 13.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun CompactMetric(@StringRes label: Int, value: Double?, decimals: Int) {
    val band = if (label == R.string.metric_premier) premierRatingBand(value) else null
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(LocalContext.current.getString(label), modifier = GlanceModifier.defaultWeight(),
            maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
        Spacer(GlanceModifier.width(if (band != null) 10.dp else 6.dp))
        if (band != null) {
            PremierRatingBadge(formatWidgetRating(value, decimals), band, 11)
        } else {
            Text(formatWidgetRating(value, decimals), maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold))
        }
    }
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
            Spacer(GlanceModifier.height(if (expanded) 6.dp else 4.dp))
            val badgeSize = if (expanded && LocalSize.current.width < 320.dp) 19 else fontSize
            PremierRatingBadge(formatWidgetRating(value, decimals), band, badgeSize)
        } else {
            if (expanded) Spacer(GlanceModifier.height(2.dp))
            Text(formatWidgetRating(value, decimals), maxLines = 1,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold))
        }
    }
}
