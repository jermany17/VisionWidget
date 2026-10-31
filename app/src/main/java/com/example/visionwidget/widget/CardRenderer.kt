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
import com.example.visionwidget.data.BASE_TOP_THREE_TASKS
import com.example.visionwidget.ui.theme.Alignments
import com.example.visionwidget.ui.theme.BackgroundStyles
import com.example.visionwidget.ui.theme.CardThemes
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.WidgetStyle
import com.example.visionwidget.ui.theme.themeOf
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

/**
 * The 4x2 card's measurements.
 *
 * A wide card is only about half as tall as it is broad, and the type that suited a
 * card drawn to its own contents is far too large for that: the title ran into the rule
 * and the why line into the dates beneath it. These are sized to the height a 4x2
 * actually has, so the whole of it sits inside the cell with room to spare.
 */
private const val MEDIUM_PADDING = 12f
private const val MEDIUM_SPACING = 5f
private const val MEDIUM_EYEBROW = 7f
private const val MEDIUM_EYEBROW_LINE = 9f
private const val MEDIUM_TITLE = 15f
private const val MEDIUM_TITLE_LINE = 17f
private const val MEDIUM_BODY = 7.5f
private const val MEDIUM_BODY_LINE = 10f
private const val MEDIUM_META = 6f
private const val MEDIUM_META_LINE = 8f
private const val MEDIUM_QUOTE = 11f
private const val MEDIUM_QUOTE_LINE = 14f
private const val MEDIUM_ROW = 9f
private const val MEDIUM_ROW_LINE = 11f
private const val MEDIUM_CHECK = 8f

/**
 * The 2x2 card's measurements.
 *
 * The square has height to spare where the wide card has none, and type scaled for the
 * wide one came out too small to read across a room. Each small face carries one figure
 * and little else, so that figure is given the room the square has.
 */
private const val SMALL_PADDING = 20f
private const val SMALL_SPACING = 8f
private const val SMALL_EYEBROW = 10f
private const val SMALL_EYEBROW_LINE = 13f
private const val SMALL_TITLE = 23f
private const val SMALL_TITLE_LINE = 26f
private const val SMALL_COUNT = 34f
private const val SMALL_COUNT_LINE = 38f
private const val SMALL_BODY = 10f
private const val SMALL_BODY_LINE = 13f
private const val SMALL_META = 9f
private const val SMALL_META_LINE = 12f
private const val SMALL_QUOTE = 14f
private const val SMALL_QUOTE_LINE = 19f

/**
 * The 4x4 card's measurements.
 *
 * Square and twice the height of the 4x2, which is room enough to carry the things the
 * wide card has to leave out: the steps toward a vision with their dates, and a foot
 * reading how the week has gone. Set between the two other sizes — larger than the wide
 * card's, since there is height for it, and short of the square's, since this one has
 * more than a single figure to hold.
 */
private const val LARGE_PADDING = 13f
private const val LARGE_SPACING = 6f
private const val LARGE_EYEBROW = 7.5f
private const val LARGE_EYEBROW_LINE = 10f
private const val LARGE_TITLE = 16f
private const val LARGE_TITLE_LINE = 18f
private const val LARGE_BODY = 7.5f
private const val LARGE_BODY_LINE = 9f
private const val LARGE_META = 6.5f
private const val LARGE_META_LINE = 9f
private const val LARGE_QUOTE = 15f
private const val LARGE_QUOTE_LINE = 19f
private const val LARGE_ROW = 9.5f
private const val LARGE_ROW_LINE = 12f
private const val LARGE_ROW_GAP = 4f
private const val LARGE_CHECK = 10f
private const val LARGE_COUNT = 18f
private const val LARGE_COUNT_LINE = 20f

/** How many steps the large vision card draws before it starts counting them instead. */
private const val MAX_MILESTONE_ROWS = 4

/**
 * How far the side margin is pushed in to clear a rounded corner, against its radius.
 *
 * A corner takes the card away diagonally, so the content's own corner has to sit
 * inside the curve: at the widest radius the slider offers, a margin the same on both
 * axes lands exactly on it, and the eyebrow at the head of the card and the dates at
 * its foot end up lying along the curve rather than inside it.
 *
 * Only the sides give way. Widening the top and bottom too would square the problem
 * away just as well, but a 4x2 is a shallow thing and the rule and dates are already
 * close under the words above them — there is width to spare there and no height. A
 * margin pushed in from the sides alone clears the curve with room over.
 */
private const val CORNER_CLEARANCE = 0.45f

private fun sideMargin(base: Float, cornerRadius: Int) =
    max(base, cornerRadius * CORNER_CLEARANCE)

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
    val theme = themeOf(style)
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
    val theme = themeOf(style)
    val fill = Paint(CardPaintFlags)

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
    strikeThrough: Boolean = false,
    /** Caps how tall a block is allowed to grow — a fixed square has no room to give. */
    maxLines: Int? = null
): StaticLayout {
    val paint = TextPaint(CardPaintFlags).apply {
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
    val builder = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, widthPx.coerceAtLeast(1))
        .setAlignment(align)
        .setLineSpacing(extra, 1f)
        .setIncludePad(false)
    if (maxLines != null) {
        builder.setMaxLines(maxLines).setEllipsize(android.text.TextUtils.TruncateAt.END)
    }
    return builder.build()
}

private fun Canvas.draw(layout: StaticLayout, x: Float, y: Float) {
    save()
    translate(x, y)
    layout.draw(this)
    restore()
}

/**
 * A card at the cell's own width and height, exactly — never shorter. Both the 4x2 and
 * the 4x4 are drawn through here; only what goes on them differs.
 *
 * Vision, Top 3 and Wisdom are each locked to one size now, so the cell is no longer a
 * window of unknown shape onto a card drawn at its own height: it is the card's shape.
 * A card sized to its content instead left a gap below a short one — empty space the
 * cell handed over that nothing was drawn into. Content that runs long is capped by
 * [content] itself instead, each in its own way, rather than shrinking the whole card.
 */
private fun fixedCard(
    context: Context,
    widthPx: Int,
    heightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    content: (canvas: Canvas, scale: Float, width: Float, height: Float) -> Unit
): Bitmap {
    val scale = widthPx / CARD_DESIGN_WIDTH
    val radius = style.cornerRadius * scale
    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    return roundedCanvas(widthPx, heightPx, radius) { canvas ->
        drawCardSurface(canvas, context, style, photoUri, w, h)
        content(canvas, scale, w, h)
        drawCardBorder(canvas, context, style, w, h, radius, scale)
    }
}

/** The hairline some themes draw just inside the card's own edge. */
private fun drawCardBorder(
    canvas: Canvas,
    context: Context,
    style: WidgetStyle,
    widthPx: Float,
    heightPx: Float,
    radius: Float,
    scale: Float
) {
    cardInk(context, style).border?.let { border ->
        val stroke = Paint(CardPaintFlags).apply {
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

/**
 * A fixed square card, as small as the 2x2 widgets are — their own size, rather than
 * the content's. A square this size has no room for everything the 4x2 card shows, so
 * each small face keeps only its headline figure, pinned to the top and bottom edges
 * with whatever room is left between them held open rather than packed.
 */
private fun smallCard(
    context: Context,
    sidePx: Int,
    style: WidgetStyle,
    photoUri: String?,
    content: (canvas: Canvas, scale: Float, side: Float) -> Unit
): Bitmap {
    val scale = sidePx / CARD_DESIGN_WIDTH
    val radius = style.cornerRadius * scale
    val side = sidePx.toFloat()

    return roundedCanvas(sidePx, sidePx, radius) { canvas ->
        drawCardSurface(canvas, context, style, photoUri, side, side)
        content(canvas, scale, side)
        drawCardBorder(canvas, context, style, side, side, radius, scale)
    }
}

// ── The three cards ─────────────────────────────────────────────────────────────

/** Paints the vision card: its name, why it matters, and how long is left. */
fun renderVisionCard(
    context: Context,
    widthPx: Int,
    heightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    goal: String?,
    why: String?,
    targetLine: String,
    weeksLine: String
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]

    return fixedCard(context, widthPx, heightPx, style, photoUri) { canvas, s, w, h ->
        val padX = sideMargin(MEDIUM_PADDING, style.cornerRadius) * s
        val padY = MEDIUM_PADDING * s
        val left = padX
        val inner = (w - 2 * left).toInt()

        // The rule and the dates beneath it keep the card's own bottom edge, wherever
        // the words above them happen to end — that gap is what holds the card to its
        // full height. Their places are settled first so the title can be given what
        // is left rather than running into them.
        val metaHeight = MEDIUM_META_LINE * s
        val metaTop = h - padY - metaHeight
        val ruleY = metaTop - MEDIUM_SPACING * s - s

        val eyebrow = textBlock(
            "VISION", ink.face, MEDIUM_EYEBROW * s, MEDIUM_EYEBROW_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )
        var y = padY
        canvas.draw(eyebrow, left, y); y += eyebrow.height + MEDIUM_SPACING * s

        // Whatever height is left between the eyebrow and the rule, less the single
        // line the why takes. A goal too long for that is cut rather than allowed to
        // push everything below it off the card.
        val forTitle = ruleY - MEDIUM_SPACING * s - y - MEDIUM_SPACING * s - MEDIUM_BODY_LINE * s
        val titleLines = (forTitle / (MEDIUM_TITLE_LINE * s)).toInt().coerceIn(1, 2)
        val title = textBlock(
            align.format(goal ?: "No vision yet"),
            ink.face, MEDIUM_TITLE * s, MEDIUM_TITLE_LINE * s, ink.onSurface, inner,
            style.alignId, maxLines = titleLines
        )
        canvas.draw(title, left, y); y += title.height + MEDIUM_SPACING * s

        val whyLine = textBlock(
            align.format(why?.takeIf { it.isNotBlank() } ?: "Nothing set yet."),
            ink.face, MEDIUM_BODY * s, MEDIUM_BODY_LINE * s, ink.onSurfaceMuted, inner,
            style.alignId, maxLines = 1
        )
        canvas.draw(whyLine, left, y)

        val rulePaint = Paint(CardPaintFlags).apply {
            color = ink.onSurfaceRule
            strokeWidth = s
        }
        canvas.drawLine(left, ruleY, w - left, ruleY, rulePaint)

        val meta = Paint(CardPaintFlags).apply {
            typeface = ink.face
            textSize = MEDIUM_META * s
            color = ink.onSurfaceMuted
            letterSpacing = 0.18f
        }
        canvas.drawTextLine(targetLine, left, metaTop, metaHeight, meta)
        meta.textAlign = Paint.Align.RIGHT
        canvas.drawTextLine(weeksLine, w - left, metaTop, metaHeight, meta)
    }
}

/** The ring, or the filled disc with its tick, beside a row that can be kept. */
private fun drawCheck(
    canvas: Canvas,
    ink: CardInk,
    left: Float,
    rowTop: Float,
    done: Boolean,
    s: Float,
    diameter: Float,
    rowLine: Float
) {
    val cx = left + diameter / 2f
    val cy = rowTop + rowLine / 2f
    val tick = diameter * 0.66f
    val paint = Paint(CardPaintFlags)
    if (done) {
        paint.color = ink.onSurfaceMuted
        canvas.drawCircle(cx, cy, diameter / 2f, paint)
        paint.color = ink.onInk
        paint.textSize = tick
        paint.typeface = ink.face
        paint.textAlign = Paint.Align.CENTER
        canvas.drawTextLine("✓", cx, cy - tick / 2f, tick, paint)
    } else {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * s
        paint.color = ink.onSurfaceMuted
        canvas.drawCircle(cx, cy, diameter / 2f - 0.75f * s, paint)
    }
}

/**
 * Paints today's three.
 *
 * Capped at [BASE_TOP_THREE_TASKS] rows rather than however many are set — a fixed card
 * has no more room for a sixth row than it does a fourth, so anything past the base
 * three is named rather than drawn.
 */
fun renderTopThreeCard(
    context: Context,
    widthPx: Int,
    heightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    tasks: List<String?>,
    checked: List<Boolean>
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]
    val set = tasks.indices.filter { tasks[it] != null }
    val done = set.count { checked[it] }
    val shown = set.take(BASE_TOP_THREE_TASKS)

    return fixedCard(context, widthPx, heightPx, style, photoUri) { canvas, s, w, h ->
        val padX = sideMargin(MEDIUM_PADDING, style.cornerRadius) * s
        val padY = MEDIUM_PADDING * s
        val left = padX
        val inner = (w - 2 * left).toInt()
        val check = MEDIUM_CHECK * s
        val gutter = check + MEDIUM_SPACING * s
        val rowInner = inner - gutter.toInt()

        val eyebrow = textBlock(
            "TODAY'S TOP 3 · $done / ${set.size}",
            ink.face, MEDIUM_EYEBROW * s, MEDIUM_EYEBROW_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )
        var y = padY
        canvas.draw(eyebrow, left, y); y += eyebrow.height + MEDIUM_SPACING * s

        if (shown.isEmpty()) {
            canvas.draw(
                textBlock(
                    align.format("Nothing set yet."), ink.face, MEDIUM_ROW * s, MEDIUM_ROW_LINE * s,
                    ink.onSurfaceMuted, inner, style.alignId
                ),
                left, y
            )
        } else {
            // How many rows the card's own height has room for, which is not always the
            // three it means to show: a flatter cell than the usual 4x2 would otherwise
            // run the last of them out past the bottom edge. The strip at the foot is
            // only kept back when something is actually being left out of the list.
            val rowSpan = MEDIUM_ROW_LINE * s + MEDIUM_SPACING * s
            val strip = MEDIUM_META_LINE * s + MEDIUM_SPACING * s
            fun roomFor(reserved: Float) =
                ((h - padY - reserved - y + MEDIUM_SPACING * s) / rowSpan)
                    .toInt().coerceAtLeast(1)

            var room = roomFor(0f)
            if (set.size > room) room = roomFor(strip)

            val visible = shown.take(room)
            val hidden = set.size - visible.size

            visible.forEach { index ->
                val row = textBlock(
                    align.format(tasks[index].orEmpty()), ink.face, MEDIUM_ROW * s, MEDIUM_ROW_LINE * s,
                    if (checked[index]) ink.onSurfaceMuted else ink.onSurface,
                    rowInner, style.alignId, strikeThrough = checked[index], maxLines = 1
                )
                drawCheck(canvas, ink, left, y, checked[index], s, check, MEDIUM_ROW_LINE * s)
                canvas.draw(row, left + gutter, y)
                y += row.height + MEDIUM_SPACING * s
            }
            // Down in the corner rather than in line with the rows: it is a count of
            // what isn't shown, not another thing to tick off.
            if (hidden > 0) {
                val more = Paint(CardPaintFlags).apply {
                    typeface = ink.face
                    textSize = MEDIUM_META * s
                    color = ink.onSurfaceMuted
                    letterSpacing = 0.14f
                    textAlign = Paint.Align.RIGHT
                }
                val moreHeight = MEDIUM_META_LINE * s
                canvas.drawTextLine(
                    "+ $hidden MORE", w - left, h - padY - moreHeight, moreHeight, more
                )
            }
        }
    }
}

/** Paints the daily line and the theme it was drawn from, as many words as fit. */
fun renderWisdomCard(
    context: Context,
    widthPx: Int,
    heightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    quote: String,
    category: String
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]

    return fixedCard(context, widthPx, heightPx, style, photoUri) { canvas, s, w, h ->
        val padX = sideMargin(MEDIUM_PADDING, style.cornerRadius) * s
        val padY = MEDIUM_PADDING * s
        val left = padX
        val inner = (w - 2 * left).toInt()

        val meta = textBlock(
            category.uppercase(), ink.face, MEDIUM_META * s, MEDIUM_META_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )

        val lineHeight = MEDIUM_QUOTE_LINE * s
        val available = h - 2 * padY - meta.height - MEDIUM_SPACING * s
        val maxLines = (available / lineHeight).toInt().coerceAtLeast(1)
        val line = textBlock(
            align.format(quote), ink.face, MEDIUM_QUOTE * s, lineHeight, ink.onSurface, inner,
            style.alignId, maxLines = maxLines
        )

        // The line and the theme under it sit together in the middle of the card rather
        // than one at each edge: a short quote pinned to the top with its theme pinned
        // to the bottom reads as two things, and it is one.
        val block = line.height + MEDIUM_SPACING * s + meta.height
        val top = (h - block) / 2f
        canvas.draw(line, left, top)
        canvas.draw(meta, left, top + line.height + MEDIUM_SPACING * s)
    }
}

// ── The same three, fixed at 4x4 ────────────────────────────────────────────────

/** One step toward a vision, as the large card draws it. */
data class MilestoneLine(val step: String, val checked: Boolean, val due: String)

/**
 * Paints the vision card at 4x4: the goal, why it matters, the steps toward it, and
 * how long is left.
 *
 * The steps are what the 4x2 has no room for. Capped all the same — a card has an edge
 * however tall it is, and whatever doesn't fit is counted rather than drawn.
 */
fun renderVisionCardLarge(
    context: Context,
    widthPx: Int,
    heightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    goal: String?,
    why: String?,
    milestones: List<MilestoneLine>,
    targetLine: String,
    weeksLine: String
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]
    val reached = milestones.count { it.checked }

    return fixedCard(context, widthPx, heightPx, style, photoUri) { canvas, s, w, h ->
        val padX = sideMargin(LARGE_PADDING, style.cornerRadius) * s
        val padY = LARGE_PADDING * s
        val left = padX
        val inner = (w - 2 * left).toInt()

        val rulePaint = Paint(CardPaintFlags).apply {
            color = ink.onSurfaceRule
            strokeWidth = s
        }
        val meta = Paint(CardPaintFlags).apply {
            typeface = ink.face
            textSize = LARGE_META * s
            color = ink.onSurfaceMuted
            letterSpacing = 0.18f
        }

        // The foot of the card is settled first — the dates sit against the bottom
        // edge, and the steps above take whatever is left rather than pushing them off.
        val metaHeight = LARGE_META_LINE * s
        val metaTop = h - padY - metaHeight
        val footRule = metaTop - LARGE_SPACING * s - s

        var y = padY
        val eyebrow = textBlock(
            "VISION", ink.face, LARGE_EYEBROW * s, LARGE_EYEBROW_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )
        canvas.draw(eyebrow, left, y); y += eyebrow.height + LARGE_SPACING * s

        val title = textBlock(
            align.format(goal ?: "No vision yet"),
            ink.face, LARGE_TITLE * s, LARGE_TITLE_LINE * s, ink.onSurface, inner,
            style.alignId, maxLines = 2
        )
        canvas.draw(title, left, y); y += title.height + LARGE_SPACING * s

        val whyLine = textBlock(
            align.format(why?.takeIf { it.isNotBlank() } ?: "Nothing set yet."),
            ink.face, LARGE_BODY * s, LARGE_BODY_LINE * s, ink.onSurfaceMuted, inner,
            style.alignId, maxLines = 1
        )
        canvas.draw(whyLine, left, y); y += whyLine.height + LARGE_SPACING * s

        canvas.drawLine(left, y, w - left, y, rulePaint)
        y += s + LARGE_SPACING * s

        val stepsEyebrow = textBlock(
            "MILESTONES · $reached / ${milestones.size} REACHED",
            ink.face, LARGE_EYEBROW * s, LARGE_EYEBROW_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )
        canvas.draw(stepsEyebrow, left, y); y += stepsEyebrow.height + LARGE_SPACING * s

        val check = LARGE_CHECK * s
        val gutter = check + LARGE_SPACING * s
        val rowLine = LARGE_ROW_LINE * s
        val rowSpan = rowLine + LARGE_ROW_GAP * s

        // The date on the right is what the step has to make way for, so it is measured
        // first and the words are given the rest.
        val dateWidth = milestones.maxOfOrNull { meta.measureText(it.due) } ?: 0f
        val rowInner = (inner - gutter - dateWidth - LARGE_SPACING * s).toInt().coerceAtLeast(1)

        // Whatever the foot leaves, less a line for the count of any steps left out.
        val roomBelow = footRule - LARGE_SPACING * s - y
        var room = ((roomBelow + LARGE_ROW_GAP * s) / rowSpan).toInt().coerceAtLeast(0)
        if (milestones.size > room) {
            room = ((roomBelow - rowSpan + LARGE_ROW_GAP * s) / rowSpan).toInt().coerceAtLeast(0)
        }
        val visible = milestones.take(room.coerceAtMost(MAX_MILESTONE_ROWS))
        val hidden = milestones.size - visible.size

        if (milestones.isEmpty()) {
            canvas.draw(
                textBlock(
                    align.format("No steps yet."), ink.face, LARGE_ROW * s, rowLine,
                    ink.onSurfaceMuted, inner, style.alignId
                ),
                left, y
            )
        } else {
            visible.forEach { step ->
                val row = textBlock(
                    align.format(step.step), ink.face, LARGE_ROW * s, rowLine,
                    if (step.checked) ink.onSurfaceMuted else ink.onSurface,
                    rowInner, style.alignId, strikeThrough = step.checked, maxLines = 1
                )
                drawCheck(canvas, ink, left, y, step.checked, s, check, rowLine)
                canvas.draw(row, left + gutter, y)
                meta.textAlign = Paint.Align.RIGHT
                canvas.drawTextLine(step.due, w - left, y, rowLine, meta)
                meta.textAlign = Paint.Align.LEFT
                y += rowSpan
            }
            if (hidden > 0) {
                meta.textAlign = Paint.Align.RIGHT
                canvas.drawTextLine("+ $hidden MORE", w - left, y, rowLine, meta)
                meta.textAlign = Paint.Align.LEFT
            }
        }

        canvas.drawLine(left, footRule, w - left, footRule, rulePaint)
        canvas.drawTextLine(targetLine, left, metaTop, metaHeight, meta)
        meta.textAlign = Paint.Align.RIGHT
        canvas.drawTextLine(weeksLine, w - left, metaTop, metaHeight, meta)
    }
}

/**
 * Paints today's three at 4x4: the count up in the corner, the tasks under it, and how
 * the week has gone along the foot.
 */
fun renderTopThreeCardLarge(
    context: Context,
    widthPx: Int,
    heightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    tasks: List<String?>,
    checked: List<Boolean>,
    streakLine: String,
    weekLine: String
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]
    val set = tasks.indices.filter { tasks[it] != null }
    val done = set.count { checked[it] }

    return fixedCard(context, widthPx, heightPx, style, photoUri) { canvas, s, w, h ->
        val padX = sideMargin(LARGE_PADDING, style.cornerRadius) * s
        val padY = LARGE_PADDING * s
        val left = padX
        val inner = (w - 2 * left).toInt()

        val rulePaint = Paint(CardPaintFlags).apply {
            color = ink.onSurfaceRule
            strokeWidth = s
        }
        val meta = Paint(CardPaintFlags).apply {
            typeface = ink.face
            textSize = LARGE_META * s
            color = ink.onSurfaceMuted
            letterSpacing = 0.18f
        }

        val metaHeight = LARGE_META_LINE * s
        val metaTop = h - padY - metaHeight
        val footRule = metaTop - LARGE_SPACING * s - s

        // The count is set large against the top right, with the label beside it on the
        // same line — the two together are the card's own heading.
        val countHeight = LARGE_COUNT_LINE * s
        val count = Paint(CardPaintFlags).apply {
            typeface = ink.face
            textSize = LARGE_COUNT * s
            color = ink.onSurface
            textAlign = Paint.Align.RIGHT
        }
        var y = padY
        canvas.drawTextLine("$done/${set.size}", w - left, y, countHeight, count)

        val eyebrow = textBlock(
            "TODAY'S TOP 3", ink.face, LARGE_EYEBROW * s, LARGE_EYEBROW_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )
        canvas.draw(eyebrow, left, y + (countHeight - eyebrow.height) / 2f)
        y += countHeight + LARGE_SPACING * s

        canvas.drawLine(left, y, w - left, y, rulePaint)
        y += s + LARGE_SPACING * s

        val check = LARGE_CHECK * s
        val gutter = check + LARGE_SPACING * s
        val rowLine = LARGE_ROW_LINE * s
        val rowSpan = rowLine + LARGE_SPACING * s
        val shown = set.take(BASE_TOP_THREE_TASKS)

        val roomBelow = footRule - LARGE_SPACING * s - y
        var room = ((roomBelow + LARGE_SPACING * s) / rowSpan).toInt().coerceAtLeast(1)
        if (set.size > room) {
            room = ((roomBelow - rowSpan + LARGE_SPACING * s) / rowSpan).toInt().coerceAtLeast(1)
        }
        val visible = shown.take(room)
        val hidden = set.size - visible.size

        if (visible.isEmpty()) {
            canvas.draw(
                textBlock(
                    align.format("Nothing set yet."), ink.face, LARGE_ROW * s, rowLine,
                    ink.onSurfaceMuted, inner, style.alignId
                ),
                left, y
            )
        } else {
            visible.forEach { index ->
                val row = textBlock(
                    align.format(tasks[index].orEmpty()), ink.face, LARGE_ROW * s, rowLine,
                    if (checked[index]) ink.onSurfaceMuted else ink.onSurface,
                    (inner - gutter).toInt(), style.alignId,
                    strikeThrough = checked[index], maxLines = 1
                )
                drawCheck(canvas, ink, left, y, checked[index], s, check, rowLine)
                canvas.draw(row, left + gutter, y)
                y += rowSpan
            }
            if (hidden > 0) {
                meta.textAlign = Paint.Align.RIGHT
                canvas.drawTextLine("+ $hidden MORE", w - left, y, rowLine, meta)
                meta.textAlign = Paint.Align.LEFT
            }
        }

        canvas.drawLine(left, footRule, w - left, footRule, rulePaint)
        canvas.drawTextLine(streakLine, left, metaTop, metaHeight, meta)
        meta.textAlign = Paint.Align.RIGHT
        canvas.drawTextLine(weekLine, w - left, metaTop, metaHeight, meta)
    }
}

/**
 * Paints the daily line at 4x4.
 *
 * The theme alone along the foot: shuffling for another line is something the app does,
 * and a widget that can't be tapped into has no business offering it.
 */
fun renderWisdomCardLarge(
    context: Context,
    widthPx: Int,
    heightPx: Int,
    style: WidgetStyle,
    photoUri: String?,
    quote: String,
    category: String
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]

    return fixedCard(context, widthPx, heightPx, style, photoUri) { canvas, s, w, h ->
        val padX = sideMargin(LARGE_PADDING, style.cornerRadius) * s
        val padY = LARGE_PADDING * s
        val left = padX
        val inner = (w - 2 * left).toInt()

        val meta = Paint(CardPaintFlags).apply {
            typeface = ink.face
            textSize = LARGE_META * s
            color = ink.onSurfaceMuted
            letterSpacing = 0.18f
        }
        val metaHeight = LARGE_META_LINE * s
        val metaTop = h - padY - metaHeight
        val footRule = metaTop - LARGE_SPACING * s - s

        var y = padY
        val eyebrow = textBlock(
            "DAILY WISDOM", ink.face, LARGE_EYEBROW * s, LARGE_EYEBROW_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )
        canvas.draw(eyebrow, left, y); y += eyebrow.height + LARGE_SPACING * s

        val lineHeight = LARGE_QUOTE_LINE * s
        val available = footRule - LARGE_SPACING * s - y
        val maxLines = (available / lineHeight).toInt().coerceAtLeast(1)
        val line = textBlock(
            align.format(quote), ink.face, LARGE_QUOTE * s, lineHeight, ink.onSurface, inner,
            style.alignId, maxLines = maxLines
        )
        canvas.draw(line, left, y)

        canvas.drawLine(
            left, footRule, w - left, footRule,
            Paint(CardPaintFlags).apply {
                color = ink.onSurfaceRule
                strokeWidth = s
            }
        )
        canvas.drawTextLine(category.uppercase(), left, metaTop, metaHeight, meta)
    }
}

// ── The same three, fixed at 2x2 ────────────────────────────────────────────────

/** Paints the vision card at 2x2: the name low against the edge, how long is left under it. */
fun renderVisionCardSmall(
    context: Context,
    sidePx: Int,
    style: WidgetStyle,
    photoUri: String?,
    goal: String?,
    weeksLine: String
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]

    return smallCard(context, sidePx, style, photoUri) { canvas, s, side ->
        val padX = sideMargin(SMALL_PADDING, style.cornerRadius) * s
        val padY = SMALL_PADDING * s
        val left = padX
        val inner = (side - 2 * left).toInt()

        val eyebrow = textBlock(
            "VISION", ink.face, SMALL_EYEBROW * s, SMALL_EYEBROW_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )
        canvas.draw(eyebrow, left, padY)

        val meta = Paint(CardPaintFlags).apply {
            typeface = ink.face
            textSize = SMALL_META * s
            color = ink.onSurfaceMuted
            letterSpacing = 0.18f
        }
        val metaHeight = SMALL_META_LINE * s
        val metaTop = side - padY - metaHeight
        canvas.drawTextLine(weeksLine, left, metaTop, metaHeight, meta)

        val ruleY = metaTop - SMALL_SPACING * s - s
        val rulePaint = Paint(CardPaintFlags).apply {
            color = ink.onSurfaceRule
            strokeWidth = s
        }
        canvas.drawLine(left, ruleY, side - left, ruleY, rulePaint)

        // Grows upward from the rule rather than down from the eyebrow: the title is
        // what the card is for, and it should meet the line under it whether it runs
        // to one line or two.
        val forTitle = ruleY - SMALL_SPACING * s - (padY + eyebrow.height + SMALL_SPACING * s)
        val titleLines = (forTitle / (SMALL_TITLE_LINE * s)).toInt().coerceIn(1, 2)
        val title = textBlock(
            align.format(goal ?: "No vision yet"),
            ink.face, SMALL_TITLE * s, SMALL_TITLE_LINE * s, ink.onSurface, inner,
            style.alignId, maxLines = titleLines
        )
        canvas.draw(title, left, ruleY - SMALL_SPACING * s - title.height)
    }
}

/** Paints today's three at 2x2: what's left to do, the count above it. */
fun renderTopThreeCardSmall(
    context: Context,
    sidePx: Int,
    style: WidgetStyle,
    photoUri: String?,
    tasks: List<String?>,
    checked: List<Boolean>
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]
    val set = tasks.indices.filter { tasks[it] != null }
    val done = set.count { checked[it] }
    val next = set.firstOrNull { !checked[it] }

    return smallCard(context, sidePx, style, photoUri) { canvas, s, side ->
        val padX = sideMargin(SMALL_PADDING, style.cornerRadius) * s
        val padY = SMALL_PADDING * s
        val left = padX
        val inner = (side - 2 * left).toInt()

        val eyebrow = textBlock(
            "TOP 3", ink.face, SMALL_EYEBROW * s, SMALL_EYEBROW_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )
        canvas.draw(eyebrow, left, padY)

        val nextText = when {
            next != null -> tasks[next].orEmpty().uppercase()
            set.isEmpty() -> "NOTHING SET YET"
            else -> "ALL DONE"
        }
        val nextLine = textBlock(
            align.format(nextText), ink.face, SMALL_BODY * s, SMALL_BODY_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.14f, maxLines = 1
        )
        val nextTop = side - padY - nextLine.height
        canvas.draw(nextLine, left, nextTop)

        val count = textBlock(
            "$done/${set.size}", ink.face, SMALL_COUNT * s, SMALL_COUNT_LINE * s,
            ink.onSurface, inner, style.alignId
        )
        canvas.draw(count, left, nextTop - SMALL_SPACING * s - count.height)
    }
}

/** Paints the daily line at 2x2: as many of its words as fit, the theme under them. */
fun renderWisdomCardSmall(
    context: Context,
    sidePx: Int,
    style: WidgetStyle,
    photoUri: String?,
    quote: String,
    category: String
): Bitmap {
    val ink = cardInk(context, style)
    val align = Alignments[style.alignId]

    return smallCard(context, sidePx, style, photoUri) { canvas, s, side ->
        val padX = sideMargin(SMALL_PADDING, style.cornerRadius) * s
        val padY = SMALL_PADDING * s
        val left = padX
        val inner = (side - 2 * left).toInt()

        val meta = textBlock(
            category.uppercase(), ink.face, SMALL_META * s, SMALL_META_LINE * s,
            ink.onSurfaceMuted, inner, style.alignId, 0.18f
        )
        val metaTop = side - padY - meta.height
        canvas.draw(meta, left, metaTop)

        val lineHeight = SMALL_QUOTE_LINE * s
        val available = metaTop - SMALL_SPACING * s - padY
        val maxLines = (available / lineHeight).toInt().coerceAtLeast(1)
        val line = textBlock(
            align.format(quote), ink.face, SMALL_QUOTE * s, lineHeight, ink.onSurface, inner,
            style.alignId, maxLines = maxLines
        )
        canvas.draw(line, left, padY)
    }
}
