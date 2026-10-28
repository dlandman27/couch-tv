package dev.couchtv.ontv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.couchtv.ontv.ui.Cj

/** A pill chip: neutral until chosen, then the accent green (the sports filters, season picks). */
@Composable
fun FilterChip(name: String, on: Boolean, onOk: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onOk,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (on) Cj.Accent else Cj.Pill.copy(alpha = 0.9f),
            contentColor = if (on) Cj.Ink else Cj.Text,
            focusedContainerColor = if (on) Cj.Accent else Cj.Hover,
            focusedContentColor = if (on) Cj.Ink else Color.White),
        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = CircleShape)),
        modifier = modifier
    ) {
        Text(name, style = Cj.Label.copy(color = Color.Unspecified),
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 6.dp))
    }
}
