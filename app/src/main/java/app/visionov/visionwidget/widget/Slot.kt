package app.visionov.visionwidget.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.visionov.visionwidget.ui.gallery.GalleryWidget

/**
 * What a widget on the home screen is holding, and how it comes to know.
 *
 * There is one provider per size rather than one per design. The launcher's picker
 * lists providers, so a provider per design meant fifty-six entries in it; three sizes
 * is what the picker is meant to show, and which design a particular widget draws is
 * then settled in a setting screen of our own.
 *
 * The choice lives in Glance's own per-widget state, and is read from inside the
 * composition rather than before it. That is the whole of why this works, and it took
 * three attempts to get right — see [SlotKey].
 */
sealed interface Slot {

    /** The dressed cards, always offered whatever the library holds. */
    data object Vision : Slot
    data object TopThree : Slot
    data object Wisdom : Slot

    /** A Gallery design, named by its catalogue id. */
    data class Design(val designId: String) : Slot

    /** How the slot is written down. */
    fun encode(): String = when (this) {
        Vision -> VISION
        TopThree -> TOP_THREE
        Wisdom -> WISDOM
        is Design -> DESIGN_PREFIX + designId
    }

    companion object {
        private const val VISION = "vision"
        private const val TOP_THREE = "top3"
        private const val WISDOM = "wisdom"
        private const val DESIGN_PREFIX = "gallery:"

        /** Null for anything unreadable, which a widget draws as a prompt rather than guessing. */
        fun decode(raw: String?): Slot? = when {
            raw == null -> null
            raw == VISION -> Vision
            raw == TOP_THREE -> TopThree
            raw == WISDOM -> Wisdom
            raw.startsWith(DESIGN_PREFIX) ->
                raw.removePrefix(DESIGN_PREFIX).takeIf { it.isNotEmpty() }?.let(::Design)
            else -> null
        }
    }
}

/** The three cells a widget may be placed in. */
enum class WidgetSize(
    /** What the catalogue calls designs of this size, or null where it offers none. */
    val catalogSize: String?
) {
    Small("small"),
    Medium("medium"),

    /**
     * Four by four. The Gallery designs are all drawn at 2x2 or 4x2, so nothing of the
     * catalogue fits here — a large widget holds one of the three cards or nothing.
     */
    Large(null);

    /** Whether [design] can be put in a cell of this size. */
    fun holds(design: GalleryWidget) =
        catalogSize != null && design.size.equals(catalogSize, ignoreCase = true)
}

/**
 * Where a widget's choice is written, in Glance's own per-widget store.
 *
 * It has to be here rather than anywhere of our own, and it has to be read from inside
 * the composition. Glance says so plainly: "update and updateAll do not restart
 * provideGlance if it is already running... observe your sources of data within the
 * composition." A session lives about forty-five seconds past its first draw, and
 * anything read before `provideContent` is captured for the whole of it.
 *
 * Every widget fault so far has been that sentence being ignored. A cell read its
 * choice in `provideGlance`, found none because the launcher composes it while its
 * setting screen is still open, and then kept the placeholder no matter what was
 * chosen — the redraws arrived, but they recomposed a tree holding the old answer. The
 * same capture is why a clock stood still: its picture was loaded once and the minute's
 * redraw had nothing new to show.
 *
 * Read through [androidx.glance.currentState] instead, a redraw genuinely redraws:
 * `update` sends the session an UpdateGlanceState event, which re-reads this store and
 * recomposes against it.
 */
val SlotKey = stringPreferencesKey("slot")

/**
 * Bumped whenever what a widget draws has changed underneath it, so the composition
 * reloads rather than recomposing to the same picture.
 *
 * The slot alone is not enough. A clock holds the same slot from the moment it is
 * placed until it is taken away, and what changes every minute is the file behind it —
 * nothing the composition would notice. This is what it notices.
 */
val RevisionKey = longPreferencesKey("revision")

/** Reads one widget's choice from outside a composition, for callers that cannot compose. */
suspend fun readSlot(context: Context, id: GlanceId): Slot? = runCatching {
    Slot.decode(getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[SlotKey])
}.getOrNull()

/**
 * Writes what a widget is to hold, and marks it as having changed.
 *
 * Both in one write, because a widget that has just been told what it is has also just
 * had everything about it change.
 */
suspend fun writeSlot(context: Context, id: GlanceId, slot: Slot) {
    runCatching {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { kept ->
            kept.toMutablePreferences().apply {
                this[SlotKey] = slot.encode()
                this[RevisionKey] = System.currentTimeMillis()
            }
        }
        Log.i("VisionSlot", "wrote ${slot.encode()} to ${appWidgetIdOf(context, id)}")
    }.onFailure { Log.w("VisionSlot", "could not write ${slot.encode()}", it) }
}

/**
 * Says that what a widget draws has changed, without changing what it holds.
 *
 * What the minute's tick does to a clock, and what applying a style in Studio does to
 * the three cards.
 */
suspend fun bumpRevision(context: Context, id: GlanceId) {
    runCatching {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { kept ->
            kept.toMutablePreferences().apply {
                this[RevisionKey] = System.currentTimeMillis()
            }
        }
    }.onFailure { Log.w("VisionSlot", "could not bump revision", it) }
}

/** The slot a composition is looking at, read out of the state it was handed. */
fun Preferences.slot(): Slot? = Slot.decode(this[SlotKey])

/** The revision a composition is looking at; zero before anything has changed. */
fun Preferences.revision(): Long = this[RevisionKey] ?: 0L

/**
 * The [GlanceId] for a widget being configured.
 *
 * Taken off the intent rather than looked up. The lookup — `getGlanceIdBy(Int)` —
 * demands that the widget already be bound and throws when it is not, which is exactly
 * the state a widget is in while its own setting screen is open. This overload is the
 * one Glance provides for configuration activities and it only unwraps the extra.
 */
fun glanceIdFor(context: Context, intent: Intent): GlanceId? =
    runCatching { GlanceAppWidgetManager(context).getGlanceIdBy(intent) }.getOrNull()

/** The launcher's own id behind a [GlanceId] — an unwrap, which cannot fail. */
fun appWidgetIdOf(context: Context, id: GlanceId): Int? =
    runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrNull()

/**
 * One of our widgets as it sits on a home screen.
 *
 * The size travels with the id because a redraw needs it: asking a widget to update a
 * [GlanceId] draws that widget's own content into the cell, so a 4x2 cell redrawn by
 * the 2x2 widget would come back as the small card.
 */
class PlacedWidget(val id: GlanceId, val size: WidgetSize, val slot: Slot)

/**
 * Every widget of ours now on a home screen, with what it holds.
 *
 * Three calls across to the widget service rather than one per design. The old shape
 * asked once per catalogue entry — forty-four round trips to answer "is anything up?",
 * which is what the `known` flag through [GalleryTicker] existed to dodge.
 */
suspend fun placedSlots(context: Context): List<PlacedWidget> {
    val manager = GlanceAppWidgetManager(context)
    return WidgetSize.entries.flatMap { size ->
        val ids = runCatching { manager.getGlanceIds(SlotWidgets.of(size).javaClass) }
            .getOrDefault(emptyList())
        ids.mapNotNull { id -> readSlot(context, id)?.let { PlacedWidget(id, size, it) } }
    }
}
