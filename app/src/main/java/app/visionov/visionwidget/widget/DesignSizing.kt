package app.visionov.visionwidget.widget

import android.content.Context
import app.visionov.visionwidget.ui.gallery.GalleryWidget
import kotlin.math.sqrt

/**
 * How large a Gallery design is painted for the home screen.
 *
 * As many pixels as a design is ever painted into for a widget.
 *
 * Not a limit the system imposes — a bitmap reaches the launcher as shared memory, and
 * what one widget may carry is six bytes per screen pixel, which is megabytes more than
 * any of these. It is the cell that sets this: a two-by-two is some five hundred pixels
 * across on a phone this size, so a design drawn at a thousand is paying twice over for
 * detail the cell has nowhere to put.
 *
 * Held as an area rather than a width, because a design's height follows its own
 * proportion rather than the cap: limiting the width alone let a square design through
 * at more than twice what a wide one cost.
 */
private const val MAX_DESIGN_PX = 640 * 640

/**
 * The size a design is painted at for the home screen — its own width at the screen's
 * density, so a widget is as sharp as the screen can draw it, held under what one widget
 * is allowed to carry.
 */
fun GalleryWidget.widgetWidthPx(context: Context): Int {
    val natural = (designWidth * context.resources.displayMetrics.density).toInt()
    // width × height ≤ MAX, and height is width ÷ ratio, so width ≤ √(MAX × ratio).
    val allowed = sqrt(MAX_DESIGN_PX.toDouble() * ratio).toInt()
    return natural.coerceAtMost(allowed).coerceAtLeast(1)
}

fun GalleryWidget.widgetHeightPx(context: Context): Int =
    (widgetWidthPx(context) / ratio).toInt().coerceAtLeast(1)
