package app.visionov.visionwidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.visionov.visionwidget.MainActivity
import app.visionov.visionwidget.ui.gallery.DesignRenderer
import app.visionov.visionwidget.ui.gallery.GalleryCatalog
import app.visionov.visionwidget.widget.config.WidgetConfigActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * One widget per size, holding whatever its [Slot] says it holds.
 *
 * The launcher's picker lists providers, so this is what the user is offered there:
 * three sizes, and nothing about which design is in them. That choice belongs to the
 * setting screen the launcher opens next — see [WidgetConfigActivity].
 *
 * Everything a cell draws is read from inside the composition and nothing before it.
 * That is not a style: Glance keeps a session alive for about forty-five seconds past
 * a widget's first draw, and a redraw asked for in that window recomposes the tree it
 * already has rather than running [SlotWidget.provideGlance] again. Anything read on
 * the way in is therefore read once and kept — which is what used to leave a cell
 * showing "Tap to set up" after a design had been chosen for it, and a clock showing
 * the minute it was placed on. See [SlotKey].
 */

/**
 * How long a cell waits for a design that has never been drawn.
 *
 * Long enough for one placed on its own to arrive as itself, and short enough that a
 * cell which cannot be drawn says so rather than sitting blank.
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

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    /**
     * Nothing is loaded here. Everything this cell shows depends on state that can
     * change while the session is alive, and state read here would be read once.
     */
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Cell() }
    }

    @Composable
    private fun Cell() {
        val state = currentState<androidx.datastore.preferences.core.Preferences>()
        val slot = state.slot()
        val revision = state.revision()

        when (slot) {
            Slot.Vision -> held(revision) { visionPayload(it, size) }
                ?.let { VisionCardContent(it, size) } ?: Pending()

            Slot.TopThree -> held(revision) { topThreePayload(it, size) }
                ?.let { TopThreeCardContent(it, size) } ?: Pending()

            Slot.Wisdom -> held(revision) { wisdomPayload(it) }
                ?.let { WisdomCardContent(it, size) } ?: Pending()

            is Slot.Design -> Design(slot.designId, revision)

            // Placed and never told what to hold, which is every cell between the
            // launcher putting it down and the setting screen being answered.
            null -> Notice("Tap to set up")
        }
    }

    /**
     * Whatever [load] last gave, reloaded whenever [keys] move.
     *
     * The last good answer is held on to while the next is being fetched, and a fetch
     * that comes back with nothing leaves it alone. `produceState` cannot be used for
     * this: it drops back to its initial value the moment a key changes, so every
     * minute's tick emptied the cell before it refilled it — a clock that blinked, and
     * one that stayed blank for good if a single reload happened to fail.
     */
    @Composable
    private fun <T : Any> held(vararg keys: Any?, load: suspend (Context) -> T?): T? {
        val context = LocalContext.current
        val kept = remember { mutableStateOf<T?>(null) }
        LaunchedEffect(keys = keys) {
            runCatching { load(context) }.getOrNull()?.let { kept.value = it }
        }
        return kept.value
    }

    /** A Gallery design, drawn by the same renderer the shelf uses. */
    @Composable
    private fun Design(designId: String, revision: Long) {
        val context = LocalContext.current

        // Read so the cell redraws when it is resized, even though the picture is
        // fitted rather than measured against it.
        LocalSize.current

        val design = remember(designId) {
            GalleryCatalog.load(context).firstOrNull { it.id == designId }
        }

        // Keyed on the revision as well as the design, because a clock holds the same
        // design all its life and what moves every minute is the file behind it. The
        // tick bumps the revision once the new picture is in place, and that is what
        // brings this round again — holding the minute that is up until the next one
        // has actually been read off the disk.
        val picture = held<Bitmap>(designId, revision) {
            design?.let { wanted ->
                DesignRenderer.cached(context, wanted)
                    ?: if (GalleryTicker.paintWithin(context, wanted, FIRST_PAINT_WAIT_MS)) {
                        DesignRenderer.cached(context, wanted)
                    } else {
                        null
                    }
            }
        }
        when {
            design == null -> Notice("Unknown design")

            // Still being painted. The painting carries on past this and bumps the
            // revision when it lands, which brings the real picture here.
            picture == null -> Pending()

            else -> Image(
                provider = ImageProvider(picture),
                contentDescription = design.name,
                // Fitted rather than filled, so a design is only ever scaled — never
                // stretched out of the proportion it was drawn at.
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier
                    .fillMaxSize()
                    // A Gallery design is a face from the shelf, so a tap on one goes
                    // back there rather than to Today.
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

    /** The cell while its picture is still being made — the app's colour, and nothing said. */
    @Composable
    private fun Pending() {
        Box(modifier = GlanceModifier.fillMaxSize().background(NoticeFill)) {}
    }

    /**
     * A plain word in the cell, for the states that have no picture to show.
     *
     * Deliberately legible rather than tasteful: each of these is a fault or an
     * unfinished setup, and the point is that it can be read off the home screen.
     */
    @Composable
    private fun Notice(text: String) {
        val context = LocalContext.current
        val id = appWidgetIdOf(context, LocalGlanceId.current)

        // A cell that was never told what to hold opens the screen that tells it, for
        // the one it is actually about rather than the app's front door.
        val open = if (id == null) {
            actionStartActivity<MainActivity>()
        } else {
            actionStartActivity<WidgetConfigActivity>(
                actionParametersOf(
                    ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID) to id
                )
            )
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(NoticeFill)
                .clickable(open)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(color = ColorProvider(NoticeInk), fontSize = 12.sp)
            )
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
     * Asks one widget to draw itself again.
     *
     * Glance's own update rather than an APPWIDGET_UPDATE broadcast: that action is
     * protected, only the system may send it, and an app that tries is refused.
     *
     * Safe to call the instant a choice is written. If a session is running this
     * reaches it and the composition re-reads the state; if none is, one starts. Either
     * way the cell draws what the state now says, because the state is read inside the
     * composition rather than before it.
     */
    fun requestRedraw(context: Context, id: GlanceId, size: WidgetSize) {
        val app = context.applicationContext
        scope.launch {
            runCatching { of(size).update(app, id) }
                .onFailure { Log.w("VisionSlot", "redraw failed", it) }
        }
    }

    /**
     * Tells a widget what it holds and asks it to show that.
     *
     * Run on a scope of this object's rather than the caller's. Both callers are on
     * their way out as they call it — a setting screen about to finish, a broadcast
     * receiver about to return — and a write tied to either would be cancelled before
     * it landed.
     */
    fun assign(context: Context, id: GlanceId, size: WidgetSize, slot: Slot) {
        val app = context.applicationContext
        scope.launch {
            writeSlot(app, id, slot)
            runCatching { of(size).update(app, id) }
                .onFailure { Log.w("VisionSlot", "redraw after assign failed", it) }
        }
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
 * leaves, so nothing is being woken for a clock nobody has up.
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

    // Glance clears a widget's own state for us when the launcher says it has gone, so
    // there is nothing of ours left behind to sweep up.
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
 * tell them. The revision is moved first so the composition reloads its data rather
 * than recomposing to the same card.
 *
 * Only the cards. A Gallery design is not dressed by Studio and has its own tick.
 */
suspend fun updateDesignWidgets(context: Context) {
    placedSlots(context)
        .filter { it.slot !is Slot.Design }
        .forEach { placed ->
            bumpRevision(context, placed.id)
            runCatching { SlotWidgets.of(placed.size).update(context, placed.id) }
                .onFailure { Log.w("VisionWidget", "card redraw failed", it) }
        }
}
