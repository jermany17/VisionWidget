package app.visionov.visionwidget.ui.gallery

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
    private const val LOAD_MS = 5_000L

    /**
     * How long to let the type and the photographs arrive after the page has.
     *
     * Running out here is not a failure — whatever the design has by then is drawn.
     */
    private const val READY_MS = 2_500L

    /** How often to ask the page whether it is ready, and how long to let it paint after. */
    private const val READY_POLL_MS = 50L
    private const val SETTLE_MS = 120L

    /**
     * Where the widget pictures are kept, and which generation of them.
     *
     * The generation is in the name so that a build which fixes how the pictures are
     * drawn does not go on showing the ones drawn wrongly. A picture is kept on the
     * disk and read back without question, and reinstalling does not clear the cache —
     * so a design that was once written with another design's face would keep that face
     * for good. Bumping this starts everyone clean.
     */
    private const val CACHE_DIR = "gallery-widgets-v3"

    /** Whether the older generations have been cleared out this run. */
    @Volatile
    private var sweptOldCaches = false

    /** A quarter of what the app is allowed, which is the usual share for a picture cache. */
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 4).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** One at a time, so there is never more than one web view alive at once. */
    private val lock = Mutex()

    /** How many painters are drawing something a person is looking at right now. */
    private val onScreen = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * Whether the shelf is waiting on a picture.
     *
     * There is one view that paints, so anything drawn in the background is drawn in
     * front of the tile under the user's finger. A sweep asks this and stands aside.
     */
    fun isBusyOnScreen(): Boolean = onScreen.get() > 0

    private suspend fun <T> forScreen(block: suspend () -> T): T {
        onScreen.incrementAndGet()
        try {
            return block()
        } finally {
            onScreen.decrementAndGet()
        }
    }

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
            return forScreen {
                lock.withLock { paint(context, widget, widthPx, heightPx, frozen = false) }
            }
        }

        val key = keyOf(widget, widthPx)
        cache.get(key)?.let { return it }

        return forScreen {
            lock.withLock {
                // Checked again inside the lock: several tiles can ask for the same
                // design at once, and only the first should have to paint it.
                cache.get(key) ?: paint(context, widget, widthPx, heightPx, frozen = true)?.also {
                    cache.put(key, it)
                }
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
        File(cacheDir(context), "${widget.resourceName}.png")

    /**
     * The picture directory for this generation, with the older ones cleared out.
     *
     * Done once per run rather than per picture: there is one widget per design, and a
     * round of updates would otherwise walk the cache forty-odd times over.
     */
    private fun cacheDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        if (!sweptOldCaches) {
            sweptOldCaches = true
            runCatching {
                context.cacheDir.listFiles()
                    ?.filter {
                        it.isDirectory &&
                            it.name.startsWith("gallery-widgets") &&
                            it.name != CACHE_DIR
                    }
                    ?.forEach { it.deleteRecursively() }
            }
        }
        return dir
    }

    /** The picture last painted for [widget], or null if there has never been one. */
    fun cached(context: Context, widget: GalleryWidget): Bitmap? {
        val file = fileFor(context, widget)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()
    }

    /**
     * Whether there is a picture for [widget] at all.
     *
     * Asked before a sweep decides to draw one, so the answer must not cost what
     * reading the picture back would.
     */
    fun hasPicture(context: Context, widget: GalleryWidget): Boolean =
        fileFor(context, widget).exists()

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
        heightPx: Int,
        /**
         * The moment to draw, as `yyyy-MM-dd` and `HH:mm`, or null for now.
         *
         * A clock is painted for the minute that is coming rather than the one showing,
         * so that when it arrives the picture is already waiting. Painting on the stroke
         * of the minute instead puts the whole cost of drawing — a web view, a photo, a
         * typeface — between the minute turning and the widget saying so.
         */
        at: Pair<String, String>? = null,
        /** Where to leave it. The live picture unless it is being painted ahead. */
        into: File = fileFor(context, widget)
    ): Boolean {
        val url = at?.let { (date, time) -> widget.urlAt(date, time) } ?: widget.url
        // Under the same lock as every other painter: the view is shared, and a tick
        // landing while the shelf is painting its own tile would otherwise land in it.
        val bitmap = lock.withLock { paintAt(context, widget, widthPx, heightPx, url) }
            ?: return false
        val kept = runCatching {
            into.outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            true
        }.getOrDefault(false)
        android.util.Log.i(
            "VisionGallery",
            "paint ${widget.id} ${widthPx}x${heightPx} -> ${into.name} kept=$kept url=$url"
        )
        return kept
    }

    /**
     * Where a picture painted before its minute waits for it.
     *
     * [stamp] is the minute it was drawn for, so a picture can only ever be put up for
     * the moment it actually shows — one left behind by a tick that was interrupted is
     * not mistaken for the one due now.
     */
    fun pendingFile(context: Context, widget: GalleryWidget, stamp: String): File =
        File(cacheDir(context), "${widget.resourceName}.next.$stamp.png")

    /**
     * Puts the picture painted for [stamp] in place, if it got there in time.
     *
     * This is the whole of what happens when the minute turns: a rename, which is as
     * near to instant as the disk gets. The drawing — a web view, a photograph, a
     * typeface, a second or two of it — was done while the minute before was still
     * showing, so nothing stands between the clock changing and the widget saying so.
     */
    fun promote(context: Context, widget: GalleryWidget, stamp: String): Boolean {
        val pending = pendingFile(context, widget, stamp)
        if (!pending.exists()) return false
        val live = fileFor(context, widget)
        val moved = runCatching {
            if (live.exists()) live.delete()
            pending.renameTo(live)
        }.getOrDefault(false)
        if (moved) clearPending(context, widget, keep = null)
        return moved
    }

    /** Throws away anything painted ahead for [widget] other than [keep]. */
    fun clearPending(context: Context, widget: GalleryWidget, keep: String?) {
        val prefix = "${widget.resourceName}.next."
        val kept = keep?.let { "$prefix$it.png" }
        runCatching {
            cacheDir(context).listFiles()?.forEach { file ->
                if (file.name.startsWith(prefix) && file.name != kept) file.delete()
            }
        }
    }

    private suspend fun paint(
        context: Context,
        widget: GalleryWidget,
        widthPx: Int,
        heightPx: Int,
        frozen: Boolean
    ): Bitmap? = paintAt(
        context, widget, widthPx, heightPx,
        if (frozen) widget.previewUrl else widget.url
    )

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun paintAt(
        context: Context,
        widget: GalleryWidget,
        widthPx: Int,
        heightPx: Int,
        url: String
    ): Bitmap? = withContext(Dispatchers.Main) {
        // A view of its own for every painting, thrown away at the end of it.
        //
        // Sharing one view is what put another design's face on a widget, and no
        // arrangement of checks around it was ever going to be enough: a page reports
        // itself finished more than once, so the design painted before this one could
        // answer this one's wait while still being the page on screen — and then be
        // drawn into this design's picture. Checking which page was loaded guarded the
        // common way that happened and cost us every design that would not answer.
        //
        // A view built for one page cannot be showing another. That ends it.
        val view = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
            settings.allowFileAccess = true
            setBackgroundColor(AndroidColor.TRANSPARENT)
            // Drawn into a bitmap rather than onto the screen, which a hardware layer
            // cannot be read back from.
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        try {
            // Sized before the page is asked for, not after it has arrived. A view that
            // is still nothing across while a page loads gives that page nothing to lay
            // itself out in, and what comes back is an empty frame.
            view.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, widthPx, heightPx)

            // The page is laid out at the width the design was drawn for, and this is
            // what that width has to come out as. The two are set apart so a design
            // keeps its own proportions whatever size it is being shown at.
            view.setInitialScale(
                (widthPx.toFloat() / widget.viewportWidth * 100f).roundToInt().coerceAtLeast(1)
            )

            val loaded = CompletableDeferred<Unit>()
            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(page: WebView, url: String) {
                    loaded.complete(Unit)
                }
            }
            view.loadUrl(url)

            // Neither wait is worth failing over now. Whatever is in this view is this
            // design and nothing else, so running out of patience costs a little of the
            // drawing at worst — where before it cost the whole cell. The designs that
            // came up empty were the ones that never answered one of these.
            withTimeoutOrNull(LOAD_MS) { loaded.await() }
            withTimeoutOrNull(READY_MS) { awaitPainted(view) }

            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            bitmap
        } finally {
            runCatching { view.destroy() }
        }
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
              // Finished, not necessarily arrived. A photograph that failed to load is
              // complete with no width of its own and never will have one, so asking
              // for a width here is asking for something that cannot come — and the
              // whole design goes undrawn waiting on it.
              var images = Array.prototype.slice.call(document.images);
              return String(images.every(function(i){ return i.complete; }));
            })()
            """.trimIndent()
        ) { answer.complete(it) }
        return answer.await()?.trim('"') == "true"
    }
}
