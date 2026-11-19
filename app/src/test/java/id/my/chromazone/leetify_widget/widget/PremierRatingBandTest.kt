package id.my.chromazone.leetify_widget.widget

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremierRatingBandTest {
    @Test fun colorChangesAtEachTierBoundary() {
        for (threshold in 5_000..30_000 step 5_000) {
            assertNotEquals(premierRatingBand((threshold - 1).toDouble()), premierRatingBand(threshold.toDouble()))
        }
        assertEquals(PremierRatingBand.BLUE, premierRatingBand(13_873.0))
        assertEquals(PremierRatingBand.GOLD, premierRatingBand(35_000.0))
    }

    @Test fun ratingTextHasReadableContrastInEveryTier() {
        for (rating in 0..30_000 step 5_000) {
            val band = checkNotNull(premierRatingBand(rating.toDouble()))
            val contrast = (band.ink.luminance() + 0.05f) / (band.surface.luminance() + 0.05f)
            assertTrue("Rating $rating has contrast $contrast", contrast >= 4.5f)
        }
    }

    @Test fun missingAndInvalidScoresHaveNoTier() {
        for (invalid in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(premierRatingBand(invalid))
        }
    }
}
