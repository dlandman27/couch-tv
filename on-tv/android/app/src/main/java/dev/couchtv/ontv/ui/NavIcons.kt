package dev.couchtv.ontv.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The nav bar's icons, drawn as small stroked vectors so there's no icon-pack dependency. Each is
 * a 24x24 line glyph in the current content colour (tinted by the Icon that draws it).
 */
private fun lineIcon(name: String, body: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round, pathBuilder = body)
    }.build()

object NavIcons {
    val Home = lineIcon("home") {
        moveTo(3f, 10.5f); lineTo(12f, 3f); lineTo(21f, 10.5f)
        moveTo(5f, 9f); lineTo(5f, 20f); lineTo(19f, 20f); lineTo(19f, 9f)
        moveTo(10f, 20f); lineTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 20f)
    }
    val Movies = lineIcon("movies") {
        moveTo(3.5f, 5f); lineTo(20.5f, 5f); lineTo(20.5f, 19f); lineTo(3.5f, 19f); close()
        moveTo(8f, 5f); lineTo(8f, 19f); moveTo(16f, 5f); lineTo(16f, 19f)
        moveTo(3.5f, 12f); lineTo(20.5f, 12f)
    }
    val Shows = lineIcon("shows") {
        moveTo(3.5f, 7f); lineTo(20.5f, 7f); lineTo(20.5f, 18f); lineTo(3.5f, 18f); close()
        moveTo(8f, 21f); lineTo(16f, 21f); moveTo(9f, 7f); lineTo(5.5f, 3.5f); moveTo(15f, 7f); lineTo(18.5f, 3.5f)
    }
    val LiveTv = lineIcon("livetv") {
        moveTo(3.5f, 8f); lineTo(16.5f, 8f); lineTo(16.5f, 18f); lineTo(3.5f, 18f); close()
        moveTo(19.5f, 9.5f); arcTo(4f, 4f, 0f, false, true, 19.5f, 16.5f)
        moveTo(21.5f, 7f); arcTo(7f, 7f, 0f, false, true, 21.5f, 19f)
    }
    val Sports = lineIcon("sports") {
        moveTo(12f, 3.5f); arcTo(8.5f, 8.5f, 0f, true, true, 11.99f, 3.5f); close()
        moveTo(12f, 3.5f); lineTo(12f, 20.5f)
        moveTo(5.6f, 6f); arcTo(9f, 9f, 0f, false, false, 18.4f, 6f)
        moveTo(5.6f, 18f); arcTo(9f, 9f, 0f, false, true, 18.4f, 18f)
    }
    val Apps = lineIcon("apps") {
        moveTo(4f, 4f); lineTo(10f, 4f); lineTo(10f, 10f); lineTo(4f, 10f); close()
        moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f); lineTo(14f, 10f); close()
        moveTo(4f, 14f); lineTo(10f, 14f); lineTo(10f, 20f); lineTo(4f, 20f); close()
        moveTo(14f, 14f); lineTo(20f, 14f); lineTo(20f, 20f); lineTo(14f, 20f); close()
    }
    val Search = lineIcon("search") {
        moveTo(11f, 4f); arcTo(7f, 7f, 0f, true, true, 10.99f, 4f); close()
        moveTo(16.2f, 16.2f); lineTo(21f, 21f)
    }

    val tabs = listOf(Home, Movies, Shows, LiveTv, Sports, Apps)
}
