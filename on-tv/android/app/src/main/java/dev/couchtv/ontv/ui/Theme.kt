package dev.couchtv.ontv.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.sp
import dev.couchtv.ontv.R

/** The reference design's palette and type, the same tokens the web skin uses. */
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

    /** Montserrat for display, Inter for everything else - the reference design's pairing. */
    val Display = FontFamily(
        Font(R.font.montserrat_var, weight = FontWeight.Medium),
        Font(R.font.montserrat_var, weight = FontWeight.SemiBold),
        Font(R.font.montserrat_var, weight = FontWeight.Bold),
        Font(R.font.montserrat_var, weight = FontWeight.ExtraBold))
    val Sans = FontFamily(
        Font(R.font.inter_var, weight = FontWeight.Normal),
        Font(R.font.inter_var, weight = FontWeight.Medium),
        Font(R.font.inter_var, weight = FontWeight.SemiBold),
        Font(R.font.inter_var, weight = FontWeight.Bold))

    /** The ground: the reference design's near-black, with only a breath of warmth at the top. */
    val GroundBrush = Brush.verticalGradient(
        0f to Color(0xFF0C0A10), 0.25f to Color(0xFF070608), 1f to Ground)

    private val heroShadow = Shadow(Color(0xB3000000), Offset(0f, 3f), 14f)

    val HeroTitle = TextStyle(fontFamily = Display, fontSize = 54.sp, lineHeight = 58.sp,
        fontWeight = FontWeight.ExtraBold, color = White, letterSpacing = (-1).sp, shadow = heroShadow)
    val RowTitle = TextStyle(fontFamily = Display, fontSize = 25.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.Bold, color = White)
    val Body = TextStyle(fontFamily = Sans, fontSize = 17.sp, lineHeight = 25.sp,
        fontWeight = FontWeight.Normal, color = Color(0xE6FFFFFF), shadow = heroShadow)
    val Label = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold, color = Text)
    val Small = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 17.sp,
        fontWeight = FontWeight.Medium, color = Dim)

    val BrandColors = mapOf(
        "netflix" to Color(0xFFE50914), "osn" to Color(0xFF8B5CF6), "disney" to Color(0xFF0E45A6),
        "apple" to Color(0xFFA2AAAD), "prime" to Color(0xFF00A8E1), "hbo" to Color(0xFF5822B4),
        "paramount" to Color(0xFF0064FF), "crunchyroll" to Color(0xFFF47521), "discovery" to Color(0xFF2175FF),
        "hulu" to Color(0xFF1CE783), "peacock" to Color(0xFF444B64), "starz" to Color(0xFF101820))
}
