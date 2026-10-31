package com.example.visionwidget.widget

import android.content.Context
import android.util.DisplayMetrics
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.action.actionStartActivity
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.example.visionwidget.MainActivity
import com.example.visionwidget.data.AppDatabase
import com.example.visionwidget.data.AppPreferences
import com.example.visionwidget.data.buildDayRecords
import com.example.visionwidget.data.tallyBetween
import com.example.visionwidget.data.topThreeStats
import com.example.visionwidget.data.visibleSlotCount
import com.example.visionwidget.ui.home.Wisdom
import com.example.visionwidget.ui.home.wisdomFor
import com.example.visionwidget.ui.home.wisdomIndices
import com.example.visionwidget.ui.theme.WidgetTarget
import com.example.visionwidget.ui.vision.formatShortDate
import com.example.visionwidget.ui.vision.formatTargetDate
import com.example.visionwidget.ui.vision.formatWeeksLeft
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.sqrt

/**
 * The three cards Studio dresses, as home screen widgets.
 *
 * Each reads what was applied to it and the user's own words, paints the card, and
 * shows it as a single image. Studio's own preview is the same drawing at another size,
 * so what lands here is what was chosen there.
 *
 * None of the three act on a tap — Top 3 used to tick a row from the home screen, but a
 * fixed-size card has no room to show which row was hit, so every tap now just opens the
 * app instead. Only the card is painted; everything around it on Today — the section
 * labels, the counts — belongs to the screen rather than to the widget.
 */

/**
 * How large a card may be painted.
 *
 * A widget update caps what one widget may carry at six bytes per screen pixel. At the
 * four bytes a pixel ARGB_8888 costs, a card drawn pixel-for-pixel with its own cell
 * always sits well inside that — a cell can never be larger than the screen it is on —
 * so the ceiling is the screen's own pixel count rather than a fixed figure. The bitmap
 * itself travels as shared memory rather than inside the Binder transaction, so the
 * transaction's own limit is not what's being held under here.
 *
 * A fixed ceiling is what used to make the type look soft: on a dense screen a 4x2 came
 * out around 560px across and the launcher then stretched it back up to the cell's own
 * 900, an upscale of better than half again.
 */
private fun maxCardArea(metrics: DisplayMetrics) =
    metrics.widthPixels.toFloat() * metrics.heightPixels

/**
 * Drawn a little over the cell's reported size rather than exactly to it.
 *
 * Launchers differ in how much of the cell they keep back as padding, so a card measured
 * exactly can still land in a view slightly larger and be scaled up to meet it. Scaling
 * down costs nothing in sharpness; scaling up costs all of it.
 */
private const val CardOversample = 1.15f

/** Read on [MainActivity]'s intent to open straight onto the Vision tab. */
const val OpenVisionExtra = "open_vision"
private val OpenVisionParams = actionParametersOf(ActionParameters.Key<Boolean>(OpenVisionExtra) to true)

/** What a widget needs to paint itself, gathered before any of it is drawn. */
private suspend fun lookOf(context: Context, target: WidgetTarget) =
    AppPreferences(context).let { it.widgetStyle(target) to it.widgetPhotoUri(target) }

/**
 * The daily line as the app left it — the theme it settled on and, under that theme,
 * the line it settled on. Never a line picked here: shuffling belongs to the app, and
 * the card on the home screen is meant to be the same card.
 */
private fun settledWisdom(preferences: AppPreferences): Wisdom {
    val category = preferences.wisdomCategory
    val pool = wisdomIndices(category)
    val index = preferences.wisdomIndex.takeIf { it in pool } ?: pool.firstOrNull() ?: 0
    return wisdomFor(category, index, preferences.wisdomOwnText)
}

@Composable
private fun CardImage(bitmap: android.graphics.Bitmap, openVision: Boolean = false) {
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        // Fitted rather than filled, so a card is only ever scaled — never stretched
        // out of the proportion it was drawn at.
        contentScale = ContentScale.Fit,
        modifier = GlanceModifier.fillMaxSize().clickable(
            if (openVision) {
                actionStartActivity<MainActivity>(OpenVisionParams)
            } else {
                actionStartActivity<MainActivity>()
            }
        )
    )
}

/**
 * The cell the launcher handed over, in pixels, at the shape the launcher gave it.
 *
 * Both sides come down together or neither does. Capping width and height separately
 * is what made a 4x2 card square: on a dense screen the width met the ceiling and the
 * height never reached it, so the card was drawn to a shape its cell never had and
 * then fitted into the real one with wallpaper down either side.
 */
@Composable
private fun cardSizePx(): Pair<Int, Int> {
    val metrics = LocalContext.current.resources.displayMetrics
    val size = LocalSize.current
    val width = (size.width.value * metrics.density * CardOversample).coerceAtLeast(1f)
    val height = (size.height.value * metrics.density * CardOversample).coerceAtLeast(1f)

    val scale = minOf(1f, sqrt(maxCardArea(metrics) / (width * height)))
    return (width * scale).toInt().coerceAtLeast(1) to
        (height * scale).toInt().coerceAtLeast(1)
}

/** The 2x2 cell's own side — square, so the shorter of the two is the whole of it. */
@Composable
private fun cardSidePx(): Int {
    val (width, height) = cardSizePx()
    return minOf(width, height)
}

class VisionAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.Vision)
        val visions = AppDatabase.getInstance(context).visionDao()
            .observeVisionsWithMilestones().first()
        // The main one, the same one Today follows — it's what the widgets are bound to.
        val main = visions.firstOrNull { it.vision.isMain } ?: visions.firstOrNull()

        provideContent {
            val (widthPx, heightPx) = cardSizePx()
            CardImage(
                bitmap = renderVisionCard(
                    context = LocalContext.current,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    style = style,
                    photoUri = photo,
                    goal = main?.vision?.goal,
                    why = main?.vision?.why,
                    targetLine = main?.let { formatTargetDate(it.vision.targetDateMillis).uppercase() }.orEmpty(),
                    weeksLine = main?.let { formatWeeksLeft(it.vision.targetDateMillis) }.orEmpty()
                ),
                openVision = true
            )
        }
    }
}

class WisdomAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.Wisdom)
        val preferences = AppPreferences(context)

        // The line the app settled on, not one picked here. Shuffling is a thing the
        // user does, and the card on the home screen is the same card.
        val wisdom = settledWisdom(preferences)

        provideContent {
            val (widthPx, heightPx) = cardSizePx()
            CardImage(
                renderWisdomCard(
                    context = LocalContext.current,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    style = style,
                    photoUri = photo,
                    quote = wisdom.text,
                    category = wisdom.category
                )
            )
        }
    }
}

/** Today's three. A tap opens Today rather than ticking a row — see the file doc. */
class TopThreeAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.TopThree)
        val slots = AppDatabase.getInstance(context).ruleOfThreeDao().observeSlots().first()
        val slotCount = visibleSlotCount(slots)
        val tasks = List(slotCount) { index -> slots.find { it.slotIndex == index }?.task }
        val checked = List(slotCount) { index -> slots.find { it.slotIndex == index }?.checked == true }

        provideContent {
            val (widthPx, heightPx) = cardSizePx()
            CardImage(
                renderTopThreeCard(
                    context = LocalContext.current,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    style = style,
                    photoUri = photo,
                    tasks = tasks,
                    checked = checked
                )
            )
        }
    }
}

/**
 * The same three, at a fixed 4x4.
 *
 * Twice the height of the 4x2, which is where the things the wide card has to leave out
 * go: the steps toward a vision, and a foot reading how the week has gone.
 */
class VisionCardLargeAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.Vision)
        val visions = AppDatabase.getInstance(context).visionDao()
            .observeVisionsWithMilestones().first()
        val main = visions.firstOrNull { it.vision.isMain } ?: visions.firstOrNull()
        val milestones = main?.milestones.orEmpty().map {
            MilestoneLine(
                step = it.step,
                checked = it.checked,
                due = formatShortDate(it.dueDateMillis).uppercase()
            )
        }

        provideContent {
            val (widthPx, heightPx) = cardSizePx()
            CardImage(
                bitmap = renderVisionCardLarge(
                    context = LocalContext.current,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    style = style,
                    photoUri = photo,
                    goal = main?.vision?.goal,
                    why = main?.vision?.why,
                    milestones = milestones,
                    targetLine = main?.let { formatTargetDate(it.vision.targetDateMillis).uppercase() }.orEmpty(),
                    weeksLine = main?.let { formatWeeksLeft(it.vision.targetDateMillis) }.orEmpty()
                ),
                openVision = true
            )
        }
    }
}

class WisdomLargeAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.Wisdom)
        val preferences = AppPreferences(context)
        val wisdom = settledWisdom(preferences)

        provideContent {
            val (widthPx, heightPx) = cardSizePx()
            CardImage(
                renderWisdomCardLarge(
                    context = LocalContext.current,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    style = style,
                    photoUri = photo,
                    quote = wisdom.text,
                    category = wisdom.category
                )
            )
        }
    }
}

class TopThreeLargeAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.TopThree)
        val dao = AppDatabase.getInstance(context).ruleOfThreeDao()
        val slots = dao.observeSlots().first()
        val slotCount = visibleSlotCount(slots)
        val tasks = List(slotCount) { index -> slots.find { it.slotIndex == index }?.task }
        val checked = List(slotCount) { index -> slots.find { it.slotIndex == index }?.checked == true }

        // The foot reads the same figures Today's own header does, built from the same
        // records the Insights tab reads rather than counted a second way here.
        val today = LocalDate.now()
        val records = buildDayRecords(dao.observeHistory().first(), slots, dao.liveDate())
        val streak = topThreeStats(records, today.toEpochDay()).streak
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val week = tallyBetween(records, weekStart.toEpochDay(), weekStart.plusDays(6).toEpochDay())

        provideContent {
            val (widthPx, heightPx) = cardSizePx()
            CardImage(
                renderTopThreeCardLarge(
                    context = LocalContext.current,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    style = style,
                    photoUri = photo,
                    tasks = tasks,
                    checked = checked,
                    streakLine = "STREAK $streak ${if (streak == 1) "DAY" else "DAYS"}",
                    weekLine = "THIS WEEK ${week.done} / ${week.total}"
                )
            )
        }
    }
}

/**
 * The same three, at a fixed 2x2 instead of 4x2.
 *
 * Studio's own preview only ever shows the 4x2 — there's no square slot on the phone
 * mockup to put these in — but they're their own widgets all the same, offered wherever
 * the launcher's own picker lists one.
 */
class VisionCardSmallAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.Vision)
        val visions = AppDatabase.getInstance(context).visionDao()
            .observeVisionsWithMilestones().first()
        val main = visions.firstOrNull { it.vision.isMain } ?: visions.firstOrNull()

        provideContent {
            CardImage(
                bitmap = renderVisionCardSmall(
                    context = LocalContext.current,
                    sidePx = cardSidePx(),
                    style = style,
                    photoUri = photo,
                    goal = main?.vision?.goal,
                    weeksLine = main?.let { formatWeeksLeft(it.vision.targetDateMillis) }.orEmpty()
                ),
                openVision = true
            )
        }
    }
}

class WisdomSmallAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.Wisdom)
        val preferences = AppPreferences(context)
        val wisdom = settledWisdom(preferences)

        provideContent {
            CardImage(
                renderWisdomCardSmall(
                    context = LocalContext.current,
                    sidePx = cardSidePx(),
                    style = style,
                    photoUri = photo,
                    quote = wisdom.text,
                    category = wisdom.category
                )
            )
        }
    }
}

class TopThreeSmallAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.TopThree)
        val slots = AppDatabase.getInstance(context).ruleOfThreeDao().observeSlots().first()
        val slotCount = visibleSlotCount(slots)
        val tasks = List(slotCount) { index -> slots.find { it.slotIndex == index }?.task }
        val checked = List(slotCount) { index -> slots.find { it.slotIndex == index }?.checked == true }

        provideContent {
            CardImage(
                renderTopThreeCardSmall(
                    context = LocalContext.current,
                    sidePx = cardSidePx(),
                    style = style,
                    photoUri = photo,
                    tasks = tasks,
                    checked = checked
                )
            )
        }
    }
}

class VisionCardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VisionAppWidget()
}

class TopThreeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopThreeAppWidget()
}

class WisdomWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WisdomAppWidget()
}

class VisionCardLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VisionCardLargeAppWidget()
}

class TopThreeLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopThreeLargeAppWidget()
}

class WisdomLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WisdomLargeAppWidget()
}

class VisionCardSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VisionCardSmallAppWidget()
}

class TopThreeSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopThreeSmallAppWidget()
}

class WisdomSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WisdomSmallAppWidget()
}

/**
 * Redraws every dressed card.
 *
 * Applying in Studio changes what these are meant to look like, and nothing else would
 * tell them — a widget is only asked to redraw on its own schedule otherwise, which is
 * half an hour away at best.
 */
suspend fun updateDesignWidgets(context: Context) {
    VisionAppWidget().updateAll(context)
    TopThreeAppWidget().updateAll(context)
    WisdomAppWidget().updateAll(context)
    VisionCardSmallAppWidget().updateAll(context)
    TopThreeSmallAppWidget().updateAll(context)
    WisdomSmallAppWidget().updateAll(context)
    VisionCardLargeAppWidget().updateAll(context)
    TopThreeLargeAppWidget().updateAll(context)
    WisdomLargeAppWidget().updateAll(context)
}
