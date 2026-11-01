package com.example.visionwidget.ui.gallery

import android.content.Context
import org.json.JSONObject

/**
 * The moment every design is shown at when it is being shown rather than used.
 *
 * The same one the Gallery preview images were captured at, so the picture in the
 * launcher's picker and the tile it was picked from agree. Changing it changes both.
 */
const val PREVIEW_DATE = "2026-10-27"
const val PREVIEW_TIME = "13:20"

/** Said in one place, for the comments that have to name it. */
const val PREVIEW_MOMENT = "$PREVIEW_DATE $PREVIEW_TIME"

/**
 * The designed widgets on the Gallery shelf.
 *
 * Each is a small web page under `assets/gallery/`, drawn as it was designed rather than
 * redrawn here — there are forty-odd of them and they lean on gradients, blurs and
 * photographs that would each have to be approximated by hand otherwise.
 *
 * The shelf is read from `catalog.json`, which the import script writes beside the
 * designs from the order files that ship with them. The orders are a decision about how
 * the shelf should read, so they are carried across rather than invented here.
 */
data class GalleryWidget(
    val id: String,
    val name: String,
    /** "clock" or "calendar" — the folder it lives in and the filter it answers to. */
    val category: String,
    /** "small" or "medium". */
    val size: String,
    /** "2x2" or "4x2", as the launcher counts cells. */
    val cells: String,
    /** Width over height, as the design was drawn. */
    val ratio: Float,
    /**
     * The width in CSS pixels the design was laid out for.
     *
     * Most measure everything inside themselves as a share of their own width and would
     * fit whatever they were given; two of the clocks are set in fixed pixels and only
     * hold together at the width they were drawn for. The page is handed this width and
     * the view scales the result, so both kinds land the same way.
     */
    val designWidth: Int,
    /**
     * The width the page is laid out at, which is usually [designWidth].
     *
     * A few designs carry breakpoints of their own that fire at or above the width they
     * were drawn for — the analog clocks shrink themselves under 420px, and they are
     * drawn at 320. Those are laid out just past the breakpoint so the design gets the
     * arrangement it was drawn in, and scaled up to the frame from there.
     */
    val viewportWidth: Int,
    val allDefault: Int,
    val allLatest: Int,
    val catDefault: Int,
    val catLatest: Int
) {
    /** The design's own page, loaded straight out of the assets, keeping real time. */
    val url: String get() = "file:///android_asset/gallery/$category/$id/index.html"

    /**
     * The same page with the clock and the calendar held at [PREVIEW_MOMENT].
     *
     * Anywhere a design is being shown rather than used — the shelf, and the picture the
     * launcher's picker shows before a widget is placed — every design reads the same
     * moment. A shelf where each tile says a slightly different time reads as a fault
     * rather than as a set, and the picker's picture is a frozen PNG anyway, so a live
     * clock there would only ever be wrong.
     *
     * The designs take the moment on their own query string; it is what their scripts
     * offer it for.
     */
    val previewUrl: String get() = "$url?date=$PREVIEW_DATE&time=$PREVIEW_TIME"

    /** "SMALL · 2 × 2" — what the design is, under its name. */
    val meta: String get() = "${size.uppercase()} · ${cells.replace("x", " × ")}"

    /**
     * The design's id as Android will take it for a resource or a class name.
     *
     * Every design has a widget of its own — that is what lets two of them sit on the
     * home screen at once — so each needs a provider, a preview and a label named after
     * it, and none of those may carry a hyphen.
     */
    val resourceName: String get() = id.replace('-', '_').lowercase()
}

/** Which designs the shelf is showing. */
enum class GalleryCategory(val label: String) {
    All("All"),
    Calendar("Calendar"),
    Clock("Clock");

    /** Whether [widget] belongs under this filter. */
    fun holds(widget: GalleryWidget) =
        this == All || widget.category.equals(name, ignoreCase = true)
}

/**
 * What order they come in.
 *
 * Default is the order they were arranged in deliberately; Latest is newest first. Both
 * are carried in the catalog per filter, since a design's place among the calendars is
 * not its place among all of them.
 */
enum class GallerySort(val label: String) {
    Default("Default"),
    Latest("Latest")
}

object GalleryCatalog {

    private const val ASSET = "gallery/catalog.json"

    /**
     * Parsed once per process.
     *
     * The catalogue is read on every widget update, and there is one widget per design —
     * so without this, a round of updates reads and parses the same file forty-odd
     * times over, on the thread a broadcast is given to answer in.
     */
    @Volatile
    private var loaded: List<GalleryWidget>? = null

    /** Every design, in the order the file lists them. */
    fun load(context: Context): List<GalleryWidget> =
        loaded ?: read(context).also { loaded = it }

    private fun read(context: Context): List<GalleryWidget> = runCatching {
        val text = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        val widgets = JSONObject(text).getJSONArray("widgets")
        (0 until widgets.length()).map { index ->
            val it = widgets.getJSONObject(index)
            GalleryWidget(
                id = it.getString("id"),
                name = it.getString("name"),
                category = it.getString("category"),
                size = it.getString("size"),
                cells = it.getString("cells"),
                ratio = it.getDouble("ratio").toFloat(),
                designWidth = it.getInt("designWidth"),
                viewportWidth = it.optInt("viewportWidth", it.getInt("designWidth")),
                allDefault = it.getInt("allDefault"),
                allLatest = it.getInt("allLatest"),
                catDefault = it.getInt("catDefault"),
                catLatest = it.getInt("catLatest")
            )
        }
    }.getOrDefault(emptyList())
}

/**
 * The shelf as [category] and [sort] ask for it.
 *
 * Under All the figures counted across everything are the ones that apply; under a
 * single category the design's place within that category is.
 */
fun List<GalleryWidget>.arrange(
    category: GalleryCategory,
    sort: GallerySort
): List<GalleryWidget> {
    val kept = filter(category::holds)
    val place: (GalleryWidget) -> Int = when {
        category == GalleryCategory.All && sort == GallerySort.Default -> GalleryWidget::allDefault
        category == GalleryCategory.All -> GalleryWidget::allLatest
        sort == GallerySort.Default -> GalleryWidget::catDefault
        else -> GalleryWidget::catLatest
    }
    return kept.sortedBy(place)
}
