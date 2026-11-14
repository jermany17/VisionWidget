package app.visionov.visionwidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import app.visionov.visionwidget.ui.gallery.GalleryWidget
import kotlin.math.sqrt

/**
 * How large a Gallery design is painted for the home screen.
 *
 * As many pixels as its cell can actually show, and not one more.
 *
 * This was a Binder budget once. The picture travelled to the launcher as pixels inside a
 * transaction, so how wide a design could be drawn was a question of what would fit in
 * one — and the answer was measured the hard way, a design at 640 across coming to 1.64MB
 * and blanking every widget while the same one at 400 drew. That reading was half the
 * truth: the megabyte a transaction holds is shared between every transaction in flight,
 * so clocks turning together overflowed it between them while each sat well inside the
 * cap. No limit on a single picture could have fixed that.
 *
 * The pixels no longer travel — [WidgetPictures] hands over the address of the file — but
 * the size still matters, and now for the launcher's sake rather than ours. It decodes
 * every picture it is pointed at and holds all of them at once, for as many widgets as
 * are on the screen. Four million bytes apiece was tried: seven widgets came to
 * twenty-eight megabytes, the low memory killer started taking processes across the
 * phone, and the cells went out one after another while every picture was painted and
 * promoted correctly.
 *
 * Sharing a fixed total between however many widgets are up was the wrong answer to that,
 * because it makes a design look different depending on what else is on the screen. The
 * right one costs nothing: a 2x2 cell measures 528 pixels across on this phone, and
 * `02-time-cloud-orbit` was being painted at 1020 to sit in one. Three quarters of that
 * was never visible. Painted to the cell it is the same picture to look at, a quarter of
 * the memory, and the same picture whether it is the only widget up or the twentieth.
 */

/**
 * A ceiling for a cell that reports something unreasonable.
 *
 * Not a budget — a cell big enough to need this does not exist on a phone. It is here so
 * that a bad answer from the launcher cannot ask for a bitmap that takes the app down.
 */
private const val MAX_PICTURE_BYTES = 2_500_000

/** Four bytes a pixel, which is what ARGB_8888 costs and what both renderers draw into. */
private const val BYTES_PER_PIXEL = 4

/**
 * How wide to paint a design to fill a cell of [cellWidthPx] by [cellHeightPx].
 *
 * The picture is fitted into its cell rather than stretched, so whichever side runs out
 * first is the one that decides: a square design in a cell taller than it is wide is
 * limited by the width, and the same design in a wide cell by the height. Painting to the
 * cell's full width in that second case would be spending pixels down the sides of
 * something that is about to be scaled to fit anyway.
 */
fun GalleryWidget.pictureWidthPx(cellWidthPx: Int, cellHeightPx: Int): Int {
    val fitted = minOf(cellWidthPx.toDouble(), cellHeightPx * ratio.toDouble())
    val ceiling = sqrt(MAX_PICTURE_BYTES.toDouble() / BYTES_PER_PIXEL * ratio)
    return minOf(fitted, ceiling).toInt().coerceAtLeast(1)
}

/** The matching height, which is the design's own proportion and not the cell's. */
fun GalleryWidget.pictureHeightPx(widthPx: Int): Int =
    (widthPx / ratio).toInt().coerceAtLeast(1)

/**
 * The cell [placed] is sitting in, in pixels, or null if the launcher will not say.
 *
 * The launcher writes the size it gave a widget into that widget's options, in dp, and
 * keeps it current as the cell is resized. That is the only honest source for this: the
 * provider's own declared minimum is what the widget may shrink to, not what it was
 * actually handed.
 *
 * The four figures it writes are not two corners of one rectangle. Each pair spans the
 * two ways the phone can be held: the width runs from its narrowest in portrait to its
 * widest in landscape, and the height the other way about, from a landscape row to a
 * taller portrait one. So the cell as it stands now is MIN_WIDTH by MAX_HEIGHT upright
 * and MAX_WIDTH by MIN_HEIGHT on its side — never a min with a min. Reading both minimums
 * described a cell shorter than any the widget is ever shown in, and a design painted to
 * that was stretched back up by the launcher to fill the real one.
 */
fun cellPxOf(context: Context, placed: PlacedWidget): Pair<Int, Int>? {
    val appWidgetId = appWidgetIdOf(context, placed.id) ?: return null
    val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return null
    val options = runCatching { manager.getAppWidgetOptions(appWidgetId) }.getOrNull() ?: return null

    val upright =
        context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
    val widthKey = if (upright) {
        AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
    } else {
        AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
    }
    val heightKey = if (upright) {
        AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
    } else {
        AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
    }

    val widthDp = options.getInt(widthKey, 0)
    val heightDp = options.getInt(heightKey, 0)
    if (widthDp <= 0 || heightDp <= 0) return null

    val density = context.resources.displayMetrics.density
    return (widthDp * density).toInt() to (heightDp * density).toInt()
}

/** How many cells across the home screen is taken to be, where it has not said. */
private const val HOME_COLUMNS = 4

/**
 * The cell to paint for when the launcher has not said yet.
 *
 * A widget placed a moment ago has no size written against it until the launcher gets
 * round to it, and a design still has to be drawn for it in the meantime. What it is
 * drawn at is the cell the design asks for, counted off the usual four-column grid —
 * which for a 2x2 is half the screen square, as it has always been, and for a 4x2 the
 * full width.
 *
 * Assuming the square for everything is what made the wide designs soft. A 4x2 was
 * painted 540 across to sit in a cell near 1080, and since the picture is only redrawn
 * when the design itself moves on, a calendar kept that first undersized painting until
 * the date changed.
 */
fun GalleryWidget.defaultCellPx(context: Context): Pair<Int, Int> {
    val metrics = context.resources.displayMetrics
    val unit = (metrics.widthPixels / HOME_COLUMNS).coerceAtLeast(1)
    val (across, down) = cellCount()
    return (across * unit).coerceAtLeast(1) to (down * unit).coerceAtLeast(1)
}

/**
 * [GalleryWidget.cells] as the two numbers it reads as — "4x2" as four by two.
 *
 * Falls back to the square, which is what every design was measured against before this
 * and is right for the twelve that are 2x2.
 */
private fun GalleryWidget.cellCount(): Pair<Int, Int> {
    val parts = cells.lowercase().split("x")
    val across = parts.getOrNull(0)?.trim()?.toIntOrNull()
    val down = parts.getOrNull(1)?.trim()?.toIntOrNull()
    if (across == null || down == null || across <= 0 || down <= 0) return 2 to 2
    return across to down
}
