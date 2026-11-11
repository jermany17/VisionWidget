package app.visionov.visionwidget.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * How a widget's picture reaches the launcher.
 *
 * Not as pixels. A widget's views travel inside a Binder transaction, and the megabyte
 * that buffer holds is shared by every transaction the process has in flight at once
 * rather than given to each. Three clocks turning on the same minute each handed over
 * their own bitmap within thirty milliseconds of the last, and six hundred and forty
 * thousand bytes apiece came to nearly twice what the buffer holds. Whatever overflowed
 * was not reported: those views arrived empty and the cell went transparent. That is the
 * blank that kept coming back however small a single picture was made — the cost was
 * never one widget's, it was all of them together, so it grew with every clock added.
 *
 * So the pixels stay on the disk and the launcher is handed the address of them. The
 * transaction carries a string, the file is read by whoever is drawing it, and how many
 * widgets are on the screen stops mattering.
 */
object WidgetPictures {

    /** Matches the provider's authority in the manifest. */
    private fun authority(context: Context) = "${context.packageName}.pictures"

    /**
     * How many addresses may pile up for one picture before they are swept.
     *
     * Each minute's address is granted and never taken back, so they gather at one an
     * hour's sixtieth. Two hours' worth is a small enough pile to leave lying in the
     * system, and sweeping only that rarely keeps the one risky moment rare — see
     * [sweep].
     */
    private const val GRANTS_BEFORE_SWEEP = 120

    /** How many addresses have been handed out for each picture since it was last swept. */
    private val handedOut = ConcurrentHashMap<String, Int>()

    /**
     * Where [file] can be read from, as an address the launcher may use, or null if
     * there is no picture there yet.
     *
     * The file's own moment is written onto the end, so each minute has an address of its
     * own. That is not decoration: handed the same address twice the launcher draws what
     * it decoded the first time, however new the file behind it is. The views were applied
     * — the host said so — and the cell still showed the minute before, because nothing in
     * what it was given had changed.
     *
     * Moving the address and letting go of the old one are separate things, and only the
     * second was ever the trouble. A revoke matches on the path and pays no attention to
     * what follows it, so releasing last minute's address takes every grant on that path
     * with it; and even granting afterwards cannot save the views already handed over,
     * which go on naming an address that no longer works. That is a cell going blank well
     * after a good minute and filling again by itself on the next update.
     *
     * So the address moves and nothing is released. Every minute's address stays good,
     * which means views of any age still draw — a stale one shows a stale minute rather
     * than nothing at all.
     */
    fun uriFor(context: Context, file: File): Uri? {
        if (!file.exists()) return null

        val base = runCatching {
            FileProvider.getUriForFile(context, authority(context), file)
        }.getOrElse {
            Log.w("VisionSlot", "no address for ${file.name}", it)
            return null
        }

        val uri = base.buildUpon()
            .appendQueryParameter("v", file.lastModified().toString())
            .build()
        grantToHost(context, file.name, base, uri)
        return uri
    }

    /**
     * Lets whoever draws the home screen read [uri], and does not take it back.
     *
     * Recent Android walks a widget's views for the addresses in them and grants the host
     * what it needs by itself, but that cannot be counted on as far back as this app runs,
     * and a grant that was not needed costs nothing.
     */
    private fun grantToHost(context: Context, key: String, base: Uri, uri: Uri) {
        val host = hostPackage(context) ?: return
        runCatching {
            val count = handedOut.merge(key, 1, Int::plus) ?: 1
            if (count > GRANTS_BEFORE_SWEEP) {
                sweep(context, key, base)
            }
            context.grantUriPermission(host, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure { Log.w("VisionSlot", "grant failed for $key", it) }
    }

    /**
     * Drops the addresses that have gathered for one picture.
     *
     * A revoke takes the whole path at once, which is the only reason this can be done at
     * all — and the reason it has to be done here rather than a minute at a time. What
     * follows it in [grantToHost] is the grant for the address actually in use, so the
     * gap in which the live one is missing is the few instructions between the two. Every
     * couple of hours, for that long, is a risk worth taking against letting a grant per
     * minute pile up for as long as the phone is on.
     */
    private fun sweep(context: Context, key: String, base: Uri) {
        runCatching {
            context.revokeUriPermission(base, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            handedOut[key] = 1
        }
    }

    /** Whichever app is drawing the home screen, which is the one holding our widgets. */
    private fun hostPackage(context: Context): String? = runCatching {
        context.packageManager
            .resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY
            )
            ?.activityInfo
            ?.packageName
    }.getOrNull()
}
