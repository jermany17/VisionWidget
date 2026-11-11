package app.visionov.visionwidget.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

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
     * Where [file] can be read from, as an address the launcher may use, or null if
     * there is no picture there yet.
     *
     * One address per design, for good — the plain name of the file, with nothing on the
     * end of it to say which minute is in it.
     *
     * Writing the minute in was the obvious thing, and it was wrong twice over. An
     * address that moves every minute is an address that has to be granted every minute,
     * and the one before it let go of; but a revoke matches on the path and pays no
     * attention to what follows, so letting go of last minute's address takes every grant
     * on that path with it — including the one just made. Granting after revoking saved
     * that much. What it could not save is the views already handed over: the launcher
     * re-applies the last ones it holds whenever it likes, on a scroll or a screen waking,
     * and those name an address that has since been revoked. That is a cell that goes
     * blank long after a perfectly good minute, and fills again on its own when the next
     * update arrives — which is exactly how it behaved.
     *
     * So nothing is ever revoked, and for that to be affordable there is only ever one
     * address to grant. The picture behind it still changes every minute: an Icon pointing
     * at a URI is decoded afresh each time the views are applied, so the file is re-read
     * rather than remembered. [DesignRenderer.promote] moving the new minute into place
     * under the same name is the whole of the update.
     */
    fun uriFor(context: Context, file: File): Uri? {
        if (!file.exists()) return null

        val uri = runCatching {
            FileProvider.getUriForFile(context, authority(context), file)
        }.getOrElse {
            Log.w("VisionSlot", "no address for ${file.name}", it)
            return null
        }

        grantToHost(context, uri)
        return uri
    }

    /**
     * Lets whoever draws the home screen read [uri], and never takes it back.
     *
     * Recent Android walks a widget's views for the addresses in them and grants the host
     * what it needs by itself, but that cannot be counted on as far back as this app runs,
     * and a grant that was not needed costs nothing. Granting the same address again costs
     * nothing either, which is what makes leaving it in place affordable: there is one of
     * these per design on the screen, not one per minute it has been up.
     */
    private fun grantToHost(context: Context, uri: Uri) {
        val host = hostPackage(context) ?: return
        runCatching {
            context.grantUriPermission(host, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure { Log.w("VisionSlot", "grant failed for $uri", it) }
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
