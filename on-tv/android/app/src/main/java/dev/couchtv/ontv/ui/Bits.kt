package dev.couchtv.ontv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** The small shared pieces of the native look. */

/** The green star row, 0 to 5, drawn as glyphs: no icon pack, nothing to rasterise. */
@Composable
fun Stars(rating: Double, size: TextUnit = 15.sp) {
    if (rating <= 0) return
    val full = Math.round(rating).toInt().coerceIn(0, 5)
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(5) { i ->
            Text("★", style = TextStyle(fontSize = size, fontWeight = FontWeight.Bold,
                color = if (i < full) Cj.Accent else Cj.Dim.copy(alpha = 0.4f)))
        }
    }
}
