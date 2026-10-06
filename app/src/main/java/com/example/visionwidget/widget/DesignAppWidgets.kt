package com.example.visionwidget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.action.actionStartActivity
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.compose.ui.unit.dp
import com.example.visionwidget.MainActivity
import com.example.visionwidget.data.AppDatabase
import com.example.visionwidget.data.AppPreferences
import com.example.visionwidget.data.RuleOfThreeSlotEntity
import com.example.visionwidget.ui.home.WISDOM
import com.example.visionwidget.ui.home.wisdomIndices
import com.example.visionwidget.ui.theme.WidgetTarget
import com.example.visionwidget.ui.vision.formatTargetDate
import com.example.visionwidget.ui.vision.formatWeeksLeft
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * The three cards Studio dresses, as home screen widgets.
 *
 * Each reads what was applied to it and the user's own words, paints the card, and
 * shows it as a single image. Studio's own preview is the same drawing at another size,
 * so what lands here is what was chosen there.
 *
 * Only the card is painted. Everything around it on Today — the section labels, the
 * counts — belongs to the screen rather than to the widget.
 */

/** The same ceiling the Gallery faces keep, for the same Binder reason. */
private const val MaxCardPx = 640

/** What a widget needs to paint itself, gathered before any of it is drawn. */
private suspend fun lookOf(context: Context, target: WidgetTarget) =
    AppPreferences(context).let { it.widgetStyle(target) to it.widgetPhotoUri(target) }

@Composable
private fun CardImage(bitmap: android.graphics.Bitmap) {
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = null,
        // Fitted rather than filled, so a card is only ever scaled — never stretched
        // out of the proportion it was drawn at.
        contentScale = ContentScale.Fit,
        modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>())
    )
}

/** The width to paint at, from whatever cell the launcher handed over. */
@Composable
private fun cardWidthPx(): Int {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    return (LocalSize.current.width.value * density).toInt().coerceIn(1, MaxCardPx)
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
            CardImage(
                renderVisionCard(
                    context = LocalContext.current,
                    widthPx = cardWidthPx(),
                    style = style,
                    photoUri = photo,
                    goal = main?.vision?.goal,
                    milestones = main?.milestones.orEmpty().map { it.step to it.checked },
                    targetLine = main?.let { formatTargetDate(it.vision.targetDateMillis).uppercase() }.orEmpty(),
                    weeksLine = main?.let { formatWeeksLeft(it.vision.targetDateMillis) }.orEmpty()
                )
            )
        }
    }
}

class WisdomAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.Wisdom)
        val category = AppPreferences(context).wisdomCategory

        // One line a day, settled by the date rather than picked afresh on every
        // update — a widget that changed its quote whenever the launcher asked would
        // be a shuffle nobody tapped.
        val pool = wisdomIndices(category)
        val wisdom = WISDOM[pool[(LocalDate.now().toEpochDay() % pool.size).toInt()]]

        provideContent {
            CardImage(
                renderWisdomCard(
                    context = LocalContext.current,
                    widthPx = cardWidthPx(),
                    style = style,
                    photoUri = photo,
                    quote = wisdom.text,
                    category = wisdom.category
                )
            )
        }
    }
}

/**
 * Today's three, with each row tickable.
 *
 * The card is a bitmap, and a bitmap can't be acted on. A transparent target is laid
 * over each row instead, placed from the fractions the renderer handed back — which
 * keeps the design exactly as drawn and still lets a task be kept from the home screen.
 */
class TopThreeAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (style, photo) = lookOf(context, WidgetTarget.TopThree)
        val slots = AppDatabase.getInstance(context).ruleOfThreeDao().observeSlots().first()
        val tasks = List(3) { index -> slots.find { it.slotIndex == index }?.task }
        val checked = List(3) { index -> slots.find { it.slotIndex == index }?.checked == true }
        val set = tasks.indices.filter { tasks[it] != null }

        provideContent {
            val widthPx = cardWidthPx()
            val rendered = renderTopThreeCard(
                context = LocalContext.current,
                widthPx = widthPx,
                style = style,
                photoUri = photo,
                tasks = tasks,
                checked = checked
            )
            // The image is fitted, so the rows sit inside however tall the drawing ends
            // up rather than inside the cell the launcher gave.
            val drawnHeight = LocalSize.current.width * (rendered.bitmap.height.toFloat() / widthPx)

            Box(GlanceModifier.fillMaxSize()) {
                CardImage(rendered.bitmap)
                Column(GlanceModifier.fillMaxWidth()) {
                    var previous = 0f
                    rendered.rows.forEachIndexed { position, bounds ->
                        Spacer(
                            GlanceModifier.height(drawnHeight * (bounds.start - previous))
                        )
                        Box(
                            GlanceModifier
                                .fillMaxWidth()
                                .height(drawnHeight * (bounds.endInclusive - bounds.start))
                                .clickable(
                                    actionRunCallback<ToggleTaskAction>(
                                        actionParametersOf(ToggleTaskAction.SlotKey to set[position])
                                    )
                                )
                        ) {}
                        previous = bounds.endInclusive
                    }
                }
            }
        }
    }
}

/** Keeps a task from the home screen, and redraws the card that was tapped. */
class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val slot = parameters[SlotKey] ?: return
        val dao = AppDatabase.getInstance(context).ruleOfThreeDao()
        val current = dao.observeSlots().first().find { it.slotIndex == slot } ?: return
        val task = current.task ?: return
        dao.upsert(RuleOfThreeSlotEntity(slot, task, !current.checked))
        TopThreeAppWidget().updateAll(context)
    }

    companion object {
        val SlotKey = ActionParameters.Key<Int>("slotIndex")
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
}
