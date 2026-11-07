package app.visionov.visionwidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import app.visionov.visionwidget.MainActivity
import app.visionov.visionwidget.ui.gallery.DesignRenderer
import app.visionov.visionwidget.ui.gallery.GalleryCatalog

/**
 * One widget per size, holding whatever its [Slot] says it holds.
 *
 * The launcher's picker lists providers, so this is what the user is offered there:
 * three sizes, and nothing about which design is in them. That choice belongs to the
 * setting screen the launcher opens next — see `widget/config/WidgetConfigActivity`.
 *
 * Fifty-six providers used to be listed instead, one per design, because the design was
 * carried by the class. It is carried by the widget's own state now, so two widgets of
 * the same size can show different things; see [Slot] for why that was the sticking
 * point.
 */

/**
 * How long a widget's own update will wait for a design that has never been drawn.
 *
 * Long enough for a design placed on its own to arrive as itself, and short enough to
 * leave room under the deadline a broadcast is answered within.
 */
private const val FIRST_PAINT_WAIT_MS = 6_000L

/**
 * Read on [MainActivity]'s intent to open straight onto Studio's Gallery tab — a Gallery
 * face is a face from there, so a tap on one goes back to it rather than to Today.
 */
const val OpenGalleryExtra = "open_gallery"

/** The cell a notice is drawn on — the app's own plum, so it reads as ours. */
private val NoticeFill = Color(0xFF4A3A52)
private val NoticeInk = Color(0xFFF2EDE6)

open class SlotWidget(private val size: WidgetSize) : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = appWidgetIdOf(context, id)

        // Its own answer if it has one. Failing that, the one the setting screen left
        // for whichever cell of this size came up next — see [offerSlot] for why the id
        // alone cannot be relied on. Claimed once and written down, so this cell has an
        // answer of its own from here on and the next one doesn't take the same.
        val held = appWidgetId?.let { readSlot(context, it) }
            ?: claimOfferedSlot(context, size)?.also { claimed ->
                appWidgetId?.let { writeSlot(context, it, claimed) }
                Log.i("VisionSlot", "widget=$appWidgetId claimed the offered ${claimed.encode()}")
            }

        Log.i("VisionSlot", "compose $size widget=$appWidgetId holding=${held?.encode()}")

        // Nothing here draws nothing. A widget that composes an empty box is
        // indistinguishable from one that was never asked to compose at all, from one
        // whose picture failed to paint, and from one that threw — and telling those
        // apart from a photograph of a home screen is most of the work of fixing any
        // of them. Each failure says which it is instead.
        try {
            when (val slot = held) {
                Slot.Vision -> {
                    val payload = visionPayload(context, size)
                    provideContent { VisionCardContent(payload, size) }
                }

                Slot.TopThree -> {
                    val payload = topThreePayload(context, size)
                    provideContent { TopThreeCardContent(payload, size) }
                }

                Slot.Wisdom -> {
                    val payload = wisdomPayload(context)
                    provideContent { WisdomCardContent(payload, size) }
                }

                is Slot.Design -> provideDesign(context, slot.designId)

                // Placed without ever being told what to hold. Tapping opens the app,
                // which is the only way back to a choice from here.
                null -> provideContent { Notice("Tap to set up", id = appWidgetId) }
            }
        } catch (failure: Throwable) {
            Log.e("VisionSlot", "compose $size widget=$appWidgetId failed", failure)
            provideContent { Notice(failure.javaClass.simpleName, id = appWidgetId) }
        }
    }

    /**
     * A plain word in the cell, for the states that have no picture to show.
     *
     * Deliberately legible rather than tasteful: every one of these is a fault or an
     * unfinished setup, and the point is that it can be read off the home screen.
     */
    @Composable
    private fun Notice(text: String, id: Int?) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(NoticeFill)
                .clickable(actionStartActivity<MainActivity>())
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (id == null) text else "$text\n#$id",
                style = TextStyle(color = ColorProvider(NoticeInk), fontSize = 12.sp)
            )
        }
    }

    /** A Gallery design, drawn by the same renderer the shelf uses. */
    private suspend fun GlanceAppWidget.provideDesign(context: Context, designId: String) {
        val design = GalleryCatalog.load(context).firstOrNull { it.id == designId }

        // Off the disk, which is immediate.
        val kept = design?.let { DesignRenderer.cached(context, it) }

        // Nothing kept yet means this design has not been drawn since it was installed,
        // so it is drawn now and waited for.
        //
        // Waited for rather than stood in for: a design frozen at a moment that has
        // already passed would mean a widget arriving on the wrong month and correcting
        // itself a second later. A widget shows the real design or it shows nothing yet.
        //
        // The wait has a limit all the same. An update is a broadcast with a deadline,
        // and a round of them all waiting without one is what queued every widget behind
        // the single view that paints until they ran out of time. Past the limit the
        // painting carries on without this update and asks for a redraw when it lands.
        val bitmap = kept ?: design?.let {
            if (GalleryTicker.paintWithin(context, it, FIRST_PAINT_WAIT_MS)) {
                DesignRenderer.cached(context, it)
            } else {
                null
            }
        }

        provideContent {
            // Read so the widget redraws when the cell changes, even though the picture
            // itself is fitted rather than measured against it.
            LocalSize.current

            if (bitmap == null) {
                // The design has not been drawn yet and could not be drawn in the time
                // this update had. Said out loud rather than left blank: the painting
                // still running will ask for the redraw that replaces this, and if it
                // never does, the cell says so instead of looking like a bug elsewhere.
                Notice(if (design == null) "Unknown design" else "Drawing…", id = null)
            } else {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = design?.name,
                    // Fitted rather than filled, so a design is only ever scaled — never
                    // stretched out of the proportion it was drawn at.
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        // A Gallery design is a face from the shelf, so a tap on one
                        // goes back there rather than to Today.
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(
                                    ActionParameters.Key<Boolean>(OpenGalleryExtra) to true
                                )
                            )
                        )
                )
            }
        }
    }
}

class SmallSlotWidget : SlotWidget(WidgetSize.Small)
class MediumSlotWidget : SlotWidget(WidgetSize.Medium)
class LargeSlotWidget : SlotWidget(WidgetSize.Large)

/**
 * The three, in one place.
 *
 * Anything asking "what of ours is on a home screen?" goes through here rather than
 * naming the classes itself — adding a fourth size (the lock screen, when it comes)
 * should be one line.
 */
object SlotWidgets {

    /** The widget that draws a cell of [size], for anything asking it to redraw. */
    fun of(size: WidgetSize): SlotWidget = when (size) {
        WidgetSize.Small -> SmallSlotWidget()
        WidgetSize.Medium -> MediumSlotWidget()
        WidgetSize.Large -> LargeSlotWidget()
    }

    /** Which size a launcher's widget id was placed as, or null if it has gone. */
    fun sizeOf(context: Context, appWidgetId: Int): WidgetSize? {
        val provider = runCatching {
            AppWidgetManager.getInstance(context)?.getAppWidgetInfo(appWidgetId)?.provider
        }.getOrNull() ?: return null

        return WidgetSize.entries.firstOrNull {
            provider == ComponentName(context, receiverOf(it))
        }
    }

    /**
     * Asks the system to redraw one widget, by the id the launcher knows it by.
     *
     * The ordinary broadcast rather than [GlanceAppWidget.update], because that needs a
     * GlanceId and the only way to one is `getGlanceIdBy`, which throws while the widget
     * is still being placed. Skipping the redraw there is what left "Tap to set up"
     * frozen on the cell: the launcher puts the widget up while its setting screen is
     * still open, so the first composition legitimately finds no choice yet, and those
     * are the views that stay until something else asks for new ones.
     *
     * This asks for new ones, and needs nothing but the Int the caller already has.
     */
    fun requestRedraw(context: Context, appWidgetId: Int, size: WidgetSize) {
        val intent = Intent(context, receiverOf(size))
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        runCatching { context.sendBroadcast(intent) }
            .onFailure { Log.w("VisionSlot", "redraw request for $appWidgetId failed", it) }
    }

    /** The receiver declared in the manifest for [size]. */
    fun receiverOf(size: WidgetSize): Class<*> = when (size) {
        WidgetSize.Small -> SmallSlotReceiver::class.java
        WidgetSize.Medium -> MediumSlotReceiver::class.java
        WidgetSize.Large -> LargeSlotReceiver::class.java
    }
}

/**
 * What every size's receiver is built on.
 *
 * The tick is booked when a design arrives on a screen and let go of when the last one
 * leaves, so nothing is being woken for a clock nobody has up. Which of the widgets
 * actually hold a design is read from their state rather than from the class now, so
 * the booking always looks — there are three classes to ask about rather than
 * forty-four, which is what the old `known` shortcut existed to avoid.
 */
abstract class SlotWidgetReceiver(
    override val glanceAppWidget: GlanceAppWidget
) : GlanceAppWidgetReceiver() {

    // Being here at all means a widget of this size exists, so the booking is told as
    // much rather than going to look. What it would look at is a record Glance writes
    // as part of this very update, and reading it mid-flight answers that the screen is
    // empty — which would stop the tick for the clock that just arrived.
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        GalleryTicker.schedule(context, known = true)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        GalleryTicker.schedule(context, known = appWidgetIds.isNotEmpty())
    }

    // What a widget held is kept against the launcher's id for it, so it has to be let
    // go of when the launcher says that id is gone. Left behind, the entries would
    // outlive every widget that ever used one — and an id the system hands out again
    // would arrive already holding something.
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { forgetSlot(context, it) }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        GalleryTicker.schedule(context)
    }
}

class SmallSlotReceiver : SlotWidgetReceiver(SmallSlotWidget())
class MediumSlotReceiver : SlotWidgetReceiver(MediumSlotWidget())
class LargeSlotReceiver : SlotWidgetReceiver(LargeSlotWidget())

/**
 * Redraws every dressed card on a home screen.
 *
 * Applying in Studio changes what these are meant to look like, and nothing else would
 * tell them — a widget is only asked to redraw on its own schedule otherwise, which is
 * half an hour away at best.
 *
 * Only the cards. A Gallery design is not dressed by Studio and has its own tick, so a
 * widget holding one is left alone rather than being made to repaint the same picture.
 */
suspend fun updateDesignWidgets(context: Context) {
    // One after another, not all at once. Running them together was quicker on paper
    // and is not worth it: they all reach for the same store Glance keeps its widget
    // state in, and a card that loses that race is a card left showing the old thing
    // with nothing said about it.
    placedSlots(context)
        .filter { it.slot !is Slot.Design }
        .forEach { placed ->
            // Redrawn by the widget of its own size — the content a widget composes is
            // the content for its cell, so the size has to match the one it was placed as.
            //
            // One that fails is one card left as it was, not the others abandoned — but
            // it is said out loud, because a card that quietly keeps the picture it had
            // looks exactly like one nobody asked to redraw.
            runCatching { SlotWidgets.of(placed.size).update(context, placed.id) }
                .onFailure { Log.w("VisionWidget", "card redraw failed", it) }
        }
}
