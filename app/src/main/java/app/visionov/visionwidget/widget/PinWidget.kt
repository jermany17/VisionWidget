package app.visionov.visionwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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

/** The size it was asked for, in case the placed widget cannot yet be asked its own. */
private const val EXTRA_SIZE = "size"

/** The one request code every pin callback is registered under. */
private const val PIN_REQUEST = 0x5107

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
        // One callback for all of them, rewritten each time, rather than one per slot.
        //
        // A code per slot was meant to keep a second ask from overwriting the first,
        // and did the opposite: each left a live callback behind, and the launcher
        // fired more than one at the next placement. Measured — a single widget was
        // told it held a date design and then a clock, eighteen milliseconds apart,
        // and which of them stuck was down to the order they happened to land in.
        //
        // Only one pin can be in flight as far as the user is concerned, so there is
        // only one callback, and asking again replaces it.
        PIN_REQUEST,
        Intent(context, WidgetPinnedReceiver::class.java)
            .setAction(ACTION_PINNED)
            .putExtra(EXTRA_SLOT, slot.encode())
            .putExtra(EXTRA_SIZE, size.name),
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
        // Said out loud when it fails. A preview that cannot be drawn is not worth
        // refusing the pin over — the dialog falls back to the provider's own picture
        // and the widget still lands — but it is exactly the failure that looks like
        // the launcher ignoring us, and the two want telling apart.
        val views = runCatching { previewViewsOf(app, slot, size) }
            .onFailure { Log.w("VisionPin", "preview for ${slot.encode()} failed", it) }
            .getOrNull()
        if (views == null) Log.w("VisionPin", "no preview for ${slot.encode()}")

        // Offered to the launcher, which may or may not take it.
        //
        // One UI does not. Measured on a device: the picture went across at the right
        // size, and so did one registered against the provider through setWidgetPreview
        // — the newer API meant for this — and the dialog drew its static previewImage
        // regardless. Nothing an app can send changes what One UI shows there, which is
        // why that image is a plain mark of the size rather than any one widget: it is
        // the only thing shown, so it had better not claim to be something else.
        //
        // The extra stays for the launchers that do read it.
        val extras = views?.let { bundleOf(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW to it) }
        val provider = ComponentName(app, SlotWidgets.receiverOf(size))

        // Three ways of offering One UI a picture have been tried on a device, and it
        // takes none of them: the extra above, a preview registered against the
        // provider through setWidgetPreview, and the same registered eight hundred
        // milliseconds ahead in case its cache was the problem. Its "add to home
        // screen" dialog draws the provider's static previewImage and nothing else.
        //
        // So that image is a plain mark of the size — see the provider xml. The design
        // the user picked is on the tile they tapped, directly behind the dialog, and
        // the widget itself lands as the real thing.

        runCatching {
            manager.requestPinAppWidget(provider, extras, callback)
        }.onFailure { Log.w("VisionPin", "pin request for ${slot.encode()} failed", it) }
    }
    return true
}

/**
 * The picture of what is about to be placed.
 *
 * Drawn the same way the library tile beside it is, so the dialog agrees with what was
 * tapped. Where it is handed to is [requestPinWidget]'s business — there are two places
 * to put it and launchers disagree about which they read.
 */
private suspend fun previewViewsOf(context: Context, slot: Slot, size: WidgetSize): RemoteViews? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

    val ratio = when (slot) {
        is Slot.Design -> GalleryCatalog.load(context)
            .firstOrNull { it.id == slot.designId }
            ?.let { it.widgetWidthPx(context).toFloat() / it.widgetHeightPx(context) }
            ?: return null

        else -> if (size == WidgetSize.Medium) 2f else 1f
    }
    val (widthPx, heightPx) = previewSize(ratio)

    val bitmap = when (slot) {
        is Slot.Design -> {
            val design = GalleryCatalog.load(context).firstOrNull { it.id == slot.designId }
            // Frozen, so the dialog agrees with the tile it was picked from rather than
            // showing a clock a minute out from the one beside it.
            design?.let { DesignRenderer.render(context, it, widthPx, heightPx, frozen = true) }
        }

        else -> renderCardPreview(context, slot, size, widthPx, heightPx)
    } ?: return null

    Log.i(
        "VisionPin",
        "preview for ${slot.encode()} at ${widthPx}x$heightPx (${bitmap.byteCount} bytes)"
    )

    return RemoteViews(context.packageName, R.layout.widget_pin_preview).apply {
        setImageViewBitmap(R.id.preview, bitmap)
    }
}

/**
 * How large the dialog's picture may be drawn.
 *
 * Not a width but a budget, because what matters is the bytes. A picture handed to the
 * launcher travels inside a Binder transaction with about a megabyte to spend on
 * everything in it, and RemoteViews keeps its own ceiling besides. Drawn at a flat six
 * hundred and forty across, a square design came to over a megabyte on its own — the
 * launcher could not inflate it, fell back to the provider's one picture per size, and
 * put up the Vision card for a calendar. It said nothing about why.
 *
 * Half a megabyte leaves room for the rest of the transaction and is still sharper than
 * the dialog draws it.
 */
private const val PREVIEW_BUDGET_BYTES = 160_000

private fun previewSize(ratio: Float): Pair<Int, Int> {
    // Four bytes a pixel at ARGB_8888, which is what both renderers draw into.
    val pixels = PREVIEW_BUDGET_BYTES / 4
    val width = kotlin.math.sqrt(pixels * ratio)
    return width.toInt().coerceAtLeast(1) to (width / ratio).toInt().coerceAtLeast(1)
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

        // The size the pin was asked for, since the widget may not be bound firmly
        // enough yet to be asked what it is.
        val size = SlotWidgets.sizeOf(app, appWidgetId)
            ?: WidgetSize.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_SIZE) }
            ?: return

        // The callback carries the id, which is all a GlanceId is — so it is taken off
        // the intent, the same way the setting screen takes its own.
        val glanceId = glanceIdFor(app, intent) ?: return

        // A pinned widget never saw the setting screen, so this is the only moment the
        // choice can be recorded.
        SlotWidgets.assign(app, glanceId, size, slot)

        // A design just arrived; if it is a clock the minute has to start running.
        if (slot is Slot.Design) GalleryTicker.schedule(app, known = true)
    }
}
