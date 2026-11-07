package app.visionov.visionwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.os.bundleOf
import app.visionov.visionwidget.R
import app.visionov.visionwidget.ui.gallery.DesignRenderer
import app.visionov.visionwidget.ui.gallery.GalleryCatalog
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

    val app = context.applicationContext
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

    // Painting the picture the dialog shows is a second's work, so the ask is made from
    // the ticker's scope rather than on the caller's thread. The answer the caller wants
    // is whether the launcher can be asked at all, which is already known.
    pinScope.launch {
        val extras = runCatching { previewOf(app, slot, size) }.getOrNull()
        runCatching {
            manager.requestPinAppWidget(
                ComponentName(app, SlotWidgets.receiverOf(size)),
                extras,
                callback
            )
        }
    }
    return true
}

/**
 * The picture the launcher's own dialog shows while it asks.
 *
 * Without one it falls back to the provider's `previewImage`, and there is a single
 * image per size now — so picking a date design out of the library and asking for it
 * put up a dialog showing the Vision card. This hands over the thing actually being
 * placed, drawn the same way the library tile beside it was.
 *
 * Only from Android 12, which is where the extra was added. Below that the dialog shows
 * the provider's picture and there is nothing to be done about it.
 */
private suspend fun previewOf(context: Context, slot: Slot, size: WidgetSize): Bundle? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

    val bitmap = when (slot) {
        is Slot.Design -> {
            val design = GalleryCatalog.load(context).firstOrNull { it.id == slot.designId }
            // Frozen, so the dialog agrees with the tile it was picked from rather than
            // showing a clock a minute out from the one beside it.
            design?.let {
                DesignRenderer.render(
                    context,
                    it,
                    it.widgetWidthPx(context),
                    it.widgetHeightPx(context),
                    frozen = true
                )
            }
        }

        else -> {
            val widthPx = PREVIEW_WIDTH_PX
            val heightPx = if (size == WidgetSize.Medium) widthPx / 2 else widthPx
            renderCardPreview(context, slot, size, widthPx, heightPx)
        }
    } ?: return null

    val views = RemoteViews(context.packageName, R.layout.widget_pin_preview).apply {
        setImageViewBitmap(R.id.preview, bitmap)
    }
    return bundleOf(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW to views)
}

/** Wide enough for the dialog, which draws it larger than a cell. */
private const val PREVIEW_WIDTH_PX = 640

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

        // Written straight away, before anything that can fail. The redraw below is
        // only to bring it forward — the widget reads this on whatever composition
        // comes next regardless.
        writeSlot(app, appWidgetId, slot)

        // Held open while the widget is redrawn: a receiver that returns first is one
        // the system may stop the app behind, leaving the cell empty until something
        // else happens to ask for it.
        val pending = goAsync()
        pinScope.launch {
            try {
                val glanceId = glanceIdOf(app, appWidgetId)
                val size = SlotWidgets.sizeOf(app, appWidgetId)
                if (glanceId != null && size != null) {
                    runCatching { SlotWidgets.of(size).update(app, glanceId) }
                }

                // A design just arrived; if it is a clock the minute has to start running.
                if (slot is Slot.Design) GalleryTicker.schedule(app)
            } finally {
                pending.finish()
            }
        }
    }
}
