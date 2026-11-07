package app.visionov.visionwidget.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
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
 * one widget being drawn, and the choice is stored against that widget's own id — so
 * two clocks placed side by side are two entries in the same store, and each is drawn
 * as itself. The earlier arrangement put the design in the class and redrew with
 * `updateAll`, which dispatches on the class: that is why a design could only ever have
 * a provider to itself, and why the second one placed used to change the first.
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

/**
 * Where each placed widget's choice is kept, against the launcher's own id for it.
 *
 * Deliberately not Glance's own per-widget state, which is the obvious home for this.
 * Writing to that needs a [GlanceId], and the only way to get one from the id a
 * configuration activity is handed is [GlanceAppWidgetManager.getGlanceIdBy], which
 * throws unless the widget is already bound. It is not reliably bound while its own
 * setting screen is open — so the write was being skipped and every widget arrived
 * holding nothing at all, which is what an empty cell on the home screen was.
 *
 * Plain preferences have none of that: the id is an Int the activity already has, and
 * the write is done before anything else is attempted. Reading back needs only
 * [GlanceAppWidgetManager.getAppWidgetId], which unwraps the id it is given and cannot
 * fail.
 */
private object SlotStore {
    private const val NAME = "vision_widget_slots"

    private const val PENDING_SLOT = "pending_slot"
    private const val PENDING_AT = "pending_at"

    /**
     * How long an unclaimed answer stays on offer.
     *
     * Long enough to cover a launcher taking its time to bind the cell, short enough
     * that an answer nobody came for cannot be picked up by an unrelated widget placed
     * later. A minute is several times the former and a small fraction of the latter.
     */
    private const val PENDING_TTL_MS = 60_000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun read(context: Context, appWidgetId: Int): Slot? =
        Slot.decode(prefs(context).getString(appWidgetId.toString(), null))

    /**
     * Committed rather than applied: the caller is a setting screen about to finish,
     * and the widget is composed the moment it does. An asynchronous write can still be
     * in flight then, which is the same empty cell by another route.
     */
    fun write(context: Context, appWidgetId: Int, slot: Slot) {
        prefs(context).edit().putString(appWidgetId.toString(), slot.encode()).commit()
    }

    fun forget(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove(appWidgetId.toString()).apply()
    }

    /** Leaves the answer where a cell that has none can find it. */
    fun offer(context: Context, slot: Slot) {
        prefs(context).edit()
            .putString(PENDING_SLOT, slot.encode())
            .putLong(PENDING_AT, System.currentTimeMillis())
            .commit()
    }

    /**
     * Takes the offered answer, whatever size asks for it, and spends it.
     *
     * Not matched against the size it was offered for, because that size is the least
     * reliable thing to hand. A setting screen asks the system what size its own widget
     * is before the widget is bound, and gets nothing — so it falls back to a guess, and
     * a wrong guess here would be a match that never happens and a fallback that never
     * fires. Which is the bug this exists to fix, kept alive by the guard against it.
     *
     * What is left guarding it is enough: the answer lives a minute, and it is spent on
     * the way out so no second cell can take the same one. Two widgets of different
     * sizes placed inside the same minute, with the first one's id lost, is the whole of
     * what this can get wrong — and getting it wrong shows the user the design they just
     * picked in a cell of another shape, which is a better wrong answer than an empty
     * cell reading Tap to set up.
     */
    fun claim(context: Context): Slot? {
        val store = prefs(context)
        val fresh = System.currentTimeMillis() - store.getLong(PENDING_AT, 0L) < PENDING_TTL_MS
        if (!fresh) return null

        val slot = Slot.decode(store.getString(PENDING_SLOT, null)) ?: return null
        clearOffer(context)
        return slot
    }

    fun clearOffer(context: Context) {
        prefs(context).edit()
            .remove(PENDING_SLOT)
            .remove(PENDING_AT)
            .commit()
    }
}

/** What the widget behind [appWidgetId] is holding, or null if it was never told. */
fun readSlot(context: Context, appWidgetId: Int): Slot? = SlotStore.read(context, appWidgetId)

/** What [id] is holding, or null if it was placed without ever being told. */
fun readSlot(context: Context, id: GlanceId): Slot? =
    appWidgetIdOf(context, id)?.let { SlotStore.read(context, it) }

/**
 * Writes what the widget behind [appWidgetId] is to hold.
 *
 * Does not redraw — the caller settles that, because the two places this is called from
 * want different things. The setting screen writes before handing the widget back to
 * the launcher, which composes it itself; a pin callback writes to a widget already up,
 * and has to ask for the redraw.
 */
fun writeSlot(context: Context, appWidgetId: Int, slot: Slot) {
    SlotStore.write(context, appWidgetId, slot)
    Log.i("VisionSlot", "wrote ${slot.encode()} to widget $appWidgetId")
}

/**
 * Leaves the answer somewhere a cell can find it, for when the id cannot be trusted.
 *
 * The setting screen is handed an id before the widget is finally bound, and not every
 * launcher keeps that id: some bind the cell under a second one once the screen returns
 * OK, and at least one takes the first id away afterwards, which erases what was written
 * against it. Either way the cell composes holding nothing, and "Tap to set up" is what
 * stays on the home screen.
 *
 * So the answer is written twice — against the id, which is right when the id survives,
 * and here, which is right when it doesn't. The second copy is addressed by size rather
 * than by id, lives a minute, and is spent by the first cell of that size to come up
 * without an answer of its own.
 */
fun offerSlot(context: Context, slot: Slot) {
    SlotStore.offer(context, slot)
    Log.i("VisionSlot", "offered ${slot.encode()} to the next cell that has none")
}

/** Takes the offered answer, if one is waiting. */
fun claimOfferedSlot(context: Context): Slot? = SlotStore.claim(context)

/** Drops the offer, once the cell it was meant for has an answer of its own. */
fun clearOfferedSlot(context: Context) = SlotStore.clearOffer(context)

/** Drops what a widget was holding, once the launcher says it has gone. */
fun forgetSlot(context: Context, appWidgetId: Int) = SlotStore.forget(context, appWidgetId)

/**
 * The launcher's own id behind a [GlanceId].
 *
 * An unwrap rather than a lookup — see [SlotStore] for why that distinction is the
 * whole of this file's design.
 */
fun appWidgetIdOf(context: Context, id: GlanceId): Int? =
    runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrNull()

/** The [GlanceId] behind a launcher's own widget id, or null if it is not bound yet. */
fun glanceIdOf(context: Context, appWidgetId: Int): GlanceId? =
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
