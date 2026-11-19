package id.my.chromazone.leetify_widget.widget

import androidx.compose.ui.graphics.Color

internal enum class PremierRatingBand(val surface: Color, val accent: Color, val ink: Color = accent) {
    SILVER(Color(0xFF3B434B), Color(0xFFB8CAD6)),
    CYAN(Color(0xFF2D4455), Color(0xFF88BBE0)),
    BLUE(Color(0xFF1F295D), Color(0xFF687FEC), Color(0xFF7F96FF)),
    PURPLE(Color(0xFF422258), Color(0xFFBD6BFB), Color(0xFFC679FC)),
    PINK(Color(0xFF580060), Color(0xFFE615F1), Color(0xFFF65DFA)),
    RED(Color(0xFF560B0C), Color(0xFFEA4B48), Color(0xFFF66A64)),
    GOLD(Color(0xFF4B3900), Color(0xFFFED502)),
}

internal fun premierRatingBand(rating: Double?): PremierRatingBand? = when {
    rating == null || !rating.isFinite() || rating < 0 -> null
    rating < 5_000 -> PremierRatingBand.SILVER
    rating < 10_000 -> PremierRatingBand.CYAN
    rating < 15_000 -> PremierRatingBand.BLUE
    rating < 20_000 -> PremierRatingBand.PURPLE
    rating < 25_000 -> PremierRatingBand.PINK
    rating < 30_000 -> PremierRatingBand.RED
    else -> PremierRatingBand.GOLD
}
