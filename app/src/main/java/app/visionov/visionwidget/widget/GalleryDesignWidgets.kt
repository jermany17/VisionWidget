package app.visionov.visionwidget.widget

import android.content.Context
import androidx.glance.GlanceId
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
import app.visionov.visionwidget.ui.gallery.GalleryWidget
import kotlin.math.sqrt

/**
 * A Gallery design, as a home screen widget.
 *
 * These are finished faces rather than something Studio dresses, so a widget has nothing
 * to read but which design it is. That is fixed per widget rather than stored: there is
 * one provider per design, declared in [GalleryWidgetReceivers], which is what lets two
 * clocks or two calendars sit on the home screen at once showing different faces. A
 * single provider shared by every design of a size cannot — the second one placed would
 * change the first.
 *
 * Drawn by the same renderer the shelf uses, so a widget is the tile it was picked from.
 */
/**
 * How long a widget's own update will wait for a design that has never been drawn.
 *
 * Long enough for a design placed on its own to arrive as itself, and short enough to
 * leave room under the deadline a broadcast is answered within.
 */
private const val FIRST_PAINT_WAIT_MS = 6_000L

open class GalleryDesignWidget(private val designId: String) : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val design = GalleryCatalog.load(context).firstOrNull { it.id == designId }

        // Off the disk, which is immediate.
        val kept = design?.let { DesignRenderer.cached(context, it) }

        // Nothing kept yet means this design has not been drawn since it was installed,
        // so it is drawn now and waited for.
        //
        // Waited for rather than stood in for: the picture the launcher's picker shows
        // is the design frozen at a moment that has already passed, so putting that up
        // here would mean a widget arriving on the wrong month and correcting itself a
        // second later. A widget shows the real design or it shows nothing yet.
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

        android.util.Log.i(
            "VisionGallery",
            "show $designId found=${design != null} cached=${kept != null} " +
                "bitmap=${bitmap?.width}x${bitmap?.height}"
        )

        provideContent {
            // Read so the widget redraws when the cell changes, even though the picture
            // itself is fitted rather than measured against it.
            LocalSize.current

            if (bitmap == null) {
                // Still a box rather than an empty tree: with nothing composed the
                // launcher goes on showing the layout it put up while it waited and
                // nothing ever replaces it. Empty for now, and the painting still
                // running will ask for the redraw that fills it.
                Box(modifier = GlanceModifier.fillMaxSize()) {}
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

/**
 * As many pixels as a design is ever painted into for a widget.
 *
 * Not a limit the system imposes — a bitmap reaches the launcher as shared memory, and
 * what one widget may carry is six bytes per screen pixel, which is megabytes more than
 * any of these. It is the cell that sets this: a two-by-two is some five hundred pixels
 * across on a phone this size, so a design drawn at a thousand is paying twice over for
 * detail the cell has nowhere to put.
 *
 * Held as an area rather than a width, because a design's height follows its own
 * proportion rather than the cap: limiting the width alone let a square design through
 * at more than twice what a wide one cost.
 */
private const val MAX_DESIGN_PX = 640 * 640

/**
 * The size a design is painted at for the home screen — its own width at the screen's
 * density, so a widget is as sharp as the screen can draw it, held under what one widget
 * is allowed to carry.
 */
fun GalleryWidget.widgetWidthPx(context: Context): Int {
    val natural = (designWidth * context.resources.displayMetrics.density).toInt()
    // width × height ≤ MAX, and height is width ÷ ratio, so width ≤ √(MAX × ratio).
    val allowed = sqrt(MAX_DESIGN_PX.toDouble() * ratio).toInt()
    return natural.coerceAtMost(allowed).coerceAtLeast(1)
}

fun GalleryWidget.widgetHeightPx(context: Context): Int =
    (widgetWidthPx(context) / ratio).toInt().coerceAtLeast(1)

/**
 * What every Gallery design's receiver is built on — it only has to name its widget.
 *
 * The widget handed in is the design's own subclass, not a shared one carrying its id.
 * Glance knows a widget by its class: with forty-four designs behind a single class it
 * cannot tell them apart, and asking it to redraw one redraws them all with whichever
 * design was being painted at the time. That is what put a different face on a widget
 * than the one chosen, and what left the clocks stuck — the redraw landed somewhere
 * other than where it was aimed.
 *
 * The subclasses and receivers are generated from the catalogue; see
 * `tools/generate_gallery_widgets.ps1`.
 */
abstract class GalleryWidgetReceiver(
    override val glanceAppWidget: GlanceAppWidget
) : GlanceAppWidgetReceiver() {

    // The tick is booked when a design arrives on a screen and let go of when the last
    // one leaves, so nothing is being woken for a clock nobody has up.
    //
    // Being here at all means a widget of this design exists, so the booking is told as
    // much rather than going looking: there is one receiver per design, and a round of
    // updates would otherwise have each of them scan every other.
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        GalleryTicker.schedule(context, known = true)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        GalleryTicker.schedule(context, known = appWidgetIds.isNotEmpty())
    }

    // The last of a design has gone, which may or may not be the last of all of them —
    // so this one does look, and it is the one case where looking is worth it.
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        GalleryTicker.schedule(context)
    }
}
