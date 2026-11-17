package id.my.chromazone.leetify_widget.data

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal fun retryAtMillis(header: String?, nowMillis: Long): Long? {
    val value = header?.trim() ?: return null
    if (value.matches(Regex("[0-9]+"))) {
        val seconds = value.toLongOrNull() ?: return null
        val delay = seconds.coerceAtMost(Long.MAX_VALUE / 1000) * 1000
        return if (nowMillis > Long.MAX_VALUE - delay) Long.MAX_VALUE else nowMillis + delay
    }
    val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("GMT")
    }
    val position = ParsePosition(0)
    val date = format.parse(value, position) ?: return null
    return date.time.takeIf { position.index == value.length }
}
