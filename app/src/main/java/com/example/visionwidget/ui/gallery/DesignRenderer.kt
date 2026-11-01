package com.example.visionwidget.ui.gallery

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.util.LruCache
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import java.io.File

/**
 * Paints a design once and keeps the picture.
 *
 * The shelf used to carry a live web view per tile, which is what a design is, and that
 * is more than a scrolling list can afford: forty of them being built and torn down
 * under a finger took the app off the air entirely. A design on the shelf is a picture
 * of a widget rather than a working one, so it is drawn once into a bitmap and the
 * shelf shows that.
 *
 * What it costs is the second hand: a clock on the shelf shows the time it was drawn at
 * rather than ticking. That is the right trade for a page of thumbnails — the live one
 * is the widget on the home screen — and the same drawing is what a widget needs, so
 * this is also where that will come from.
 *
 * One view does all the painting, one design at a time. Building a web view is the
 * expensive part, and there is no reason to hold more than one.
 */
object DesignRenderer {

    /**
     * How long to wait on a design before giving up on it.
     *
     * Everything is local — no network — so a page that has not settled by now is not
     * going to. A tile with nothing in it is better than a shelf that never scrolls.
     */
    private const val TIMEOUT_MS = 8_000L

    /** How often to ask the page whether it is ready, and how long to let it paint after. */
    private const val READY_POLL_MS = 50L
    private const val SETTLE_MS = 120L

    /** A quarter of what the app is allowed, which is the usual share for a picture cache. */
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 4).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** One at a time: the view is shared, and a second page would land in the first. */
    private val lock = Mutex()
    private var painter: WebView? = null

    private fun keyOf(widget: GalleryWidget, widthPx: Int) = "${widget.id}@$widthPx"

    /**
     * The picture for [widget] at [widthPx] across, drawn if it hasn't been already.
     *
     * [frozen] holds the clock and the calendar at the moment every preview is shown at
     * — which is what a shelf wants, where a set of designs each saying a different time
     * reads as a fault. A widget on the home screen asks for the live one.
     *
     * Only a frozen picture is kept. A live one is a clock, and a cached clock is a
     * stopped one: keeping those is what left a widget showing the minute it was first
     * drawn at however often it was asked to redraw.
     */
    suspend fun render(
        context: Context,
        widget: GalleryWidget,
        widthPx: Int,
        heightPx: Int,
        frozen: Boolean = true
    ): Bitmap? {
        if (widthPx <= 0 || heightPx <= 0) return null
        if (!frozen) {
            return lock.withLock { paint(context, widget, widthPx, heightPx, frozen = false) }
        }

        val key = keyOf(widget, widthPx)
        cache.get(key)?.let { return it }

        return lock.withLock {
            // Checked again inside the lock: several tiles can ask for the same design
            // at once, and only the first should have to paint it.
            cache.get(key) ?: paint(context, widget, widthPx, heightPx, frozen = true)?.also {
                cache.put(key, it)
            }
        }
    }

    /**
     * Where a widget's picture is kept between updates.
     *
     * A widget update is a broadcast with a deadline, and painting a design takes a
     * second or two. One at a time that is fine; when the system asks every widget to
     * redraw at once — after an install, or a restart — they queue behind the one view
     * that does the painting and the later ones run out of time. So a widget reads a
     * picture off the disk, which is immediate, and the painting is done away from the
     * update that needed it.
     */
    fun fileFor(context: Context, widget: GalleryWidget): File =
        File(context.cacheDir, "gallery-widgets").apply { mkdirs() }
            .let { File(it, "${widget.resourceName}.png") }

    /** The picture last painted for [widget], or null if there has never been one. */
    fun cached(context: Context, widget: GalleryWidget): Bitmap? {
        val file = fileFor(context, widget)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()
    }

    /** How old a widget's picture may be before it is worth painting again. */
    fun isStale(context: Context, widget: GalleryWidget, maxAgeMs: Long): Boolean {
        val file = fileFor(context, widget)
        return !file.exists() || System.currentTimeMillis() - file.lastModified() > maxAgeMs
    }

    /**
     * Paints [widget] as it is now and keeps the picture for the widget to pick up.
     *
     * Returns whether anything was painted — a design that would not settle leaves the
     * last picture where it is rather than blanking the widget.
     */
    suspend fun refresh(
        context: Context,
        widget: GalleryWidget,
        widthPx: Int,
        heightPx: Int
    ): Boolean {
        val bitmap = render(context, widget, widthPx, heightPx, frozen = false) ?: return false
        return runCatching {
            fileFor(context, widget).outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            true
        }.getOrDefault(false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun paint(
        context: Context,
        widget: GalleryWidget,
        widthPx: Int,
        heightPx: Int,
        frozen: Boolean
    ): Bitmap? = withContext(Dispatchers.Main) {
        val view = painter ?: WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            settings.allowFileAccess = true
            setBackgroundColor(AndroidColor.TRANSPARENT)
            // Drawn into a bitmap rather than onto the screen, which a hardware layer
            // cannot be read back from.
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }.also { painter = it }

        // Sized before the page is asked for, not after it has arrived. A view that is
        // still nothing across while a page loads gives that page nothing to lay itself
        // out in, and what comes back is an empty frame — which is how the first design
        // of a run came out blank while every one after it, inheriting the size of the
        // one before, was fine.
        view.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, widthPx, heightPx)

        // The page is laid out at the width the design was drawn for, and this is what
        // that width has to come out as. The two are set apart so a design keeps its own
        // proportions whatever size it is being shown at.
        view.setInitialScale(
            (widthPx.toFloat() / widget.viewportWidth * 100f).roundToInt().coerceAtLeast(1)
        )

        val loaded = CompletableDeferred<Unit>()
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(page: WebView, url: String) {
                loaded.complete(Unit)
            }
        }
        view.loadUrl(if (frozen) widget.previewUrl else widget.url)

        val settled = withTimeoutOrNull(TIMEOUT_MS) {
            loaded.await()
            // The page is laid out before it is painted, and the fonts and photographs
            // it draws with arrive later still. Asking it when it is ready is more
            // honest than guessing at a delay.
            awaitPainted(view)
        } != null
        if (!settled) return@withContext null

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        bitmap
    }

    /**
     * Waits until the page has everything it draws with.
     *
     * The page being finished is not the same as the page being ready: the type and the
     * photographs arrive after it, and a design copied off before they land is the
     * backdrop and nothing else — which is exactly how the silk one came out. So the
     * page is asked, rather than guessed at, and only once it says it has the lot is the
     * frame given a moment to be painted.
     */
    private suspend fun awaitPainted(view: WebView) {
        while (!isReady(view)) {
            kotlinx.coroutines.delay(READY_POLL_MS)
        }
        kotlinx.coroutines.delay(SETTLE_MS)
    }

    /** Whether the page has its type and its pictures and has finished laying out. */
    private suspend fun isReady(view: WebView): Boolean {
        val answer = CompletableDeferred<String?>()
        view.evaluateJavascript(
            """
            (function(){
              if (document.readyState !== 'complete') return 'false';
              if (document.fonts && document.fonts.status !== 'loaded') return 'false';
              var images = Array.prototype.slice.call(document.images);
              return String(images.every(function(i){ return i.complete && i.naturalWidth > 0; }));
            })()
            """.trimIndent()
        ) { answer.complete(it) }
        return answer.await()?.trim('"') == "true"
    }
}
