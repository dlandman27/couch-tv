package dev.couchtv.ontv.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Turns an app's launcher icon into a uniform square tile, the way the reference home shows apps:
 * every app the same rounded square with its brand filling it edge to edge.
 *
 * Adaptive icons (most modern apps) are drawn layer by layer, background then foreground, without
 * the system's mask: the 108-unit layers are scaled so their central 72-unit "safe" area fills the
 * tile, and the caller's rounded shape does the clipping. Legacy icons have no background layer, so
 * they are drawn inset and the caller's dark tile shows behind them.
 */
object AppTile {
    /** [fullBleed] is true when the bitmap fills the whole tile; false means "draw a tile behind". */
    data class Tile(val image: ImageBitmap, val fullBleed: Boolean)

    fun render(d: Drawable, px: Int = 216): Tile {
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        if (Build.VERSION.SDK_INT >= 26 && d is AdaptiveIconDrawable) {
            // layers are 1.5× the visible area; centre them so the safe zone fills the tile
            val o = px / 4
            d.background?.apply { setBounds(-o, -o, px + o, px + o); draw(c) }
            d.foreground?.apply { setBounds(-o, -o, px + o, px + o); draw(c) }
            return Tile(bmp.asImageBitmap(), fullBleed = d.background != null)
        }
        val inset = (px * 0.16f).toInt()
        d.setBounds(inset, inset, px - inset, px - inset)
        d.draw(c)
        return Tile(bmp.asImageBitmap(), fullBleed = false)
    }
}
