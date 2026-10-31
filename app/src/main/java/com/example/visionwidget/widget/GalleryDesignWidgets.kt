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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.example.visionwidget.MainActivity
import com.example.visionwidget.ui.gallery.DesignRenderer
import com.example.visionwidget.ui.gallery.GalleryCatalog

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
        val bitmap = design?.let {
            val density = context.resources.displayMetrics.density
            // Held under what one widget may carry, the same ceiling the dressed cards
            // keep to.
            val widthPx = (it.designWidth * density).toInt().coerceAtMost(MAX_DESIGN_SIDE)
            val heightPx = (widthPx / it.ratio).toInt()
            // Live, not the frozen moment the shelf and the picker show: this one is on
            // the home screen being used rather than being looked at, and a clock that
            // is wrong is no clock at all.
            DesignRenderer.render(context, it, widthPx, heightPx, frozen = false)
        }

        provideContent {
            // Read so the widget redraws when the cell changes, even though the picture
            // itself is fitted rather than measured against it.
            LocalSize.current
            bitmap?.let {
                Image(
                    provider = ImageProvider(it),
                    contentDescription = design.name,
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

    private companion object {
        /** As wide as a design is ever painted for a widget. */
        const val MAX_DESIGN_SIDE = 1080
    }
}

/**
 * What every Gallery design's receiver is built on — it only has to name its design.
 *
 * The receivers themselves are generated from the catalogue; see
 * `tools/generate_gallery_widgets.ps1`.
 */
abstract class GalleryWidgetReceiver(designId: String) : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GalleryDesignWidget(designId)
}
