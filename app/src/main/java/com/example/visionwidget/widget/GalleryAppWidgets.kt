package com.example.visionwidget.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionStartActivity
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import com.example.visionwidget.MainActivity

/**
 * The Gallery faces as home screen widgets.
 *
 * Each is a single image: the face is painted by [FaceRenderer] and handed to the
 * launcher as a bitmap, which keeps the designs exactly as drawn rather than
 * approximating them in the handful of things Glance can express. Neither face reads
 * anything of the user's own — they show the date and nothing else — so there is no
 * state to carry and no store to read.
 */

/**
 * A ceiling on how large a face is painted.
 *
 * A widget's bitmap crosses a Binder transaction to the launcher, and a full-resolution
 * one on a dense screen is large enough to be refused. Four hundred thousand pixels is
 * comfortably under that and still sharper than the cell it lands in.
 */
private const val MaxFacePx = 640

/**
 * The face, sized to the cell it was given.
 *
 * Painted square at the shorter side: both designs are square, and a widget that has
 * been resized out of square should letterbox rather than stretch.
 */
@Composable
private fun Face(paint: (Context, Int) -> Bitmap) {
    val context = LocalContext.current
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val sidePx = (minOf(size.width.value, size.height.value) * density)
        .toInt()
        .coerceIn(1, MaxFacePx)

    Image(
        provider = ImageProvider(paint(context, sidePx)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        // Nothing inside the face can be acted on, so the whole of it opens the app —
        // which is the only thing a tap here could reasonably mean.
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>())
    )
}

class ClockAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Face { ctx, px -> renderClockFace(ctx, px) } }
    }
}

class QuoteAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Face { ctx, px -> renderQuoteFace(ctx, px) } }
    }
}

class ClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockAppWidget()

    // The minute runs only while a clock is up, and stops with the last of them.
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleClockTick(context)
    }

    override fun onDisabled(context: Context) {
        cancelClockTick(context)
        super.onDisabled(context)
    }
}

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteAppWidget()
}

/**
 * The calendar is wide rather than square, so it is given the cell's full width and
 * drawn at its own proportion beneath that.
 */
class BlushAppWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val glanceContext = LocalContext.current
            val size = LocalSize.current
            val density = glanceContext.resources.displayMetrics.density
            val widthPx = (size.width.value * density).toInt().coerceIn(1, MaxFacePx)

            Image(
                provider = ImageProvider(renderBlushCalendar(glanceContext, widthPx)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity<MainActivity>())
            )
        }
    }
}

class BlushWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BlushAppWidget()
}
