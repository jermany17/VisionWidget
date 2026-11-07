package app.visionov.visionwidget.widget

import android.content.Context
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
 * That only works because the choice is kept per widget rather than per class. Glance
 * hands [androidx.glance.appwidget.GlanceAppWidget.provideGlance] the [GlanceId] of the
 * one widget being drawn, and the state read here is keyed by it — so two clocks placed
 * side by side are two rows in the same store, and each is drawn as itself. The earlier
 * arrangement put the design in the class and redrew with `updateAll`, which dispatches
 * on the class: that is why a design could only ever have a provider to itself, and why
 * the second one placed used to change the first.
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

        /** Null for anything unreadable, which a widget draws as empty rather than guessing. */
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

/** Where the choice is written in the widget's own state. */
private val SlotKey = stringPreferencesKey("slot")

/** What [id] is holding, or null if it was placed without ever being told. */
suspend fun readSlot(context: Context, id: GlanceId): Slot? =
    runCatching {
        Slot.decode(getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[SlotKey])
    }.getOrNull()

/**
 * Writes what [id] is to hold.
 *
 * Does not redraw — the caller settles that, because the two places this is called from
 * want different things. The setting screen writes before handing the widget back to the
 * launcher, which draws it itself; a pin callback writes to a widget already up, and has
 * to ask for the redraw.
 */
suspend fun writeSlot(context: Context, id: GlanceId, slot: Slot) {
    updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { kept ->
        kept.toMutablePreferences().apply { this[SlotKey] = slot.encode() }
    }
}

/** The [GlanceId] behind a launcher's own widget id, or null if it has gone. */
suspend fun glanceIdOf(context: Context, appWidgetId: Int): GlanceId? =
    runCatching { GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId) }.getOrNull()

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
