package id.my.chromazone.leetify_widget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.background
import androidx.glance.layout.padding
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

@Composable
internal fun PremierRatingBadge(text: String, band: PremierRatingBand, fontSize: Int) {
    val background = remember(band) { badgeBackground(band.surface, band.accent) }
    Text(
        text,
        modifier = GlanceModifier.background(ImageProvider(background))
            .padding(start = 23.dp, end = 13.dp),
        maxLines = 1,
        style = TextStyle(
            color = ColorProvider(band.ink),
            fontSize = fontSize.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
        ),
    )
}

// Draw only the badge decoration. The rating remains native, accessible text.
private fun badgeBackground(surface: Color, accent: Color): Bitmap =
    Bitmap.createBitmap(160, 40, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun fill(color: Color, vararg points: Float) {
            val path = Path().apply {
                moveTo(points[0], points[1])
                for (index in 2 until points.size step 2) lineTo(points[index], points[index + 1])
                close()
            }
            paint.color = color.toArgb()
            canvas.drawPath(path, paint)
        }
        fill(surface, 12f, 0f, 160f, 0f, 160f, 30f, 149f, 40f, 0f, 40f)
        fill(accent, 12f, 0f, 22f, 0f, 10f, 40f, 0f, 40f)
        fill(accent.copy(alpha = 0.88f), 26f, 0f, 36f, 0f, 24f, 40f, 14f, 40f)
    }
