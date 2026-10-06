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
import java.time.LocalDate
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

    return roundedCanvas(sizePx, sizePx, p(32f)) { canvas ->
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

    return roundedCanvas(sizePx, sizePx, p(31f)) { canvas ->
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
internal fun roundedCanvas(
    widthPx: Int,
    heightPx: Int,
    radius: Float,
    content: (Canvas) -> Unit
): Bitmap {
    val square = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    content(Canvas(square))

    val rounded = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

// ── 02 Blush ────────────────────────────────────────────────────────────────────
//
// A calendar rather than a date: the month laid out in full with today picked out of
// it. Drawn at the proportions its design states, which are wide rather than square.

/** The width the blush calendar is drawn at, and the height that goes with it. */
const val BLUSH_DESIGN_WIDTH = 480f
const val BLUSH_DESIGN_HEIGHT = 255f

/** Paints the blush calendar at [widthPx] across, in its own proportion. */
fun renderBlushCalendar(
    context: Context,
    widthPx: Int,
    today: LocalDate = LocalDate.now()
): Bitmap {
    val s = widthPx / BLUSH_DESIGN_WIDTH
    fun p(px: Float) = px * s
    val heightPx = (BLUSH_DESIGN_HEIGHT * s).toInt().coerceAtLeast(1)

    val serif = ResourcesCompat.getFont(context, R.font.instrument_serif_regular)
    val sans = ResourcesCompat.getFont(context, R.font.dm_sans_variable)

    return roundedCanvas(widthPx, heightPx, p(29f)) { canvas ->
        val w = widthPx.toFloat()
        val h = heightPx.toFloat()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        // The blush itself: one wash across the whole card, then three soft lights
        // pooled on top of it.
        fill.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(0xFFF9DFE3.toInt(), 0xFFF4C6CF.toInt(), 0xFFF3CDD4.toInt()),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, fill)

        listOf(
            Quintuple(p(96f), p(107f), p(120f), p(180f), Color.argb(133, 255, 239, 241)),
            Quintuple(p(360f), p(46f), p(160f), p(100f), Color.argb(128, 255, 231, 236)),
            Quintuple(p(326f), p(199f), p(170f), p(150f), Color.argb(43, 218, 139, 157))
        ).forEach { (cx, cy, rx, ry, colour) ->
            fill.shader = ellipseGlow(cx, cy, rx, ry, colour)
            canvas.drawRect(0f, 0f, w, h, fill)
        }
        fill.shader = null

        // The ruled grain. A single repeating hairline rather than noise — it is what
        // keeps a flat wash from reading as plastic.
        fill.color = Color.argb(10, 255, 255, 255)
        fill.strokeWidth = p(1f)
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w, y, fill)
            y += p(4f)
        }

        val weekdayPaint = italicPaint(serif, p(25f), Color.argb(209, 121, 68, 79))
        canvas.drawTextLine(
            today.format(FullWeekdayFormat),
            p(38f),
            p(27f),
            p(28.8f),
            weekdayPaint
        )

        val monthPaint = italicPaint(serif, p(14f), Color.argb(199, 111, 66, 76)).apply {
            textAlign = Paint.Align.CENTER
        }
        val columnWidth = p(190f) / 7f
        canvas.drawTextLine(
            today.monthValue.toString(),
            w - p(38f) - columnWidth / 2f,
            p(38f),
            p(14f),
            monthPaint
        )

        val dayPaint = italicPaint(serif, p(88f), Color.argb(219, 151, 68, 86)).apply {
            letterSpacing = -0.07f
        }
        val dayMetrics = dayPaint.fontMetrics
        canvas.drawText(
            today.dayOfMonth.toString(),
            p(38f),
            h - p(39f) - dayMetrics.descent,
            dayPaint
        )

        drawMonthGrid(canvas, today, s, w, serif, sans)
    }
}

/** The month as a grid, with today picked out of it. */
private fun drawMonthGrid(
    canvas: Canvas,
    today: LocalDate,
    s: Float,
    widthPx: Float,
    serif: android.graphics.Typeface?,
    sans: android.graphics.Typeface?
) {
    fun p(px: Float) = px * s

    val gridWidth = p(190f)
    val left = widthPx - p(38f) - gridWidth
    val column = gridWidth / 7f
    val ink = Color.argb(214, 111, 66, 76)

    val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = sans
        textSize = p(9f)
        color = ink
        letterSpacing = 0.08f
        textAlign = Paint.Align.CENTER
    }
    listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { index, letter ->
        canvas.drawTextLine(
            letter,
            left + column * (index + 0.5f),
            p(64f),
            p(9f),
            headingPaint
        )
    }

    val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = serif
        textSize = p(10f)
        color = ink
        textAlign = Paint.Align.CENTER
    }
    val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(102, 255, 255, 255) }

    // Weeks run Sunday first, as the heading row does, so the first of the month is
    // pushed along by however many days precede it.
    val first = today.withDayOfMonth(1)
    val lead = first.dayOfWeek.value % 7
    val rowTop = p(64f) + p(9f) + p(9f)
    val rowHeight = p(17f) + p(8f)

    for (day in 1..today.lengthOfMonth()) {
        val cell = lead + day - 1
        val cx = left + column * (cell % 7 + 0.5f)
        val cy = rowTop + rowHeight * (cell / 7)
        if (day == today.dayOfMonth) {
            canvas.drawCircle(cx, cy + p(17f) / 2f, p(10f), markPaint)
        }
        canvas.drawTextLine(day.toString(), cx, cy, p(17f), dayPaint)
    }
}

/** Georgia sets this face in italic, and none of the app's own faces carry one. */
private fun italicPaint(face: android.graphics.Typeface?, sizePx: Float, colour: Int) =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = face
        textSize = sizePx
        color = colour
        textSkewX = -0.21f
    }

/**
 * A pool of light rather than a circle of it. Android's radial shader is round, so the
 * ellipse the design asks for is a round one squashed along the way.
 */
private fun ellipseGlow(cx: Float, cy: Float, rx: Float, ry: Float, colour: Int): Shader {
    val shader = android.graphics.RadialGradient(
        cx, cy, rx,
        intArrayOf(colour, colour and 0x00FFFFFF),
        floatArrayOf(0f, 0.73f),
        Shader.TileMode.CLAMP
    )
    shader.setLocalMatrix(android.graphics.Matrix().apply { setScale(1f, ry / rx, cx, cy) })
    return shader
}

private data class Quintuple(
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float,
    val colour: Int
)

private val FullWeekdayFormat = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
