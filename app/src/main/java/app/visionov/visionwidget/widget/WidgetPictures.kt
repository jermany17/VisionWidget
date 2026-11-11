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
     * The last address handed out for each picture, so the one before it can be let go.
     *
     * A clock is promoted into place every minute of the day and each minute is a new
     * address. Granting without releasing would leave fourteen hundred of them behind by
     * nightfall, all held in the system on our behalf.
     */
    private val granted = ConcurrentHashMap<String, Uri>()

    /**
     * Where [file] can be read from, as an address the launcher may use, or null if
     * there is no picture there yet.
     *
     * The file's own moment is written into the address because the launcher will not
     * look twice at one it has already seen. A picture is promoted into place under a
     * fixed name, so the name alone never changes; an ImageView handed the same address
     * again keeps the picture it had, and the clock would stand still at the minute it
     * was first given. The moment moves, so the address moves, so the new minute is
     * actually read off the disk.
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
     * Lets whoever draws the home screen read [uri].
     *
     * Recent Android walks a widget's views for the addresses in them and grants the host
     * what it needs by itself, but that cannot be counted on as far back as this app
     * runs, and a grant that was not needed costs nothing.
     *
     * Last minute's address is released *before* this minute's is granted, and the order
     * is the whole point. A revoke matches on the address's path and pays no attention to
     * the moment on the end of it, so releasing the old one afterwards took the new one
     * with it — the grant was gone before the launcher ever read it, and every cell
     * holding a clock went blank with a Permission Denial behind it. Granted last, it
     * survives.
     *
     * Both the bare address and the stamped one are granted, because whether a grant is
     * matched on the whole address or on its path alone is not worth guessing at when a
     * picture failing to appear is the bug being fixed. The bare one never changes, so
     * granting it again costs nothing; the stamped one is what replaces its predecessor,
     * which keeps the count to one per design rather than one per minute of the day.
     */
    private fun grantToHost(context: Context, key: String, base: Uri, uri: Uri) {
        val host = hostPackage(context) ?: return
        runCatching {
            granted.remove(key)
                ?.takeIf { it != uri }
                ?.let { context.revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            context.grantUriPermission(host, base, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.grantUriPermission(host, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            granted[key] = uri
        }.onFailure { Log.w("VisionSlot", "grant failed for $key", it) }
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
