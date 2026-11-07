package app.visionov.visionwidget.ui.gallery

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.util.Log
import app.visionov.visionwidget.ui.theme.WidgetStyle
import app.visionov.visionwidget.widget.MilestoneLine
import app.visionov.visionwidget.widget.renderVisionCard
import app.visionov.visionwidget.widget.renderVisionCardLarge
import app.visionov.visionwidget.widget.renderVisionCardSmall
import java.io.File

/**
 * Writes the three pictures the launcher's widget picker shows.
 *
 * A Glance widget has no static layout to draw a preview from, so one has to be supplied
 * as a PNG. There are three providers — one per size — so there are three of these, and
 * each is the Vision card, which is what a cell most often ends up holding.
 *
 * Everything in them is invented. A preview is shown before a widget exists and to
 * people who may not have written anything yet, so drawing it from the user's own words
 * would leave the picker offering a blank card on a fresh install. The sample is fixed
 * in [SampleGoal] and the lines beside it: the dates are written out rather than worked
 * out from a target, so the picture never drifts as the weeks pass.
 *
 * This used to export the Gallery designs instead, one per design, back when each had a
 * provider of its own. Those are no longer in the picker at all — a design is picked in
 * our own setting screen now, which draws it live.
 *
 * Kept in the app rather than done by hand because the previews have to be redone every
 * time the card design changes, and a step nobody can repeat is a step that quietly
 * rots. It is fenced behind a debug build and an explicit extra: nothing reaches it by
 * accident.
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
     * How wide a preview is drawn.
     *
     * Not the thumbnail it sounds like: on a dense screen the picker draws a 2x2 at some
     * eight hundred pixels across, and a picture half that is visibly soft against the
     * widget it is offering.
     */
    private const val SMALL_SIDE = 640
    private const val MEDIUM_WIDTH = 840
    private const val LARGE_SIDE = 840

    // The card the picker shows. Invented, and written out rather than computed — see
    // the note above on why none of this comes from the user or from the clock.
    private const val SampleGoal = "Harvard Law"
    private const val SampleWhy = "Become the lawyer I promised myself I would be."
    private const val SampleTarget = "1 DECEMBER 2028"
    private const val SampleWeeks = "121 WEEKS LEFT"
    private val SampleMilestones = listOf(
        MilestoneLine(step = "LSAT 175+", checked = true, due = "14 MAR 27"),
        MilestoneLine(step = "Applications in", checked = false, due = "10 OCT 27"),
        MilestoneLine(step = "Offer", checked = false, due = "1 DEC 28")
    )

    /** The default look, not the user's — a preview is the same for everyone. */
    private val style = WidgetStyle()

    suspend fun run(context: Context) {
        val debuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) return

        val folder = File(context.getExternalFilesDir(null), "previews").apply { mkdirs() }

        val cards = listOf(
            "preview_vision_small" to renderVisionCardSmall(
                context = context,
                sidePx = SMALL_SIDE,
                style = style,
                photoUri = null,
                goal = SampleGoal,
                weeksLine = SampleWeeks
            ),
            "preview_vision_medium" to renderVisionCard(
                context = context,
                widthPx = MEDIUM_WIDTH,
                heightPx = MEDIUM_WIDTH / 2,
                style = style,
                photoUri = null,
                goal = SampleGoal,
                why = SampleWhy,
                targetLine = SampleTarget,
                weeksLine = SampleWeeks
            ),
            "preview_vision_large" to renderVisionCardLarge(
                context = context,
                widthPx = LARGE_SIDE,
                heightPx = LARGE_SIDE,
                style = style,
                photoUri = null,
                goal = SampleGoal,
                why = SampleWhy,
                milestones = SampleMilestones,
                targetLine = SampleTarget,
                weeksLine = SampleWeeks
            )
        )

        // PNG rather than WebP: these are flat colour and type, which PNG stores both
        // smaller and exactly. The Gallery designs that used to be written here were
        // mostly photographs, which is what WebP was for.
        cards.forEach { (name, bitmap) ->
            File(folder, "$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        Log.i("PreviewExport", "wrote ${cards.size} previews to ${folder.absolutePath}")
    }
}
