package app.visionov.visionwidget.widget

import android.content.Context
import app.visionov.visionwidget.ui.gallery.GalleryWidget
import kotlin.math.sqrt

/**
 * How large a Gallery design is painted for the home screen.
 *
 * A guard on this app's own memory, and nothing more than that now.
 *
 * It used to be a Binder budget. The picture travelled to the launcher as pixels inside
 * a transaction, so how wide a design could be drawn was really a question of what would
 * fit in one, and it was measured that way: on a device at 1080x2340 a design painted 640
 * across came to 1.64MB and every widget holding one went blank, while the same design at
 * 400 across came to 0.64MB and drew.
 *
 * That reading was half the truth. Six hundred and forty thousand bytes fit, and cells
 * still went blank — because the megabyte a transaction buffer holds is shared by every
 * transaction the process has in flight rather than granted to each of them. Three clocks
 * turning on the same minute overflowed it between them, each one well inside the budget
 * on its own. No cap on a single picture could have fixed that; the cost was all of them
 * at once, and it grew with every clock added to the screen.
 *
 * The pixels no longer travel. [WidgetPictures] hands the launcher the address of the
 * file, so what a design costs is now a question about this app's heap and the time taken
 * to compress a PNG — a far looser one. Four million bytes is about a thousand pixels
 * square, more than a cell on a phone can show, and a design is drawn at its own width at
 * the screen's density until it reaches that.
 */
private const val MAX_DESIGN_BYTES = 4_000_000

/** Four bytes a pixel, which is what ARGB_8888 costs and what both renderers draw into. */
private const val BYTES_PER_PIXEL = 4

/**
 * The size a design is painted at for the home screen — its own width at the screen's
 * density, held under what this app can afford to draw at once.
 */
fun GalleryWidget.widgetWidthPx(context: Context): Int {
    val natural = (designWidth * context.resources.displayMetrics.density).toInt()
    // width × height ≤ budget, and height is width ÷ ratio, so width ≤ √(budget × ratio).
    val allowed = sqrt(MAX_DESIGN_BYTES.toDouble() / BYTES_PER_PIXEL * ratio).toInt()
    return natural.coerceAtMost(allowed).coerceAtLeast(1)
}

fun GalleryWidget.widgetHeightPx(context: Context): Int =
    (widgetWidthPx(context) / ratio).toInt().coerceAtLeast(1)
