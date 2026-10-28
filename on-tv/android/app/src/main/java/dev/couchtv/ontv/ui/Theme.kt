package dev.couchtv.ontv.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.couchtv.ontv.R

/** The reference design's palette and type, the same tokens the web skin uses. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
object Cj {
    // Ultramodern monochrome: near-black grounds, neutral dark surfaces, white as the accent.
    val Ground = Color(0xFF000000)
    val Surface = Color(0xFF131316)
    val Surface2 = Color(0xFF1B1B1F)
    val Hover = Color(0xFF26262B)
    val Pill = Color(0xFF1A1A1A)        // jet black, matches the search circle
    val Hi = Color(0xFFBFBFC6)          // secondary accent text (genre, labels)
    val Accent = Color(0xFFFFFFFF)      // focus/accent is white now, not green
    val Text = Color(0xFFCECED4)
    val Dim = Color(0xFF7E7E88)
    val White = Color.White
    val Ink = Color(0xFF0A0A0B)

    /** Montserrat for display, Inter for everything else - the reference design's pairing. These are
     * variable fonts, so each weight must drive the wght axis explicitly (the weight arg alone does
     * not), or everything renders at the file's thin default. */
    private fun v(w: Int) = FontVariation.Settings(FontVariation.weight(w))
    val Display = FontFamily(
        Font(R.font.montserrat_var, weight = FontWeight.Normal, variationSettings = v(400)),
        Font(R.font.montserrat_var, weight = FontWeight.Medium, variationSettings = v(500)),
        Font(R.font.montserrat_var, weight = FontWeight.SemiBold, variationSettings = v(600)),
        Font(R.font.montserrat_var, weight = FontWeight.Bold, variationSettings = v(700)),
        Font(R.font.montserrat_var, weight = FontWeight.ExtraBold, variationSettings = v(800)))
    val Sans = FontFamily(
        Font(R.font.inter_var, weight = FontWeight.Normal, variationSettings = v(400)),
        Font(R.font.inter_var, weight = FontWeight.Medium, variationSettings = v(500)),
        Font(R.font.inter_var, weight = FontWeight.SemiBold, variationSettings = v(600)),
        Font(R.font.inter_var, weight = FontWeight.Bold, variationSettings = v(700)))

    /** The ground: a clean near-black, no colour cast. */
    val GroundBrush = Brush.verticalGradient(
        0f to Color(0xFF0C0C0E), 0.3f to Color(0xFF050506), 1f to Ground)

    private val heroShadow = Shadow(Color(0xB3000000), Offset(0f, 3f), 14f)

    val HeroTitle = TextStyle(fontFamily = Display, fontSize = 54.sp, lineHeight = 58.sp,
        fontWeight = FontWeight.ExtraBold, color = White, letterSpacing = (-1).sp, shadow = heroShadow)
    val RowTitle = TextStyle(fontFamily = Display, fontSize = 20.sp, lineHeight = 26.sp,
        fontWeight = FontWeight.Bold, color = White)

    /** Crisp, restrained corners — ~12px on screen (6dp at this 2.0 density). */
    val Card: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    val CardSmall: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(5.dp)

    /** TV layout tokens, in dp. Density is 2.0, so each value is ×2 px on a 1920×1080 panel.
     * One source of truth for margins and the home hierarchy, so nothing is placed by ad-hoc offsets. */
    val Gutter = 48.dp          // side safe margin (96px)
    val FavIcon = 42.dp         // installed-app tile (84px), matches the reference row
    val RailCard = 128.dp       // Continue Watching card (256px, 16:9): six span the content width,
                                // a seventh peeks at the edge, so the rail reads as scrollable
    val RailGap = 12.dp
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
