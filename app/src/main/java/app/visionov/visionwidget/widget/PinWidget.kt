package app.visionov.visionwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The action the launcher's confirmation comes back on. */
private const val ACTION_PINNED = "app.visionov.visionwidget.WIDGET_PINNED"

/** What the widget about to be placed should hold, carried on the callback. */
private const val EXTRA_SLOT = "slot"

private val pinScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Asks the launcher to put a widget of [size] on the home screen, holding [slot].
 *
 * Declaring a widget only ever offers it in the launcher's own picker; it takes the user
 * going there and dragging it out for one to actually land. This asks on their behalf
 * instead, so a design can be put up from the shelf it was chosen on.
 *
 * The launcher answers with its own confirmation, and may decline to be asked at all —
 * a few don't support being asked, and there's nothing to be done about that but say so.
 * False means the ask never happened.
 *
 * What the widget is to hold cannot be settled here, because the widget does not exist
 * yet: a pinned widget skips the setting screen the picker would have opened, so nothing
 * would ever write its state. The answer is the success callback — the launcher sends it
 * once the widget is placed, with the id it was given, and [WidgetPinnedReceiver] writes
 * the slot then.
 */
fun requestPinWidget(context: Context, size: WidgetSize, slot: Slot): Boolean {
    val manager = AppWidgetManager.getInstance(context) ?: return false
    if (!manager.isRequestPinAppWidgetSupported) return false

    val callback = PendingIntent.getBroadcast(
        context,
        // One per slot, so asking for a second design while the first is still pending
        // does not rewrite the first one's callback out from under it.
        slot.encode().hashCode(),
        Intent(context, WidgetPinnedReceiver::class.java)
            .setAction(ACTION_PINNED)
            .putExtra(EXTRA_SLOT, slot.encode()),
        // Mutable because the system is the one filling in EXTRA_APPWIDGET_ID — an
        // immutable callback comes back without it, which is a widget placed and never
        // told what it is. Safe to leave open: the component is named explicitly, so
        // there is nowhere else for it to be sent.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )

    return runCatching {
        manager.requestPinAppWidget(
            ComponentName(context, SlotWidgets.receiverOf(size)),
            null,
            callback
        )
    }.getOrDefault(false)
}

/**
 * Takes the launcher's word that a widget asked for from inside the app has landed, and
 * tells it what it is holding.
 *
 * This is the one path where a widget arrives without the setting screen having run, so
 * it is the one place the slot has to be written after the fact rather than before.
 */
class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PINNED) return

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val slot = Slot.decode(intent.getStringExtra(EXTRA_SLOT))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || slot == null) return

        val app = context.applicationContext
        // Held open while the state is written: a receiver that returns first is one the
        // system may stop the app behind, leaving the widget blank until something else
        // happens to redraw it.
        val pending = goAsync()
        pinScope.launch {
            try {
                val glanceId = glanceIdOf(app, appWidgetId) ?: return@launch
                writeSlot(app, glanceId, slot)

                val size = SlotWidgets.sizeOf(app, appWidgetId) ?: return@launch
                runCatching { SlotWidgets.of(size).update(app, glanceId) }

                // A design just arrived; if it is a clock the minute has to start running.
                if (slot is Slot.Design) GalleryTicker.schedule(app)
            } finally {
                pending.finish()
            }
        }
    }
}
