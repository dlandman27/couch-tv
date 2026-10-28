package dev.couchtv.ontv.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * A squircle: a rectangle whose corners are a superellipse (iOS-style continuous curve) rather than
 * a plain circular arc. One shared shape for every card, so the whole UI reads soft and of a piece.
 */
class SquircleShape(private val radius: Dp = 20.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width; val h = size.height
        val r = with(density) { radius.toPx() }.coerceAtMost(minOf(w, h) / 2f)
        val e = 0.5f            // 2/n with n = 4: the classic squircle exponent
        val steps = 14
        val half = (Math.PI / 2).toFloat()
        fun s(v: Float) = v.coerceAtLeast(0f).pow(e)
        val path = Path()
        path.moveTo(0f, r)
        // top-left corner: (0,r) -> (r,0)
        for (i in 0..steps) { val t = i.toFloat() / steps * half; path.lineTo(r - r * s(cos(t)), r - r * s(sin(t))) }
        path.lineTo(w - r, 0f)
        // top-right: (w-r,0) -> (w,r)
        for (i in 0..steps) { val t = i.toFloat() / steps * half; path.lineTo((w - r) + r * s(sin(t)), r - r * s(cos(t))) }
        path.lineTo(w, h - r)
        // bottom-right: (w,h-r) -> (w-r,h)
        for (i in 0..steps) { val t = i.toFloat() / steps * half; path.lineTo((w - r) + r * s(cos(t)), (h - r) + r * s(sin(t))) }
        path.lineTo(r, h)
        // bottom-left: (r,h) -> (0,h-r)
        for (i in 0..steps) { val t = i.toFloat() / steps * half; path.lineTo(r - r * s(sin(t)), (h - r) + r * s(cos(t))) }
        path.close()
        return Outline.Generic(path)
    }
}
