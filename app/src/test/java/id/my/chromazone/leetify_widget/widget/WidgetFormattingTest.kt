package id.my.chromazone.leetify_widget.widget

import id.my.chromazone.leetify_widget.data.MatchOutcome
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class WidgetFormattingTest {
    @Test fun preservesScaleZeroSignAndUnavailableValues() {
        assertEquals("—", formatWidgetRating(null, 1, Locale.US))
        assertEquals("—", formatWidgetRating(Double.NaN, 1, Locale.US))
        assertEquals("0", formatWidgetRating(0.0, 1, Locale.US))
        assertEquals("-2.12", formatWidgetRating(-2.12, 2, Locale.US))
        assertEquals("61.3", formatWidgetRating(61.26, 1, Locale.US))
        assertEquals("19,309", formatWidgetRating(19309.0, 0, Locale.US))
        assertEquals("W L D ?", MatchOutcome.entries.joinToString(" ") { it.widgetLabel() })
    }
}
