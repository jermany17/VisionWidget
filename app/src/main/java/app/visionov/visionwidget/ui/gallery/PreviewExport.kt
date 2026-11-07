package app.visionov.visionwidget.ui.gallery

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.util.Log
import java.io.File

/**
 * Writes the pictures the launcher's widget picker shows.
 *
 * A Glance widget has no static layout to draw a preview from, so one has to be supplied
 * as a PNG — and a PNG is a frozen moment, which is why the Gallery designs are painted
 * for it at [PREVIEW_MOMENT] rather than at whatever the clock happens to say. The shelf
 * shows the same moment, so the picture in the picker and the tile it was picked from
 * agree.
 *
 * Kept in the app rather than done by hand because the previews have to be redone every
 * time a design changes, and a step nobody can repeat is a step that quietly rots. It is
 * fenced behind a debug build and an explicit extra: nothing reaches it by accident.
 *
 * To redo them:
 * ```
 * adb shell am start -n app.visionov.visionwidget/.MainActivity --ez export_gallery_previews true
 * adb pull /sdcard/Android/data/app.visionov.visionwidget/files/previews
 * ```
 * then copy the PNGs into `res/drawable-nodpi/`.
 */
object PreviewExport {

    /** Read on MainActivity's intent. Does nothing outside a debug build. */
    const val EXTRA = "export_gallery_previews"

    /**
     * How wide a preview is drawn, and how hard it is squeezed.
     *
     * Wide enough for the picker, which is not the thumbnail it sounds like: on a dense
     * screen it draws a 2x2 at some eight hundred pixels across, and a picture half that
     * is visibly soft against the widget it is offering. These are written as WebP
     * instead of PNG, which is what makes the resolution affordable — most of them are
     * photographs, and PNG stores those very badly.
     */
    private const val SMALL_WIDTH = 640
    private const val MEDIUM_WIDTH = 840
    private const val QUALITY = 88

    suspend fun run(context: Context) {
        val debuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) return

        val catalog = GalleryCatalog.load(context)
        val folder = File(context.getExternalFilesDir(null), "previews").apply { mkdirs() }

        var done = 0
        for (design in catalog) {
            val width = if (design.size == "medium") MEDIUM_WIDTH else SMALL_WIDTH
            val height = (width / design.ratio).toInt()
            val bitmap = DesignRenderer.render(context, design, width, height, frozen = true)
            if (bitmap == null) {
                Log.w("PreviewExport", "could not paint ${design.id}")
                continue
            }

            // The name the generated provider expects to find it under. The extension
            // is not part of a drawable's name, so this stays `preview_gallery_<id>`
            // however it is encoded.
            val file = File(folder, "preview_gallery_${design.resourceName}.webp")
            val format =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            file.outputStream().use { bitmap.compress(format, QUALITY, it) }
            done++
        }
        Log.i("PreviewExport", "wrote $done of ${catalog.size} to ${folder.absolutePath}")
    }
}
