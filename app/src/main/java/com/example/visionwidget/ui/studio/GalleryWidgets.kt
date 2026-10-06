package com.example.visionwidget.ui.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.visionwidget.R
import com.example.visionwidget.ui.theme.DMSans
import com.example.visionwidget.ui.theme.InstrumentSerif
import com.example.visionwidget.widget.renderBlushCalendar
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ready-made faces for the Gallery.
 *
 * These stand apart from the widgets Studio dresses: they carry nothing of the user's
 * own — no vision, no tasks, no quote of theirs — and only ever show the date. So they
 * take no skin, no font and no theme, and are drawn exactly as their design states.
 *
 * Every measurement is given at the size the designs were drawn at and scaled from
 * there, so one composable serves a gallery thumbnail and a full-size preview alike
 * rather than needing a second set of numbers for each.
 */
private const val DESIGN_SIZE = 320f

/** Short weekday and date, as the designs set them: "SUN" over "OCT 04". */
private val WeekdayFormat = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val DateFormat = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH)

/**
 * The wallpaper the designs were drawn against. A glass panel is only ever as readable
 * as what sits behind it, so the face carries its own backdrop rather than borrowing
 * whatever the gallery happens to be laid on.
 */
private val GalleryBackdrop = Color(0xFF706D6F)

/** Ticks once a minute would leave the minute hand visibly behind; this is close enough. */
private const val ClockTickMillis = 20_000L

@Composable
private fun rememberNow(): LocalDateTime {
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(ClockTickMillis)
        }
    }
    return now
}

/**
 * A glass panel carrying the date, the hour as a figure, and an analogue face.
 *
 * Nothing here reads from the app — it's a clock, and the only thing it knows is the
 * time it's being drawn at.
 */
@Composable
fun ClockWidget(size: Dp, modifier: Modifier = Modifier) {
    val scale = size.value / DESIGN_SIZE
    fun d(px: Float) = (px * scale).dp
    fun f(px: Float) = (px * scale).sp

    val now = rememberNow()
    val ink = Color.White.copy(alpha = 0.94f)
    val shape = RoundedCornerShape(d(32f))

    Box(
        modifier
            .size(size)
            .clip(shape)
            // Three layers, the way the design states them: the wallpaper, the panel's
            // own translucent fill over it, and a sheen across the top of both.
            .background(GalleryBackdrop)
            .background(Color(0xFF7E7A7C).copy(alpha = 0.58f))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .border(d(1f), Color.White.copy(alpha = 0.28f), shape)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = d(29f), y = d(29f)),
            verticalArrangement = Arrangement.spacedBy(d(2f))
        ) {
            val dateStyle = TextStyle(
                fontFamily = DMSans,
                fontWeight = FontWeight.Medium,
                fontSize = f(12f),
                lineHeight = f(13.8f),
                letterSpacing = 0.08.em,
                color = ink
            )
            Text(text = now.format(WeekdayFormat).uppercase(), style = dateStyle)
            Text(text = now.format(DateFormat).uppercase(), style = dateStyle)
        }

        Text(
            // Twelve-hour, and twelve rather than zero at midnight.
            text = (now.hour % 12).let { if (it == 0) 12 else it }.toString(),
            style = TextStyle(
                fontFamily = InstrumentSerif,
                fontSize = f(25f),
                lineHeight = f(25f),
                color = ink
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = -d(31f), y = d(27f))
        )

        ClockFace(
            now = now,
            ink = ink,
            scale = scale,
            // Set a touch above the middle, so the caption below it has room without
            // the face looking pushed off centre.
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = d(65f), y = d(70.7f))
        )

        Text(
            text = "GOOD THINGS\nTAKE TIME",
            style = TextStyle(
                fontFamily = DMSans,
                fontSize = f(7f),
                lineHeight = f(11.9f),
                letterSpacing = 0.35.em,
                color = Color.White.copy(alpha = 0.72f)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -d(30f))
        )
    }
}

/** The dial: a hairline ring, four figures, eight ticks and two hands. */
@Composable
private fun ClockFace(
    now: LocalDateTime,
    ink: Color,
    scale: Float,
    modifier: Modifier = Modifier
) {
    fun d(px: Float) = (px * scale).dp
    fun f(px: Float) = (px * scale).sp

    val markerStyle = TextStyle(
        fontFamily = InstrumentSerif,
        fontSize = f(18f),
        lineHeight = f(18f),
        color = ink
    )

    Box(modifier.size(d(190f))) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)

            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = this.size.minDimension / 2f - d(11f).toPx(),
                style = Stroke(width = d(1f).toPx())
            )

            // The hours without a figure of their own. Measured from three o'clock and
            // running clockwise, the way the design's own rotations are given.
            val tickInner = d(76f).toPx()
            val tickOuter = d(88f).toPx()
            listOf(30f, 60f, 120f, 150f, 210f, 240f, 300f, 330f).forEach { degrees ->
                val radians = Math.toRadians(degrees.toDouble())
                val dx = cos(radians).toFloat()
                val dy = sin(radians).toFloat()
                drawLine(
                    color = Color.White.copy(alpha = 0.34f),
                    start = centre + Offset(dx * tickInner, dy * tickInner),
                    end = centre + Offset(dx * tickOuter, dy * tickOuter),
                    strokeWidth = d(1f).toPx()
                )
            }

            // Both hands are measured from twelve, clockwise. The hour hand carries the
            // minutes and the minute hand the seconds, so neither ever jumps.
            fun hand(degrees: Float, length: Float, width: Float) {
                val radians = Math.toRadians(degrees.toDouble())
                drawLine(
                    color = ink,
                    start = centre,
                    end = centre + Offset(
                        sin(radians).toFloat() * length,
                        -cos(radians).toFloat() * length
                    ),
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }

            val minutes = now.minute + now.second / 60f
            val hours = (now.hour % 12) + minutes / 60f
            hand(hours * 30f, d(55f).toPx(), d(3f).toPx())
            hand(minutes * 6f, d(78f).toPx(), d(2f).toPx())

            drawCircle(
                color = Color.White.copy(alpha = 0.96f),
                radius = d(4f).toPx(),
                center = centre
            )
        }

        Text("12", style = markerStyle, modifier = Modifier.align(Alignment.TopCenter).offset(y = d(4f)))
        Text("3", style = markerStyle, modifier = Modifier.align(Alignment.CenterEnd))
        Text("6", style = markerStyle, modifier = Modifier.align(Alignment.BottomCenter).offset(y = -d(3f)))
        Text("9", style = markerStyle, modifier = Modifier.align(Alignment.CenterStart))
    }
}

/**
 * A photograph washed out from the left so a line of type can sit on it, with the date
 * above and a rule under the words.
 */
@Composable
fun QuoteWidget(size: Dp, modifier: Modifier = Modifier) {
    val scale = size.value / DESIGN_SIZE
    fun d(px: Float) = (px * scale).dp
    fun f(px: Float) = (px * scale).sp

    val now = rememberNow()
    val ink = Color(0xFF433A37)
    val wash = Color(0xFFFAF6EF)

    // Pulled down a little and brightened, so the photograph sits back from the words
    // rather than competing with them.
    val photoFilter = remember {
        val matrix = ColorMatrix().apply { setToSaturation(0.72f) }
        matrix.timesAssign(
            ColorMatrix(
                floatArrayOf(
                    1.06f, 0f, 0f, 0f, 0f,
                    0f, 1.06f, 0f, 0f, 0f,
                    0f, 0f, 1.06f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        ColorFilter.colorMatrix(matrix)
    }

    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(d(31f)))
            .background(Color(0xFFF5F0E9))
    ) {
        Image(
            painter = painterResource(R.drawable.gallery_quote_photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // Held off-centre to the right, which is where the design crops it.
            alignment = BiasAlignment(horizontalBias = 0.4f, verticalBias = 0f),
            colorFilter = photoFilter,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to wash.copy(alpha = 0.98f),
                        0.28f to wash.copy(alpha = 0.90f),
                        0.53f to wash.copy(alpha = 0.40f),
                        1f to wash.copy(alpha = 0.05f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = d(29f), y = d(28f)),
            verticalArrangement = Arrangement.spacedBy(d(3f))
        ) {
            val dateStyle = TextStyle(
                fontFamily = InstrumentSerif,
                fontSize = f(12f),
                lineHeight = f(13.8f),
                letterSpacing = 0.07.em,
                color = Color(0xFF776E6A)
            )
            Text(text = now.format(WeekdayFormat).uppercase(), style = dateStyle)
            Text(text = now.format(DateFormat).uppercase(), style = dateStyle)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = d(29f), y = -d(31f))
        ) {
            Text(
                text = "A more\nintentional\nme.",
                style = TextStyle(
                    fontFamily = InstrumentSerif,
                    fontSize = f(29f),
                    lineHeight = f(34.2f),
                    letterSpacing = (-0.025).em,
                    color = ink
                )
            )
            Spacer(Modifier.height(d(18f)))
            Box(
                Modifier
                    .width(d(31f))
                    .height(d(2f))
                    .clip(CircleShape)
                    .background(ink.copy(alpha = 0.75f))
            )
        }
    }
}

/**
 * The blush calendar, drawn by the very renderer the widget uses.
 *
 * Painted rather than composed: the widget has to paint it anyway, and showing the same
 * bitmap here means the shelf can't drift from what actually lands on the home screen.
 */
@Composable
fun BlushCalendarFace(size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val today = rememberNow().toLocalDate()
    val widthPx = with(density) { size.roundToPx() }

    val bitmap = remember(widthPx, today) {
        renderBlushCalendar(context, widthPx, today).asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxWidth()
    )
}
