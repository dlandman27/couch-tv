package dev.couchtv.ontv.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** The reference design's palette, the same tokens the web skin uses. */
object Cj {
    val Ground = Color(0xFF050505)
    val Surface = Color(0xFF1D1728)
    val Surface2 = Color(0xFF251E32)
    val Hover = Color(0xFF302741)
    val Pill = Color(0xFF3C324F)
    val Hi = Color(0xFF7A6BAE)
    val Accent = Color(0xFF95FF50)
    val Text = Color(0xFFBEB3D6)
    val Dim = Color(0xFF756790)
    val White = Color.White
    val Ink = Color(0xFF10131C)

    /** The ambient bleed behind every page: near-black with a faint warm-purple glow. */
    val GroundBrush = Brush.verticalGradient(
        0f to Color(0xFF17101E), 0.35f to Color(0xFF0B0812), 1f to Ground)

    val HeroTitle = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.ExtraBold, color = White, letterSpacing = (-1).sp)
    val RowTitle = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, color = White)
    val Body = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal, color = Color(0xE6FFFFFF))
    val Label = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = Text)
    val Small = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, color = Dim)

    val BrandColors = mapOf(
        "netflix" to Color(0xFFE50914), "osn" to Color(0xFF8B5CF6), "disney" to Color(0xFF0E45A6),
        "apple" to Color(0xFFA2AAAD), "prime" to Color(0xFF00A8E1), "hbo" to Color(0xFF5822B4),
        "paramount" to Color(0xFF0064FF), "crunchyroll" to Color(0xFFF47521), "discovery" to Color(0xFF2175FF),
        "hulu" to Color(0xFF1CE783), "peacock" to Color(0xFF444B64), "starz" to Color(0xFF101820))
}
