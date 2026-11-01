package com.example.visionwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.example.visionwidget.ui.gallery.DesignRenderer
import com.example.visionwidget.ui.gallery.GalleryCatalog
import com.example.visionwidget.ui.gallery.GalleryWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps the Gallery clocks running.
 *
 * A widget is only redrawn when something asks it to, and the schedule a provider can
 * declare for itself bottoms out at half an hour — which is no use to a clock showing
 * minutes. So one is driven from here: an alarm a minute out, rescheduled each time it
 * lands, running only while there is a Gallery widget on the screen to redraw.
 *
 * The alarm is inexact, which needs no permission and is accurate to a few seconds. The
 * clocks show minutes, so that is close enough; an exact alarm every minute would be
 * both restricted on recent Android and far more than this is worth.
 *
 * Only the clocks ride it. A calendar changes once a day, and waking twenty of those
 * every minute to redraw the same picture would be a waste of the battery — they follow
 * the day instead, which the system announces.
 */
object GalleryTicker {

    private const val REQUEST = 0xC10C
    private const val ACTION = "com.example.visionwidget.GALLERY_TICK"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Sets the next tick, if there is anything on the screen that wants one.
     *
     * [known] says the caller has already seen a widget — which the update of one has,
     * by definition. Asking again would mean a scan of every design, and with one widget
     * per design that scan would run once per design per round of updates: some two
     * thousand calls across to the widget service, on the thread a broadcast is given to
     * answer in. That is enough to stall the system, and it did.
     */
    fun schedule(context: Context, known: Boolean = false) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val app = context.applicationContext

        if (!known && !hasGalleryWidget(app)) {
            alarms.cancel(pendingIntent(app))
            return
        }

        // The top of the next minute, so the figure changes when the clock says it does
        // rather than a stray number of seconds later.
        val now = System.currentTimeMillis()
        val nextMinute = now - (now % 60_000L) + 60_000L
        alarms.setWindow(
            AlarmManager.RTC,
            nextMinute,
            5_000L,
            pendingIntent(app)
        )
    }

    /** Repaints the clocks that are actually up, then books the next tick. */
    fun tick(context: Context) {
        val app = context.applicationContext
        scope.launch {
            repaint(app) { it.category.equals("clock", ignoreCase = true) }
            schedule(app)
        }
    }

    /** Repaints everything — the day has turned, so the calendars have moved too. */
    fun redrawAll(context: Context) {
        val app = context.applicationContext
        scope.launch { repaint(app) { true } }
    }

    /**
     * Paints each design that is on a screen, then tells its widget to pick the picture
     * up.
     *
     * The painting is done here rather than inside the widget's own update because that
     * update is a broadcast with a deadline it cannot afford to spend on a web view.
     */
    private suspend fun repaint(context: Context, wanted: (GalleryWidget) -> Boolean) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        GalleryCatalog.load(context).filter(wanted).forEach { design ->
            val up = runCatching {
                manager.getAppWidgetIds(ComponentName(context, receiverFor(design))).isNotEmpty()
            }.getOrDefault(false)
            if (!up) return@forEach

            runCatching {
                DesignRenderer.refresh(
                    context,
                    design,
                    design.widgetWidthPx(context),
                    design.widgetHeightPx(context)
                )
                GalleryDesignWidget(design.id).updateAll(context)
            }
        }
    }

    /**
     * Whether any Gallery design is actually on a screen.
     *
     * Asked of the widget manager rather than remembered: a widget can be dropped in the
     * bin without the app hearing about it in any way it could rely on.
     */
    private fun hasGalleryWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return runCatching {
            // Stops at the first one found. The whole list is only walked when there are
            // none left, which is exactly when the answer is that the tick should stop.
            GalleryCatalog.load(context).any { design ->
                manager.getAppWidgetIds(
                    ComponentName(context, receiverFor(design))
                ).isNotEmpty()
            }
        }.getOrDefault(false)
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST,
            Intent(context, GalleryTickReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

/**
 * Takes the minute's tick, and the handful of system announcements that move a clock or
 * a calendar on their own — the day turning, the time being set, a change of timezone.
 */
class GalleryTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                GalleryTicker.redrawAll(context)
                GalleryTicker.schedule(context)
            }

            Intent.ACTION_BOOT_COMPLETED -> GalleryTicker.schedule(context)

            else -> GalleryTicker.tick(context)
        }
    }
}
