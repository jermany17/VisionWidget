package app.visionov.visionwidget.widget

import android.content.Context
import android.util.DisplayMetrics
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import app.visionov.visionwidget.MainActivity
import app.visionov.visionwidget.data.AppDatabase
import app.visionov.visionwidget.data.AppPreferences
import app.visionov.visionwidget.data.buildDayRecords
import app.visionov.visionwidget.data.tallyBetween
import app.visionov.visionwidget.data.topThreeStats
import app.visionov.visionwidget.data.AFTER_THREE_SLOTS
import app.visionov.visionwidget.data.TOP_THREE_SLOTS
import app.visionov.visionwidget.data.checkedAt
import app.visionov.visionwidget.data.rowOrder
import app.visionov.visionwidget.data.taskAt
import app.visionov.visionwidget.data.topThreeLeads
import app.visionov.visionwidget.ui.home.Wisdom
import app.visionov.visionwidget.ui.home.wisdomFor
import app.visionov.visionwidget.ui.home.wisdomIndices
import app.visionov.visionwidget.ui.theme.WidgetStyle
import app.visionov.visionwidget.ui.theme.WidgetTarget
import app.visionov.visionwidget.ui.vision.formatShortDate
import app.visionov.visionwidget.ui.vision.formatTargetDate
import app.visionov.visionwidget.ui.vision.formatWeeksLeft
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.sqrt

/**
 * The three cards Studio dresses, as they are painted into a widget.
 *
 * Each reads what was applied to it and the user's own words, paints the card, and
 * shows it as a single image. Studio's own preview is the same drawing at another size,
 * so what lands here is what was chosen there.
 *
 * None of the three act on a tap — Top 3 used to tick a row from the home screen, but a
 * fixed-size card has no room to show which row was hit, so every tap now just opens the
 * app instead. Only the card is painted; everything around it on Today — the section
 * labels, the counts — belongs to the screen rather than to the widget.
 *
 * There are no widget classes here any more. A card is one of the things a [Slot] can
 * hold, and [SlotWidget] is what holds it — so what a card needs is gathered by a
 * payload function and drawn by a content composable, both taking the [WidgetSize] of
 * the cell they are headed for. That is what used to be nine near-identical classes.
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
private fun lookOf(context: Context, target: WidgetTarget) =
    AppPreferences(context).let { it.widgetStyle(target) to it.widgetPhotoUri(target) }

/**
 * The daily line as the app left it — the theme it settled on and, under that theme,
 * the line it settled on. The card on the home screen is meant to be the same card.
 *
 * The one line picked here is the day's own. A home screen can go a week without the
 * app being opened, and the line is meant to turn with the date rather than with the
 * next visit, so whichever of the two wakes first does the turning and the day stamp
 * keeps the other from doing it again.
 */
private fun settledWisdom(preferences: AppPreferences): Wisdom {
    preferences.rollWisdomForDay(LocalDate.now().toEpochDay())
    val category = preferences.wisdomCategory
    val pool = wisdomIndices(category)
    val index = preferences.wisdomIndex.takeIf { it in pool } ?: pool.firstOrNull() ?: 0
    return wisdomFor(category, index, preferences.wisdomOwnText)
}

@Composable
internal fun CardImage(bitmap: android.graphics.Bitmap, openVision: Boolean = false) {
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

// ---------------------------------------------------------------- Vision

/** The main vision's words, read once before the card is drawn. */
internal class VisionPayload(
    val style: WidgetStyle,
    val photo: String?,
    val goal: String?,
    val why: String?,
    val targetLine: String,
    val weeksLine: String,
    val milestones: List<MilestoneLine>
)

internal suspend fun visionPayload(context: Context, size: WidgetSize): VisionPayload {
    val (style, photo) = lookOf(context, WidgetTarget.Vision)
    val visions = AppDatabase.getInstance(context).visionDao().visionsWithMilestones()
    // The main one, the same one Today follows — it's what the widgets are bound to.
    val main = visions.firstOrNull { it.vision.isMain } ?: visions.firstOrNull()

    return VisionPayload(
        style = style,
        photo = photo,
        goal = main?.vision?.goal,
        why = main?.vision?.why,
        targetLine = main?.let { formatTargetDate(it.vision.targetDateMillis).uppercase() }.orEmpty(),
        weeksLine = main?.let { formatWeeksLeft(it.vision.targetDateMillis) }.orEmpty(),
        // Only the tall card has anywhere to put the steps, so only it pays to build them.
        milestones = if (size != WidgetSize.Large) emptyList() else {
            main?.milestones.orEmpty().map {
                MilestoneLine(
                    step = it.step,
                    checked = it.checked,
                    due = formatShortDate(it.dueDateMillis).uppercase()
                )
            }
        }
    )
}

@Composable
internal fun VisionCardContent(payload: VisionPayload, size: WidgetSize) {
    val context = LocalContext.current
    val bitmap = when (size) {
        WidgetSize.Small -> renderVisionCardSmall(
            context = context,
            sidePx = cardSidePx(),
            style = payload.style,
            photoUri = payload.photo,
            goal = payload.goal,
            weeksLine = payload.weeksLine
        )

        WidgetSize.Medium -> {
            val (widthPx, heightPx) = cardSizePx()
            renderVisionCard(
                context = context,
                widthPx = widthPx,
                heightPx = heightPx,
                style = payload.style,
                photoUri = payload.photo,
                goal = payload.goal,
                why = payload.why,
                targetLine = payload.targetLine,
                weeksLine = payload.weeksLine
            )
        }

        WidgetSize.Large -> {
            val (widthPx, heightPx) = cardSizePx()
            renderVisionCardLarge(
                context = context,
                widthPx = widthPx,
                heightPx = heightPx,
                style = payload.style,
                photoUri = payload.photo,
                goal = payload.goal,
                why = payload.why,
                milestones = payload.milestones,
                targetLine = payload.targetLine,
                weeksLine = payload.weeksLine
            )
        }
    }
    CardImage(bitmap, openVision = true)
}

// ---------------------------------------------------------------- Top 3

internal class TopThreePayload(
    val style: WidgetStyle,
    val photo: String?,
    val tasks: List<String?>,
    val checked: List<Boolean>,
    /** Only the tall card has a foot; null for the other two. */
    val streakLine: String?,
    val weekLine: String?
)

internal suspend fun topThreePayload(context: Context, size: WidgetSize): TopThreePayload {
    val (style, photo) = lookOf(context, WidgetTarget.TopThree)
    val dao = AppDatabase.getInstance(context).ruleOfThreeDao()
    val slots = dao.slots()
    // Whichever set is in front, which is the one with work left in it. A widget is
    // read at a glance and has room for three, so it shows what is being got on with
    // rather than the half of the day that happens to be written first.
    val set = if (slots.topThreeLeads()) TOP_THREE_SLOTS else AFTER_THREE_SLOTS
    val order = slots.rowOrder(set)

    // The foot reads the same figures Today's own header does, built from the same
    // records the Insights tab reads rather than counted a second way here. Only the
    // tall card shows them, and the history read is the dearest thing on this path.
    var streakLine: String? = null
    var weekLine: String? = null
    if (size == WidgetSize.Large) {
        val today = LocalDate.now()
        val records = buildDayRecords(dao.observeHistory().first(), slots, dao.liveDate())
        val streak = topThreeStats(records, today.toEpochDay()).streak
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val week = tallyBetween(records, weekStart.toEpochDay(), weekStart.plusDays(6).toEpochDay())
        streakLine = "STREAK $streak ${if (streak == 1) "DAY" else "DAYS"}"
        weekLine = "THIS WEEK ${week.done} / ${week.total}"
    }

    return TopThreePayload(
        style = style,
        photo = photo,
        tasks = order.map { slots.taskAt(it) },
        checked = order.map { slots.checkedAt(it) },
        streakLine = streakLine,
        weekLine = weekLine
    )
}

@Composable
internal fun TopThreeCardContent(payload: TopThreePayload, size: WidgetSize) {
    val context = LocalContext.current
    val bitmap = when (size) {
        WidgetSize.Small -> renderTopThreeCardSmall(
            context = context,
            sidePx = cardSidePx(),
            style = payload.style,
            photoUri = payload.photo,
            tasks = payload.tasks,
            checked = payload.checked
        )

        WidgetSize.Medium -> {
            val (widthPx, heightPx) = cardSizePx()
            renderTopThreeCard(
                context = context,
                widthPx = widthPx,
                heightPx = heightPx,
                style = payload.style,
                photoUri = payload.photo,
                tasks = payload.tasks,
                checked = payload.checked
            )
        }

        WidgetSize.Large -> {
            val (widthPx, heightPx) = cardSizePx()
            renderTopThreeCardLarge(
                context = context,
                widthPx = widthPx,
                heightPx = heightPx,
                style = payload.style,
                photoUri = payload.photo,
                tasks = payload.tasks,
                checked = payload.checked,
                streakLine = payload.streakLine.orEmpty(),
                weekLine = payload.weekLine.orEmpty()
            )
        }
    }
    CardImage(bitmap)
}

// ---------------------------------------------------------------- Wisdom

internal class WisdomPayload(
    val style: WidgetStyle,
    val photo: String?,
    val quote: String,
    val category: String
)

internal suspend fun wisdomPayload(context: Context): WisdomPayload {
    val (style, photo) = lookOf(context, WidgetTarget.Wisdom)
    // The line the app settled on, not one picked here. Shuffling is a thing the user
    // does, and the card on the home screen is the same card.
    val wisdom = settledWisdom(AppPreferences(context))
    return WisdomPayload(style, photo, wisdom.text, wisdom.category)
}

@Composable
internal fun WisdomCardContent(payload: WisdomPayload, size: WidgetSize) {
    val context = LocalContext.current
    val bitmap = when (size) {
        WidgetSize.Small -> renderWisdomCardSmall(
            context = context,
            sidePx = cardSidePx(),
            style = payload.style,
            photoUri = payload.photo,
            quote = payload.quote,
            category = payload.category
        )

        WidgetSize.Medium -> {
            val (widthPx, heightPx) = cardSizePx()
            renderWisdomCard(
                context = context,
                widthPx = widthPx,
                heightPx = heightPx,
                style = payload.style,
                photoUri = payload.photo,
                quote = payload.quote,
                category = payload.category
            )
        }

        WidgetSize.Large -> {
            val (widthPx, heightPx) = cardSizePx()
            renderWisdomCardLarge(
                context = context,
                widthPx = widthPx,
                heightPx = heightPx,
                style = payload.style,
                photoUri = payload.photo,
                quote = payload.quote,
                category = payload.category
            )
        }
    }
    CardImage(bitmap)
}
