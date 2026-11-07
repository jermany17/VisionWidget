package app.visionov.visionwidget.widget

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/**
 * The few things every card is painted with.
 *
 * These were in FaceRenderer alongside the three hand-drawn faces, which have gone —
 * the launcher's picker offers three sizes now rather than a provider per face, and
 * nothing in the app reached those three any other way. The helpers outlived them
 * because [CardRenderer] was built on the same ones.
 */

/**
 * What everything on a widget is painted with: edges smoothed, and glyphs placed at
 * subpixel positions rather than snapped to whole ones.
 *
 * Subpixel placement earns its keep here because a card is drawn at whatever size the
 * cell turns out to be rather than at a size the type was fitted to. Rounding every
 * glyph to the nearest whole pixel at those sizes is what leaves a line looking unevenly
 * spaced, and it shows most on the small type the eyebrows and dates are set in.
 */
internal const val CardPaintFlags = Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG

/**
 * A bitmap with the corners taken off it.
 *
 * Masked through a shader rather than clipped: a clipped path leaves the corners
 * stepped, and a widget sits against whatever wallpaper the user has, where that shows.
 */
internal fun roundedCanvas(
    widthPx: Int,
    heightPx: Int,
    radius: Float,
    content: (Canvas) -> Unit
): Bitmap {
    val square = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    content(Canvas(square))

    val rounded = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val paint = Paint(CardPaintFlags).apply {
        shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    Canvas(rounded).drawRoundRect(
        RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()),
        radius,
        radius,
        paint
    )
    square.recycle()
    return rounded
}

/**
 * Sets a line by the box it occupies rather than by its baseline, so the measurements
 * can be read straight off the design instead of being worked back from the metrics.
 */
internal fun Canvas.drawTextLine(
    text: String,
    x: Float,
    lineTop: Float,
    lineHeight: Float,
    paint: Paint
) {
    val metrics = paint.fontMetrics
    val baseline = lineTop + (lineHeight - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent
    drawText(text, x, baseline, paint)
}
