package app.visionov.visionwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import app.visionov.visionwidget.ui.gallery.DesignRenderer
import app.visionov.visionwidget.ui.gallery.GalleryCatalog
import app.visionov.visionwidget.ui.gallery.GalleryWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

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
    private const val ACTION = "app.visionov.visionwidget.GALLERY_TICK"

    /**
     * How far before the minute the tick is woken.
     *
     * The drawing is done in this margin, while the minute before is still the one
     * showing, so that when the minute turns there is nothing left to do but hand the
     * finished picture over. Painting after the turn instead put the whole cost of it —
     * a web view, a photograph, a typeface — between the clock changing and the widget
     * saying so, which is the delay this is here to remove.
     */
    private const val LEAD_MS = 6_000L

    /**
     * How loose the waking alarm may be.
     *
     * It does not have to be precise. Whatever moment it arrives at, the picture is held
     * back until the system clock actually reaches the minute — so the figure changes on
     * the second it should, not on the second the alarm happened to land.
     */
    private const val WINDOW_MS = 2_000L

    private const val MINUTE_MS = 60_000L

    /** The minute the pictures that are up were drawn for. */
    @Volatile
    private var shownMinute = 0L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * The paintings under way, by design.
     *
     * A widget asks for one on every update it arrives at without a picture, and the
     * same design can be asked for several times over before the first has finished.
     * One painting per design is enough, and whoever asks can wait on the one already
     * running rather than starting another behind it.
     */
    private val inFlight = ConcurrentHashMap<String, Deferred<Boolean>>()


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

        // A little before the top of the next minute, so there is room to draw it before
        // it arrives. If that moment has already gone by — a widget added mid-minute asks
        // for this at any point at all — the tick runs at once instead and catches up,
        // rather than standing the whole minute out.
        val now = System.currentTimeMillis()
        val at = (now - (now % MINUTE_MS) + MINUTE_MS - LEAD_MS).coerceAtLeast(now + 250L)
        alarms.setWindow(
            AlarmManager.RTC,
            at,
            WINDOW_MS,
            pendingIntent(app)
        )
        android.util.Log.i("VisionGallery", "next tick in ${at - now}ms")
    }

    /**
     * Draws the minute that is coming, puts it up the moment it arrives, books the next.
     *
     * [done] is the broadcast's own hold on the process. Without it the receiver returns
     * the moment this is launched and the system is free to stop the app before any of
     * the painting has happened — which is a clock that never moves.
     */
    fun tick(context: Context, done: () -> Unit = {}) {
        val app = context.applicationContext
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val thisMinute = now - (now % MINUTE_MS)

                val target = thisMinute + MINUTE_MS

                // The minute showing is behind the minute it is: the waking alarm came
                // late, or the process was away while several went by. Put the right one
                // up at once rather than letting the wrong one stand until the next turn.
                //
                // Only when the turn is far enough off to be worth it. Close to it the
                // flip below is along in a moment anyway, and a painting started here
                // would still be running then — landing on the picture the flip had just
                // put up and pulling the widget back a minute.
                if (shownMinute != thisMinute && target - now > LEAD_MS + WINDOW_MS) {
                    catchUp(app, thisMinute)
                }

                flipOn(app, target)
                schedule(app)
            } finally {
                done()
            }
        }
    }

    /**
     * Draws every design that is up for [target], then puts them all up on the stroke.
     *
     * The waiting is what makes the change land when the clock says it does. The alarm
     * only has to arrive somewhere in the margin before the minute; the system clock is
     * what the picture is held for, so the figure turns on the second and not a stray
     * number of them later.
     */
    private suspend fun flipOn(context: Context, target: Long) {
        // Only ever the minute we were woken for. Asked at any other time — a widget
        // just added, a tick that came round late — the minute is too far off to sit
        // and wait for, and holding a broadcast open the better part of one is not
        // something it would survive. The alarm comes back in the margin before it.
        if (target - System.currentTimeMillis() > LEAD_MS + WINDOW_MS) return

        val moment = at(target)
        val stamp = moment.third
        val up = onScreen(context)
        val designs = up.filter { moves(context, it, target) }
        android.util.Log.i(
            "VisionGallery",
            "flip for ${moment.second} — up=${up.map { it.id }} moving=${designs.map { it.id }}"
        )

        designs.forEach { design ->
            // No point starting one that cannot land before the minute does; it would
            // only hold the broadcast open past its welcome. The ones left behind are
            // caught by the fallback below.
            if (System.currentTimeMillis() >= target) return@forEach
            runCatching {
                DesignRenderer.refresh(
                    context,
                    design,
                    design.widgetWidthPx(context),
                    design.widgetHeightPx(context),
                    at = moment.first to moment.second,
                    into = DesignRenderer.pendingFile(context, design, stamp)
                )
            }
        }

        // Held until the minute actually turns.
        val wait = target - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        shownMinute = target

        designs.forEach { design ->
            val promoted = DesignRenderer.promote(context, design, stamp)
            android.util.Log.i(
                "VisionGallery",
                "flipped ${design.id} promoted=$promoted late=${System.currentTimeMillis() - target}ms"
            )
            if (promoted) {
                runCatching { widgetFor(design).updateAll(context) }
            } else {
                // It never got drawn in time. Asked for in the background rather than
                // here, so a slow design holds up the broadcast no further than this.
                requestPaint(context, design)
            }
        }
    }

    /** Puts the minute that is already showing right, without waiting on anything. */
    private fun catchUp(context: Context, minute: Long) {
        shownMinute = minute
        onScreen(context)
            .filter { moves(context, it, minute) }
            .forEach { requestPaint(context, it) }
    }

    /**
     * Whether [design] reads differently at [target] than it does in the picture it has.
     *
     * A clock moves every minute. A calendar or a date moves once a day, and drawing one
     * sixty times an hour to arrive at the same picture is a waste of the battery — so
     * those are only redrawn when the day under them has turned.
     */
    private fun moves(context: Context, design: GalleryWidget, target: Long): Boolean {
        if (design.category.equals("clock", ignoreCase = true)) return true
        val file = DesignRenderer.fileFor(context, design)
        if (!file.exists()) return true
        return dayOf(file.lastModified()) != dayOf(target)
    }

    private fun dayOf(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    /** [target] as the designs take it: the date, the time, and a name for the minute. */
    private fun at(target: Long): Triple<String, String, String> {
        val moment = Instant.ofEpochMilli(target).atZone(ZoneId.systemDefault())
        return Triple(
            moment.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            moment.format(DateTimeFormatter.ofPattern("HH:mm")),
            moment.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
        )
    }

    /** The designs with a widget of their own actually on a screen. */
    private fun onScreen(context: Context): List<GalleryWidget> {
        val manager = AppWidgetManager.getInstance(context) ?: return emptyList()
        return GalleryCatalog.load(context).filter { design ->
            runCatching {
                manager.getAppWidgetIds(ComponentName(context, receiverFor(design))).isNotEmpty()
            }.getOrDefault(false)
        }
    }

    /**
     * The painting of [design], started if it is not already going.
     *
     * Run in the ticker's own scope rather than the caller's, so a widget that gives up
     * waiting does not take the painting down with it — the picture it wanted still
     * arrives, and the widget is told about it.
     */
    private fun paintJob(context: Context, design: GalleryWidget): Deferred<Boolean> {
        val app = context.applicationContext
        return inFlight.computeIfAbsent(design.id) {
            scope.async {
                try {
                    val painted = runCatching {
                        DesignRenderer.refresh(
                            app,
                            design,
                            design.widgetWidthPx(app),
                            design.widgetHeightPx(app)
                        )
                    }.getOrDefault(false)
                    // Only when there is something new to show. A design that would not
                    // settle leaves the widget as it was rather than asking it to redraw
                    // the same nothing.
                    if (painted) runCatching { widgetFor(design).updateAll(app) }
                    painted
                } finally {
                    inFlight.remove(design.id)
                }
            }
        }
    }

    /** Paints [design] away from whatever asked for it, without waiting on the result. */
    fun requestPaint(context: Context, design: GalleryWidget) {
        paintJob(context, design)
    }

    /**
     * Paints [design] and waits up to [timeoutMs] for it; true if it got there in time.
     *
     * This is what lets a widget come up as itself rather than as its frozen preview.
     * A design placed on its own takes a second or two, which a widget's update can
     * afford to wait out — what it cannot afford is waiting without a limit, which is
     * how a round of updates all queued behind one view and came up empty. Past the
     * limit the painting carries on without the waiter and the widget is told when it
     * lands.
     */
    suspend fun paintWithin(context: Context, design: GalleryWidget, timeoutMs: Long): Boolean {
        val job = paintJob(context, design)
        return withTimeoutOrNull(timeoutMs) { job.await() } ?: false
    }


    /** Repaints everything — the day has turned, so the calendars have moved too. */
    fun redrawAll(context: Context, done: () -> Unit = {}) {
        val app = context.applicationContext
        scope.launch {
            try {
                repaint(app) { true }
            } finally {
                done()
            }
        }
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
            val component = ComponentName(context, receiverFor(design))
            val ids = runCatching { manager.getAppWidgetIds(component) }
                .getOrDefault(IntArray(0))
            if (ids.isEmpty()) return@forEach

            runCatching {
                val painted = DesignRenderer.refresh(
                    context,
                    design,
                    design.widgetWidthPx(context),
                    design.widgetHeightPx(context)
                )
                android.util.Log.i("GalleryTick", "${design.id} painted=$painted ids=${ids.size}")

                // Asked of the design's own widget class, which is how the clock that
                // worked before any of this always did it. Glance goes by the class, so
                // this reaches that design's widgets and no others.
                widgetFor(design).updateAll(context)
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
                val pending = goAsync()
                GalleryTicker.schedule(context)
                GalleryTicker.redrawAll(context) { pending.finish() }
            }

            Intent.ACTION_BOOT_COMPLETED -> GalleryTicker.schedule(context)

            else -> {
                android.util.Log.i("GalleryTick", "tick received")
                // Held open until the painting is done. A receiver that returns first
                // is a receiver the system may stop the app behind.
                val pending = goAsync()
                GalleryTicker.tick(context) { pending.finish() }
            }
        }
    }
}
