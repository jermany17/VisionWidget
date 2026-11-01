package com.example.visionwidget.widget

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
import com.example.visionwidget.MainActivity
import com.example.visionwidget.ui.gallery.DesignRenderer
import com.example.visionwidget.ui.gallery.GalleryCatalog
import com.example.visionwidget.ui.gallery.GalleryWidget

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
class GalleryDesignWidget(private val designId: String) : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val design = GalleryCatalog.load(context).firstOrNull { it.id == designId }

        // Off the disk, which is immediate. An update is a broadcast with a deadline,
        // and painting a design takes a second or two — fine on its own, but when every
        // widget is asked to redraw at once they queue behind the one view that paints
        // and the later ones run out of time. That is what left widgets spinning.
        //
        // Nothing there yet means this is the first time the design has been put up, so
        // it is painted here and then kept; after that the ticker does the painting.
        val bitmap = design?.let {
            DesignRenderer.cached(context, it)
                ?: run {
                    DesignRenderer.refresh(context, it, it.widgetWidthPx(context), it.widgetHeightPx(context))
                    DesignRenderer.cached(context, it)
                }
        }

        // The design's own picker image, which is built into the app and always there.
        // Composing nothing is what left a widget spinning forever: with an empty tree
        // the launcher goes on showing the layout it puts up while it waits, and nothing
        // ever replaces it. A design that was slow to paint now arrives as its frozen
        // self and is replaced by the live one on the next tick.
        val fallback = design?.let {
            context.resources
                .getIdentifier("preview_gallery_${it.resourceName}", "drawable", context.packageName)
                .takeIf { res -> res != 0 }
        }

        provideContent {
            // Read so the widget redraws when the cell changes, even though the picture
            // itself is fitted rather than measured against it.
            LocalSize.current

            val provider = when {
                bitmap != null -> ImageProvider(bitmap)
                fallback != null -> ImageProvider(fallback)
                else -> null
            }

            if (provider == null) {
                // Still a box rather than an empty tree, so the cell has something in it.
                Box(modifier = GlanceModifier.fillMaxSize()) {}
            } else {
                Image(
                    provider = provider,
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

/** As wide as a design is ever painted for a widget. */
private const val MAX_DESIGN_SIDE = 1080

/**
 * The size a design is painted at for the home screen — its own width at the screen's
 * density, so a widget is as sharp as the screen can draw it, held under what one widget
 * is allowed to carry.
 */
fun GalleryWidget.widgetWidthPx(context: Context): Int =
    (designWidth * context.resources.displayMetrics.density).toInt()
        .coerceAtMost(MAX_DESIGN_SIDE)

fun GalleryWidget.widgetHeightPx(context: Context): Int =
    (widgetWidthPx(context) / ratio).toInt()

/**
 * What every Gallery design's receiver is built on — it only has to name its design.
 *
 * The receivers themselves are generated from the catalogue; see
 * `tools/generate_gallery_widgets.ps1`.
 */
abstract class GalleryWidgetReceiver(designId: String) : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GalleryDesignWidget(designId)

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
