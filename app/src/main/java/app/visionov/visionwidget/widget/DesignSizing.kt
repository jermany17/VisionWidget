package app.visionov.visionwidget.widget

import android.content.Context
import app.visionov.visionwidget.ui.gallery.GalleryWidget
import kotlin.math.sqrt

/**
 * How large a Gallery design is painted for the home screen.
 *
 * A budget in bytes rather than a width, because what decides this is how much a
 * picture costs to hand over, not how wide it looks. A widget's views travel to the
 * launcher inside a Binder transaction with about a megabyte to spend on all of it,
 * and a bitmap that does not fit is not reported — the views simply arrive empty and
 * the cell goes transparent.
 *
 * Measured on a device at 1080x2340: a design painted 640 across came to 1.64MB and
 * every widget holding one went blank, while the same design at 400 across came to
 * 0.64MB and drew correctly. The composition was healthy in both cases — the slot, the
 * bitmap and the cell size were all right there in the log — which is what made this
 * hard to see from the app's side.
 *
 * Six hundred and forty thousand bytes is comfortably under the limit and is the size
 * that was actually verified. It also costs nothing in sharpness worth having: a 2x2
 * cell on this phone is about 528 pixels across, so a design drawn much beyond that is
 * paying for detail the cell has nowhere to put.
 */
private const val MAX_DESIGN_BYTES = 640_000

/** Four bytes a pixel, which is what ARGB_8888 costs and what both renderers draw into. */
private const val BYTES_PER_PIXEL = 4

/**
 * The size a design is painted at for the home screen — its own width at the screen's
 * density, held under what one widget can actually carry.
 */
fun GalleryWidget.widgetWidthPx(context: Context): Int {
    val natural = (designWidth * context.resources.displayMetrics.density).toInt()
    // width × height ≤ budget, and height is width ÷ ratio, so width ≤ √(budget × ratio).
    val allowed = sqrt(MAX_DESIGN_BYTES.toDouble() / BYTES_PER_PIXEL * ratio).toInt()
    return natural.coerceAtMost(allowed).coerceAtLeast(1)
}

fun GalleryWidget.widgetHeightPx(context: Context): Int =
    (widgetWidthPx(context) / ratio).toInt().coerceAtLeast(1)
