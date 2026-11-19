package id.my.chromazone.leetify_widget.widget

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PremierRatingBandTest {
    @Test fun colorChangesAtEachTierBoundary() {
        for (threshold in 5_000..30_000 step 5_000) {
            assertNotEquals(premierRatingBand((threshold - 1).toDouble()), premierRatingBand(threshold.toDouble()))
        }
        assertEquals(premierRatingBand(10_000.0), premierRatingBand(13_873.0))
        assertEquals(premierRatingBand(30_000.0), premierRatingBand(35_000.0))
        assertEquals(Color.White, premierRatingInk(checkNotNull(premierRatingBand(13_873.0))))
        assertNotEquals(Color.White, premierRatingInk(checkNotNull(premierRatingBand(30_000.0))))
    }

    @Test fun missingAndInvalidScoresHaveNoTier() {
        for (invalid in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(premierRatingBand(invalid))
        }
    }
}
