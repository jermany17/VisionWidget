package com.example.visionwidget.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.visionwidget.ui.theme.Alignments
import com.example.visionwidget.ui.theme.BackgroundStyles
import com.example.visionwidget.ui.theme.CardThemes
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.WidgetStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The three cards Studio dresses, drawn for a home screen widget.
 *
 * Unlike the Gallery faces these carry the user's own words and wear whatever was
 * applied to them, so everything here is resolved from a [WidgetStyle] rather than
 * fixed. The measurements are the preview's own, scaled from the width the card is
 * given — which is what keeps a widget and the phone inside Studio the same drawing at
 * two sizes.
 */
private const val CARD_DESIGN_WIDTH = 202f

/** The card's own spacing, as the preview sets it. */
private const val CARD_PADDING = 22f
private const val CARD_SPACING = 9f

/**
 * A card drawn, and where its rows ended up.
 *
 * Top 3 has to be tickable, and a bitmap can't be. The rows come back as fractions of
 * the height so a transparent target can be laid over each one, which is the only way
 * to keep the design and still be able to act on it.
 */
data class RenderedCard(
    val bitmap: Bitmap,
    /** Top and bottom of each row, as a fraction of the card's height. */
    val rows: List<ClosedFloatingPointRange<Float>> = emptyList()
)

/** Everything a card draws with, resolved from what was applied to it. */
private class CardInk(
    val face: Typeface?,
    val onSurface: Int,
    val onSurfaceMuted: Int,
    val onSurfaceRule: Int,
    val onInk: Int,
    val border: Int?
)

private fun androidx.compose.ui.graphics.Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)

private fun cardInk(context: Context, style: WidgetStyle): CardInk {
    val theme = CardThemes[style.themeId]
    val font = UserFonts[style.fontId]
    val face = ResourcesCompat.getFont(context, font.resId)

    // Photo hands back its own ink: what the picture underneath looks like is unknown,
    // so the scrim is what guarantees contrast and the text colour has to match it.
    if (style.backgroundId == BackgroundStyles.PHOTO) {
        val photoInk = 0xFFF7F4EE.toInt()
        return CardInk(
            face = face,
            onSurface = photoInk,
            onSurfaceMuted = withAlpha(photoInk, 0.76f),
            onSurfaceRule = withAlpha(photoInk, 0.22f),
            onInk = 0xFF1C1A17.toInt(),
            border = null
        )
    }
    return CardInk(
        face = face,
        onSurface = theme.onSurface.toArgb(),
        onSurfaceMuted = theme.onSurfaceMuted.toArgb(),
        onSurfaceRule = theme.onSurfaceRule.toArgb(),
        onInk = theme.surface.toArgb(),
        border = if (style.backgroundId == BackgroundStyles.GLASS) {
            withAlpha(theme.onSurface.toArgb(), 0.22f)
        } else {
            theme.border?.toArgb()
        }
    )
}

private fun withAlpha(colour: Int, alpha: Float) =
    Color.argb((alpha * 255).toInt(), Color.red(colour), Color.green(colour), Color.blue(colour))

/** Paints the fill the applied background asks for, over the whole card. */
private fun drawCardSurface(
    canvas: Canvas,
    context: Context,
    style: WidgetStyle,
    photoUri: String?,
    width: Float,
    height: Float
) {
    val theme = CardThemes[style.themeId]
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    when (style.backgroundId) {
        BackgroundStyles.GRADIENT -> {
            // The design's fixed angle, measured with zero pointing up and increasing
            // clockwise, so the line reaches both corners it points at.
            val radians = Math.toRadians(158.0)
            val dx = sin(radians).toFloat()
            val dy = -cos(radians).toFloat()
            val half = (abs(width * dx) + abs(height * dy)) / 2f
            fill.shader = LinearGradient(
                width / 2f - dx * half, height / 2f - dy * half,
                width / 2f + dx * half, height / 2f + dy * half,
                theme.surface.toArgb(), theme.surfaceStep.toArgb(),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width, height, fill)
            fill.shader = null
        }

        BackgroundStyles.GLASS -> {
            fill.color = withAlpha(theme.surface.toArgb(), 0.74f)
            canvas.drawRect(0f, 0f, width, height, fill)
        }

        BackgroundStyles.PHOTO -> {
            val photo = photoUri?.let { loadPhoto(context, it) }
            if (photo != null) {
                canvas.drawBitmap(
                    photo,
                    coverRect(photo.width, photo.height, width.toInt(), height.toInt()),
                    RectF(0f, 0f, width, height),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
                photo.recycle()
            } else {
                fill.color = 0xFF6A6459.toInt()
                canvas.drawRect(0f, 0f, width, height, fill)
            }
            // The scrim is not adjustable, because it is what makes any photo safe.
            fill.shader = LinearGradient(
                0f, 0f, 0f, height,
                Color.argb(77, 0, 0, 0), Color.argb(158, 0, 0, 0),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width, height, fill)
            fill.shader = null
        }

        else -> {
            fill.color = theme.surface.toArgb()
            canvas.drawRect(0f, 0f, width, height, fill)
        }
    }
}

private fun loadPhoto(context: Context, uri: String): Bitmap? = runCatching {
    context.contentResolver.openInputStream(Uri.parse(uri)).use { BitmapFactory.decodeStream(it) }
}.getOrNull()

/** The slice of a source image that fills the card without distorting it. */
private fun coverRect(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): Rect {
    val scale = max(dstWidth.toFloat() / srcWidth, dstHeight.toFloat() / srcHeight)
    val visibleWidth = dstWidth / scale
    val visibleHeight = dstHeight / scale
    val left = (srcWidth - visibleWidth) / 2f
    val top = (srcHeight - visibleHeight) / 2f
    return Rect(
        left.toInt(),
        top.toInt(),
        (left + visibleWidth).toInt(),
        (top + visibleHeight).toInt()
    )
}

/** A block of the user's own words, wrapped to the card and set as the layout asks. */
private fun textBlock(
    text: String,
    face: Typeface?,
    sizePx: Float,
    lineHeightPx: Float,
    colour: Int,
    widthPx: Int,
    alignId: Int,
    letterSpacing: Float = 0f,
    strikeThrough: Boolean = false
): StaticLayout {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = face
        textSize = sizePx
        color = colour
        this.letterSpacing = letterSpacing
        isStrikeThruText = strikeThrough
    }
    val align = when (Alignments[alignId].textAlign) {
        androidx.compose.ui.text.style.TextAlign.Center -> Layout.Alignment.ALIGN_CENTER
        else -> Layout.Alignment.ALIGN_NORMAL
    }
    val extra = lineHeightPx - (paint.descent() - paint.ascent())
    return StaticLayout.Builder
        .obtain(text, 0, text.length, paint, widthPx.coerceAtLeast(1))
        .setAlignment(align)
        .setLineSpacing(extra, 1f)
        .setIncludePad(false)
        .build()
}

private fun Canvas.draw(layout: StaticLayout, x: Float, y: Float) {
    save()
    translate(x, y)
    layout.draw(this)
    restore()
}

/**
 * Draws a card: the fill, whatever [content] puts on it, and the hairline over the top.
 *
 * The card fills the cell, and the drawing is scaled until its contents fill the card.
 *
 * Studio shows a card that sits close around what it holds. A cell is rarely that
 * proportion, and the two ways of meeting it both go wrong: fitted, the card stops
 * short and leaves bars; stretched to the cell with the type left alone, the words
 * huddle in the middle of an empty panel. Neither is the card that was designed.
 *
 * So the scale itself is solved for. Everything — type, padding, rules, the tick beside
 * a row — moves together on one number, which is what holds the proportions; the
 * number is chosen so the contents come out as tall as the cell. Growing the type wraps
 * the words onto more lines, which makes the block taller again, so the height is not
 * a straight line in the scale and is searched rather than divided.
 */
private fun card(
    context: Context,
    widthPx: Int,
    cellHeightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    measure: (scale: Float) -> Float,
    content: (canvas: Canvas, scale: Float, offsetY: Float, heightPx: Float) -> Unit
): Bitmap {
    val scale = scaleToFill(widthPx / CARD_DESIGN_WIDTH, cellHeightPx.toFloat(), measure)
    val contentHeight = measure(scale)

    val heightPx = max(cellHeightPx.toFloat(), contentHeight).toInt().coerceAtLeast(1)
    val offsetY = (heightPx - contentHeight) / 2f
    val radius = style.cornerRadius * scale

    return roundedCanvas(widthPx, heightPx, radius) { canvas ->
        drawCardSurface(canvas, context, style, photoUri, widthPx.toFloat(), heightPx.toFloat())
        content(canvas, scale, offsetY, heightPx.toFloat())

        cardInk(context, style).border?.let { border ->
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = scale
                color = border
            }
            val inset = scale / 2f
            canvas.drawRoundRect(
                RectF(inset, inset, widthPx - inset, heightPx - inset),
                radius,
                radius,
                stroke
            )
        }
    }
}

/**
 * How far the design's own scale has to move for its contents to fill [targetHeight].
 *
 * Bounded either side of [designScale]: a card with almost nothing in it would
 * otherwise blow its three words up to fill a tall cell, and one with a great deal in
 * it would shrink them past reading. Within those bounds it is a bisection, since
 * measuring is cheap and the height climbs in steps as lines wrap rather than smoothly.
 */
private const val SCALE_FLOOR = 0.75f
private const val SCALE_CEILING = 1.9f
private const val SCALE_STEPS = 14

private fun scaleToFill(
    designScale: Float,
    targetHeight: Float,
    measure: (scale: Float) -> Float
): Float {
    var low = designScale * SCALE_FLOOR
    var high = designScale * SCALE_CEILING

    // Already too tall at its smallest, or still too short at its largest — there is
    // nothing between to find.
    if (measure(low) >= targetHeight) return low
    if (measure(high) <= targetHeight) return high

    repeat(SCALE_STEPS) {
        val mid = (low + high) / 2f
        if (measure(mid) <= targetHeight) low = mid else high = mid
    }
    return low
}

// ── The three cards ─────────────────────────────────────────────────────────────

/** Paints the vision card: its name, the steps toward it, and how long is left. */
fun renderVisionCard(
    context: Context,
    widthPx: Int,
    cellHeightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    goal: String?,
    milestones: List<Pair<String, Boolean>>,
    targetLine: String,
    weeksLine: String
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]
    val scale = widthPx / CARD_DESIGN_WIDTH
    val inner = (widthPx - 2 * CARD_PADDING * scale).toInt()

    fun layouts(s: Float): Triple<StaticLayout, StaticLayout?, List<StaticLayout>> {
        val eyebrow = textBlock("VISION", ink.face, 8f * s, 11f * s, ink.onSurfaceMuted, inner, style.alignId, 0.18f)
        val title = textBlock(
            align.format(goal ?: "No vision yet"),
            ink.face, 19f * s, 20f * s, ink.onSurface, inner, style.alignId
        )
        val rows = milestones.map { (step, done) ->
            textBlock(
                align.format(step), ink.face, 9f * s, 12f * s,
                if (done) ink.onSurfaceMuted else ink.onSurface,
                inner - (9f * s + 8f * s).toInt(), style.alignId, strikeThrough = done
            )
        }
        return Triple(eyebrow, title, rows)
    }

    return card(context, widthPx, cellHeightPx, style, photoUri, measure = { s ->
        val (eyebrow, title, rows) = layouts(s)
        val body = if (rows.isEmpty()) 12f * s else rows.sumOf { it.height }.toFloat() +
            CARD_SPACING * s * (rows.size - 1)
        2 * CARD_PADDING * s + eyebrow.height + CARD_SPACING * s + (title?.height ?: 0) +
            CARD_SPACING * s + s + CARD_SPACING * s + body +
            CARD_SPACING * s + s + CARD_SPACING * s + 10f * s
    }) { canvas, s, offsetY, _ ->
        val (eyebrow, title, rows) = layouts(s)
        val left = CARD_PADDING * s
        var y = CARD_PADDING * s + offsetY

        canvas.draw(eyebrow, left, y); y += eyebrow.height + CARD_SPACING * s
        title?.let { canvas.draw(it, left, y); y += it.height + CARD_SPACING * s }
        y = rule(canvas, ink, left, y, widthPx - left, s)

        if (rows.isEmpty()) {
            canvas.draw(
                textBlock(align.format("Nothing set yet."), ink.face, 9f * s, 12f * s, ink.onSurfaceMuted, inner, style.alignId),
                left, y
            )
            y += 12f * s + CARD_SPACING * s
        } else {
            milestones.forEachIndexed { index, (_, done) ->
                drawCheck(canvas, ink, left, y, done, s)
                canvas.draw(rows[index], left + 9f * s + 8f * s, y)
                y += rows[index].height + CARD_SPACING * s
            }
        }
        y = rule(canvas, ink, left, y - CARD_SPACING * s + CARD_SPACING * s, widthPx - left, s)

        val meta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ink.face
            textSize = 7f * s
            color = ink.onSurfaceMuted
            letterSpacing = 0.18f
        }
        canvas.drawTextLine(targetLine, left, y, 10f * s, meta)
        meta.textAlign = Paint.Align.RIGHT
        canvas.drawTextLine(weeksLine, widthPx - left, y, 10f * s, meta)
    }
}

/** A hairline across the card, and where the next thing starts below it. */
private fun rule(canvas: Canvas, ink: CardInk, left: Float, y: Float, right: Float, s: Float): Float {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink.onSurfaceRule
        strokeWidth = s
    }
    canvas.drawLine(left, y, right, y, paint)
    return y + s + CARD_SPACING * s
}

/** The ring, or the filled disc with its tick, beside a row that can be kept. */
private fun drawCheck(canvas: Canvas, ink: CardInk, left: Float, rowTop: Float, done: Boolean, s: Float) {
    val size = 9f * s
    val cx = left + size / 2f
    val cy = rowTop + 12f * s / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    if (done) {
        paint.color = ink.onSurfaceMuted
        canvas.drawCircle(cx, cy, size / 2f, paint)
        paint.color = ink.onInk
        paint.textSize = 6f * s
        paint.typeface = ink.face
        paint.textAlign = Paint.Align.CENTER
        canvas.drawTextLine("✓", cx, cy - 6f * s / 2f, 6f * s, paint)
    } else {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * s
        paint.color = ink.onSurfaceMuted
        canvas.drawCircle(cx, cy, size / 2f - 0.75f * s, paint)
    }
}

/** Paints today's three, and says where each row landed so it can be ticked. */
fun renderTopThreeCard(
    context: Context,
    widthPx: Int,
    cellHeightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    tasks: List<String?>,
    checked: List<Boolean>
): RenderedCard {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]
    val scale = widthPx / CARD_DESIGN_WIDTH
    val inner = (widthPx - 2 * CARD_PADDING * scale).toInt()
    val set = tasks.indices.filter { tasks[it] != null }
    val done = set.count { checked[it] }

    fun parts(s: Float): Pair<StaticLayout, List<StaticLayout>> {
        val eyebrow = textBlock(
            "TODAY'S TOP 3 · $done / ${set.size}",
            ink.face, 8f * s, 11f * s, ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )
        val rows = set.map { index ->
            textBlock(
                align.format(tasks[index].orEmpty()), ink.face, 9f * s, 12f * s,
                if (checked[index]) ink.onSurfaceMuted else ink.onSurface,
                inner - (9f * s + 8f * s).toInt(), style.alignId, strikeThrough = checked[index]
            )
        }
        return eyebrow to rows
    }

    val bounds = mutableListOf<ClosedFloatingPointRange<Float>>()

    val bitmap = card(context, widthPx, cellHeightPx, style, photoUri, measure = { s ->
        val (eyebrow, rows) = parts(s)
        val body = if (rows.isEmpty()) 12f * s
        else rows.sumOf { it.height }.toFloat() + CARD_SPACING * s * (rows.size - 1)
        2 * CARD_PADDING * s + eyebrow.height + CARD_SPACING * s + body
    }) { canvas, s, offsetY, drawnHeight ->
        val (eyebrow, rows) = parts(s)
        val left = CARD_PADDING * s
        var y = CARD_PADDING * s + offsetY

        canvas.draw(eyebrow, left, y); y += eyebrow.height + CARD_SPACING * s

        if (rows.isEmpty()) {
            canvas.draw(
                textBlock(align.format("Nothing set yet."), ink.face, 9f * s, 12f * s, ink.onSurfaceMuted, inner, style.alignId),
                left, y
            )
        } else {
            set.forEachIndexed { position, index ->
                val rowTop = y
                drawCheck(canvas, ink, left, y, checked[index], s)
                canvas.draw(rows[position], left + 9f * s + 8f * s, y)
                y += rows[position].height + CARD_SPACING * s
                bounds += (rowTop / drawnHeight)..((rowTop + rows[position].height) / drawnHeight)
            }
        }
    }

    return RenderedCard(bitmap, bounds)
}

/** Paints the daily line and the theme it was drawn from. */
fun renderWisdomCard(
    context: Context,
    widthPx: Int,
    cellHeightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    quote: String,
    category: String
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]
    val scale = widthPx / CARD_DESIGN_WIDTH
    val inner = (widthPx - 2 * CARD_PADDING * scale).toInt()

    fun parts(s: Float) = textBlock(
        align.format(quote), ink.face, 11f * s, 15f * s, ink.onSurface, inner, style.alignId
    ) to textBlock(
        category.uppercase(), ink.face, 7f * s, 10f * s, ink.onSurfaceMuted, inner, style.alignId, 0.18f
    )

    return card(context, widthPx, cellHeightPx, style, photoUri, measure = { s ->
        val (line, meta) = parts(s)
        2 * CARD_PADDING * s + line.height + CARD_SPACING * s + meta.height
    }) { canvas, s, offsetY, _ ->
        val (line, meta) = parts(s)
        val left = CARD_PADDING * s
        canvas.draw(line, left, CARD_PADDING * s + offsetY)
        canvas.draw(meta, left, CARD_PADDING * s + offsetY + line.height + CARD_SPACING * s)
    }
}
