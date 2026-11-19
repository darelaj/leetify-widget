package id.my.chromazone.leetify_widget.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private val goldBand = Color(0xFFFFCF66)

internal fun premierRatingBand(rating: Double?): Color? = when {
    rating == null || !rating.isFinite() || rating < 0 -> null
    rating < 5_000 -> Color(0xFF505760)
    rating < 10_000 -> Color(0xFF25637A)
    rating < 15_000 -> Color(0xFF294DB8)
    rating < 20_000 -> Color(0xFF6943A7)
    rating < 25_000 -> Color(0xFF972B78)
    rating < 30_000 -> Color(0xFFA93046)
    else -> goldBand
}

internal fun premierRatingSurface(band: Color): Color = lerp(Color(0xFF111522), band, 0.34f)

internal fun premierRatingInk(band: Color): Color = lerp(band, Color.White, 0.43f)
