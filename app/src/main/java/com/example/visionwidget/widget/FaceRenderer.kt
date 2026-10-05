package com.example.visionwidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.visionwidget.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The Gallery faces, drawn for a home screen widget.
 *
 * A widget is a RemoteViews tree in the launcher's process, so none of the Compose
 * drawing the gallery uses can run there — no canvas, no brushes, no app fonts. These
 * faces are therefore painted into a bitmap the widget shows as a single image, which
 * keeps them exactly as designed rather than approximating them in what Glance offers.
 *
 * Every measurement is the one the designs state at 320pt, scaled to whatever the widget
 * turns out to be, so this and the gallery preview stay the same drawing.
 */
private const val DESIGN_SIZE = 320f

private val WeekdayFormat = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val DateFormat = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH)

/**
 * The wallpaper the glass face was drawn against. It travels with the face: a
 * translucent panel is only ever as readable as whatever sits behind it, and a home
 * screen offers no promise about that.
 */
private const val GalleryBackdrop = 0xFF706D6F.toInt()

/** Paints the clock face at [sizePx] square. */
fun renderClockFace(context: Context, sizePx: Int, now: LocalDateTime = LocalDateTime.now()): Bitmap {
    val s = sizePx / DESIGN_SIZE
    fun p(px: Float) = px * s

    val serif = ResourcesCompat.getFont(context, R.font.instrument_serif_regular)
    val sans = ResourcesCompat.getFont(context, R.font.dm_sans_variable)
    val ink = Color.argb(240, 255, 255, 255)

    return roundedCanvas(sizePx, p(32f)) { canvas ->
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        // The three layers the design states: the wallpaper, the panel's own
        // translucent fill over it, and a sheen falling across both.
        fill.color = GalleryBackdrop
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), fill)
        fill.color = Color.argb(148, 126, 122, 124)
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), fill)
        fill.shader = LinearGradient(
            0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
            Color.argb(26, 255, 255, 255),
            Color.argb(5, 255, 255, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), fill)
        fill.shader = null

        val datePaint = textPaint(sans, p(12f), ink, letterSpacing = 0.08f).apply {
            typeface = Typeface.create(sans, 500, false)
        }
        canvas.drawTextLine(now.format(WeekdayFormat).uppercase(), p(29f), p(29f), p(13.8f), datePaint)
        canvas.drawTextLine(now.format(DateFormat).uppercase(), p(29f), p(44.8f), p(13.8f), datePaint)

        val hourPaint = textPaint(serif, p(25f), ink).apply { textAlign = Paint.Align.RIGHT }
        val hour = (now.hour % 12).let { if (it == 0) 12 else it }
        canvas.drawTextLine(hour.toString(), sizePx - p(31f), p(27f), p(25f), hourPaint)

        drawDial(canvas, now, s, ink, serif)

        val captionPaint = textPaint(sans, p(7f), Color.argb(184, 255, 255, 255), letterSpacing = 0.35f)
            .apply { textAlign = Paint.Align.CENTER }
        val captionBottom = sizePx - p(30f)
        canvas.drawTextLine("GOOD THINGS", sizePx / 2f, captionBottom - p(23.8f), p(11.9f), captionPaint)
        canvas.drawTextLine("TAKE TIME", sizePx / 2f, captionBottom - p(11.9f), p(11.9f), captionPaint)
    }
}

/** The dial: a hairline ring, four figures, eight ticks and two hands. */
private fun drawDial(
    canvas: Canvas,
    now: LocalDateTime,
    s: Float,
    ink: Int,
    serif: Typeface?
) {
    fun p(px: Float) = px * s

    // Set a touch above the middle, so the caption below has room without the dial
    // looking pushed off centre.
    val cx = p(160f)
    val cy = p(165.7f)
    val radius = p(95f)

    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = p(1f)
        color = Color.argb(31, 255, 255, 255)
    }
    canvas.drawCircle(cx, cy, radius - p(11f), stroke)

    // The hours without a figure of their own. Measured from three o'clock and running
    // clockwise, the way the design's own rotations are given.
    stroke.color = Color.argb(87, 255, 255, 255)
    listOf(30f, 60f, 120f, 150f, 210f, 240f, 300f, 330f).forEach { degrees ->
        val radians = Math.toRadians(degrees.toDouble())
        val dx = cos(radians).toFloat()
        val dy = sin(radians).toFloat()
        canvas.drawLine(
            cx + dx * p(76f), cy + dy * p(76f),
            cx + dx * p(88f), cy + dy * p(88f),
            stroke
        )
    }

    val markerPaint = textPaint(serif, p(18f), ink).apply { textAlign = Paint.Align.CENTER }
    canvas.drawTextLine("12", cx, cy - radius + p(4f), p(18f), markerPaint)
    canvas.drawTextLine("6", cx, cy + radius - p(3f) - p(18f), p(18f), markerPaint)
    markerPaint.textAlign = Paint.Align.RIGHT
    canvas.drawTextLine("3", cx + radius, cy - p(9f), p(18f), markerPaint)
    markerPaint.textAlign = Paint.Align.LEFT
    canvas.drawTextLine("9", cx - radius, cy - p(9f), p(18f), markerPaint)

    // Both hands run clockwise from twelve. The hour hand carries the minutes and the
    // minute hand the seconds, so neither ever jumps.
    val hand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        strokeCap = Paint.Cap.ROUND
    }
    fun drawHand(degrees: Float, length: Float, width: Float) {
        val radians = Math.toRadians(degrees.toDouble())
        hand.strokeWidth = width
        canvas.drawLine(
            cx, cy,
            cx + sin(radians).toFloat() * length,
            cy - cos(radians).toFloat() * length,
            hand
        )
    }

    val minutes = now.minute + now.second / 60f
    val hours = (now.hour % 12) + minutes / 60f
    drawHand(hours * 30f, p(55f), p(3f))
    drawHand(minutes * 6f, p(78f), p(2f))

    hand.color = Color.argb(245, 255, 255, 255)
    canvas.drawCircle(cx, cy, p(4f), hand)
}

/** Paints the quote face at [sizePx] square. */
fun renderQuoteFace(context: Context, sizePx: Int, now: LocalDateTime = LocalDateTime.now()): Bitmap {
    val s = sizePx / DESIGN_SIZE
    fun p(px: Float) = px * s

    val serif = ResourcesCompat.getFont(context, R.font.instrument_serif_regular)
    val ink = 0xFF433A37.toInt()

    return roundedCanvas(sizePx, p(31f)) { canvas ->
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.color = 0xFFF5F0E9.toInt()
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), fill)

        val photo = BitmapFactory.decodeResource(context.resources, R.drawable.gallery_quote_photo)
        if (photo != null) {
            // Pulled down a little and brightened, so the photograph sits back from the
            // words rather than competing with them.
            val matrix = ColorMatrix().apply { setSaturation(0.72f) }
            matrix.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.06f, 0f, 0f, 0f, 0f,
                        0f, 1.06f, 0f, 0f, 0f,
                        0f, 0f, 1.06f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            val photoPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            canvas.drawBitmap(photo, coverRect(photo.width, photo.height, sizePx, 0.7f), RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), photoPaint)
            photo.recycle()
        }

        // Washed out from the left, far enough across that the words never sit on the
        // picture itself.
        fill.shader = LinearGradient(
            0f, 0f, sizePx.toFloat(), 0f,
            intArrayOf(
                Color.argb(250, 250, 246, 239),
                Color.argb(230, 250, 246, 239),
                Color.argb(102, 250, 246, 239),
                Color.argb(13, 250, 246, 239)
            ),
            floatArrayOf(0f, 0.28f, 0.53f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), fill)
        fill.shader = null

        val datePaint = textPaint(serif, p(12f), 0xFF776E6A.toInt(), letterSpacing = 0.07f)
        canvas.drawTextLine(now.format(WeekdayFormat).uppercase(), p(29f), p(28f), p(13.8f), datePaint)
        canvas.drawTextLine(now.format(DateFormat).uppercase(), p(29f), p(44.8f), p(13.8f), datePaint)

        // The block is set from its foot: the rule sits a fixed distance off the bottom
        // and the three lines stack upward from it.
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink; alpha = 191 }
        val ruleTop = sizePx - p(31f) - p(2f)
        canvas.drawRect(p(29f), ruleTop, p(29f) + p(31f), ruleTop + p(2f), rulePaint)

        val headlinePaint = textPaint(serif, p(29f), ink, letterSpacing = -0.025f)
        val lines = listOf("A more", "intentional", "me.")
        var lineTop = ruleTop - p(18f) - p(34.2f) * lines.size
        lines.forEach { line ->
            canvas.drawTextLine(line, p(29f), lineTop, p(34.2f), headlinePaint)
            lineTop += p(34.2f)
        }
    }
}

/**
 * The slice of a source image that fills a square without distorting it, held [bias] of
 * the way across whatever overflows — the design crops its photograph off to the right.
 */
private fun coverRect(srcWidth: Int, srcHeight: Int, sizePx: Int, bias: Float): Rect {
    val scale = max(sizePx.toFloat() / srcWidth, sizePx.toFloat() / srcHeight)
    val visibleWidth = sizePx / scale
    val visibleHeight = sizePx / scale
    val left = (srcWidth - visibleWidth) * bias
    val top = (srcHeight - visibleHeight) * 0.5f
    return Rect(
        left.toInt(),
        top.toInt(),
        (left + visibleWidth).toInt(),
        (top + visibleHeight).toInt()
    )
}

/**
 * Draws [content] and hands back a bitmap with the corners rounded off.
 *
 * Masked through a shader rather than clipped: a clipped path leaves the corners
 * stepped, and a widget sits against whatever wallpaper the user has, where that shows.
 */
private fun roundedCanvas(sizePx: Int, radius: Float, content: (Canvas) -> Unit): Bitmap {
    val square = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    content(Canvas(square))

    val rounded = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(square, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    Canvas(rounded).drawRoundRect(
        RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()),
        radius,
        radius,
        paint
    )
    square.recycle()
    return rounded
}

private fun textPaint(
    face: Typeface?,
    sizePx: Float,
    colour: Int,
    letterSpacing: Float = 0f
) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = face
    textSize = sizePx
    color = colour
    this.letterSpacing = letterSpacing
}

/**
 * Sets a line by the box it occupies rather than by its baseline, so the measurements
 * can be read straight off the design instead of being worked back from the metrics.
 */
private fun Canvas.drawTextLine(
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
