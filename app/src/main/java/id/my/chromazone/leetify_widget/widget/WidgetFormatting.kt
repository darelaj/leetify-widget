package id.my.chromazone.leetify_widget.widget

import id.my.chromazone.leetify_widget.data.MatchOutcome
import java.text.NumberFormat
import java.util.Locale

internal fun formatWidgetRating(value: Double?, decimals: Int, locale: Locale = Locale.getDefault()): String {
    if (value == null || !value.isFinite()) return "—"
    return NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = decimals
        minimumFractionDigits = 0
    }.format(value)
}

internal fun MatchOutcome.widgetLabel(): String = when (this) {
    MatchOutcome.WIN -> "W"
    MatchOutcome.LOSS -> "L"
    MatchOutcome.DRAW -> "D"
    MatchOutcome.UNKNOWN -> "?"
}
