package app.visionov.visionwidget.widget.config

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.visionov.visionwidget.MainActivity
import app.visionov.visionwidget.data.AppPreferences
import app.visionov.visionwidget.ui.ContentWidthFraction
import app.visionov.visionwidget.ui.ScaledToDesign
import app.visionov.visionwidget.ui.gallery.DesignFace
import app.visionov.visionwidget.ui.gallery.GalleryCatalog
import app.visionov.visionwidget.ui.gallery.GalleryColumns
import app.visionov.visionwidget.ui.gallery.GalleryGutter
import app.visionov.visionwidget.ui.gallery.GalleryMeta
import app.visionov.visionwidget.ui.gallery.GalleryWidget
import app.visionov.visionwidget.ui.gallery.TileShape
import app.visionov.visionwidget.ui.gallery.galleryName
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.Rule
import app.visionov.visionwidget.ui.theme.VisionType
import app.visionov.visionwidget.ui.theme.UserFonts
import app.visionov.visionwidget.widget.OpenGalleryExtra
import app.visionov.visionwidget.widget.GalleryTicker
import app.visionov.visionwidget.widget.Slot
import app.visionov.visionwidget.widget.SlotWidgets
import app.visionov.visionwidget.widget.WidgetSize
import android.util.Log
import app.visionov.visionwidget.widget.glanceIdFor
import app.visionov.visionwidget.widget.renderCardPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What a widget is to hold, asked the moment one is placed.
 *
 * The launcher's picker now offers three sizes and nothing else, so this is where the
 * actual choice is made: it is named on each provider as `android:configure`, and the
 * launcher opens it with the id of the widget it has just put down.
 *
 * The three dressed cards are always here. The Gallery is not — only the designs the
 * user has put in their library, because forty-four of them is a list nobody reads.
 * Where the library has nothing to offer, the way to it is still here as the last cell.
 */
class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    /** The size the screen was drawn for, kept so the redraw agrees with it. */
    private var requestedSize = WidgetSize.Medium

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Set before anything else, so backing out of here leaves the launcher to clear
        // away the cell it had already made room for. Without it a dismissed setting
        // screen leaves an empty widget sitting on the home screen.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        requestedSize = SlotWidgets.sizeOf(this, appWidgetId) ?: WidgetSize.Medium

        setContent {
            ScaledToDesign {
                ChooseWidgetScreen(
                    size = requestedSize,
                    onPick = ::choose,
                    onBrowse = ::browseGallery,
                    onClose = ::finish
                )
            }
        }
    }

    /** Writes the choice onto the widget, then hands it back to the launcher to draw. */
    private fun choose(slot: Slot) {
        val size = SlotWidgets.sizeOf(applicationContext, appWidgetId) ?: requestedSize

        // Named off this screen's own intent rather than looked up. The lookup insists
        // the widget already be bound, which it reliably is not while its setting
        // screen is open — that refusal is what used to leave the choice unwritten.
        val glanceId = glanceIdFor(applicationContext, intent)

        if (glanceId == null) {
            Log.w("VisionSlot", "no glance id on the configuration intent; nothing written")
        } else {
            // The cell composed once already, while this screen was open, and found
            // nothing chosen. Assigning sends it round again — and because the choice
            // is read inside the composition, this time it finds it.
            SlotWidgets.assign(applicationContext, glanceId, size, slot)
        }

        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        // A clock face needs the minute running; nothing else books it.
        if (slot is Slot.Design) GalleryTicker.schedule(applicationContext, known = true)

        finish()
    }

    /**
     * Opens the shelf so the library can be filled.
     *
     * The widget being configured is left behind: there is nothing to put in it yet, and
     * the result stays CANCELED so the launcher takes the empty cell back rather than
     * leaving one that shows nothing.
     */
    private fun browseGallery() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(OpenGalleryExtra, true)
        )
        finish()
    }
}

/** The three cards, then whatever the library holds at this size, then the way to the shelf. */
@Composable
private fun ChooseWidgetScreen(
    size: WidgetSize,
    onPick: (Slot) -> Unit,
    onBrowse: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val cards = listOf(Slot.Vision, Slot.TopThree, Slot.Wisdom)

    // Only what the user put in the library, and only what fits a cell this size. A
    // large cell has no Gallery designs at all — they are all drawn at 2x2 or 4x2.
    //
    // Nothing at all without Vision+: these are kept by the subscription, so offering
    // one here while the library itself is empty would be offering what cannot be had.
    // Read from the note Play's last answer was left in, as the widgets do — this
    // screen is opened from the launcher and has no billing connection of its own.
    val designs = remember(size) {
        val preferences = AppPreferences(context)
        if (!preferences.plusHeld) {
            emptyList()
        } else {
            val byId = GalleryCatalog.load(context).associateBy { it.id }
            preferences.libraryDesignIds.mapNotNull { byId[it] }.filter { size.holds(it) }
        }
    }

    // A square cell takes two across; the wide and tall ones are shown one to a row, at
    // their own proportion, because half a 4x2 is too small to read.
    val columns = if (size == WidgetSize.Small) GalleryColumns else 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(size = size, onClose = onClose)

        // The bar at the foot of the screen is given to the grid to scroll past rather
        // than fenced off beside it. A sibling spacer cannot do it: the grid takes the
        // whole of what the column has left, so anything after it is measured at
        // nothing and the last row stays under the buttons.
        //
        // Read at runtime rather than written down, because it is not one number — a
        // phone on gestures leaves a thin handle, the same phone on three buttons leaves
        // several times that, and the two differ again between devices.
        val barInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth(ContentWidthFraction),
            horizontalArrangement = Arrangement.spacedBy(GalleryGutter),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = barInset + 36.dp)
        ) {
            items(cards, key = { it.encode() }) { slot ->
                CardChoice(slot = slot, size = size, onPick = { onPick(slot) })
            }

            items(designs, key = { it.id }) { design ->
                DesignChoice(design = design, onPick = { onPick(Slot.Design(design.id)) })
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                BrowseCell(onBrowse = onBrowse, tall = designs.isEmpty())
            }
        }
    }
}

@Composable
private fun Header(size: WidgetSize, onClose: () -> Unit) {
    val label = when (size) {
        WidgetSize.Small -> "SMALL · 2 × 2"
        WidgetSize.Medium -> "MEDIUM · 4 × 2"
        WidgetSize.Large -> "LARGE · 4 × 4"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(ContentWidthFraction)
            .padding(top = 14.dp, bottom = 18.dp)
    ) {
        Text(
            text = "×",
            style = VisionType.glyph,
            color = OnCanvas,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onClose)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose a widget",
                style = VisionType.modalTitle(UserFonts[UserFonts.DEFAULT_ID]),
                color = OnCanvas
            )
            Spacer(Modifier.height(4.dp))
            Text(text = label, style = VisionType.eyebrow, color = OnCanvasMuted)
        }
    }
}

/**
 * One of the three dressed cards, drawn as it will actually look.
 *
 * Painted off the main thread and shown when it lands — the backdrop holds the space
 * meanwhile, so the grid never jumps as the three arrive.
 */
@Composable
private fun CardChoice(slot: Slot, size: WidgetSize, onPick: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val ratio = when (size) {
        WidgetSize.Small -> 1f
        WidgetSize.Medium -> 2f
        WidgetSize.Large -> 1f
    }

    val bitmap by produceState<Bitmap?>(initialValue = null, slot, size) {
        val widthPx = with(density) { 320.dp.toPx() }.toInt()
        value = withContext(Dispatchers.Default) {
            runCatching {
                renderCardPreview(context, slot, size, widthPx, (widthPx / ratio).toInt())
            }.getOrNull()
        }
    }

    Column(Modifier.clickable(onClick = onPick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(TileShape),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let {
                androidx.compose.foundation.Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(text = nameOf(slot), style = galleryName(), color = OnCanvas, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(text = "YOUR OWN", style = GalleryMeta, color = OnCanvasMuted)
    }
}

private fun nameOf(slot: Slot) = when (slot) {
    Slot.Vision -> "Vision"
    Slot.TopThree -> "Today's Top 3"
    Slot.Wisdom -> "Wisdom"
    is Slot.Design -> slot.designId
}

@Composable
private fun DesignChoice(design: GalleryWidget, onPick: () -> Unit) {
    Column(Modifier.clickable(onClick = onPick)) {
        DesignFace(design)
        Spacer(Modifier.height(10.dp))
        Text(text = design.name, style = galleryName(), color = OnCanvas, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(text = design.meta, style = GalleryMeta, color = OnCanvasMuted)
    }
}

/** The empty cell that leads to the shelf, where the library is filled. */
@Composable
private fun BrowseCell(onBrowse: () -> Unit, tall: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tall) Modifier.height(132.dp) else Modifier.height(96.dp))
            .clip(TileShape)
            .border(BorderStroke(1.dp, Rule), TileShape)
            .clickable(onClick = onBrowse),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "+", style = VisionType.glyph, color = OnCanvasMuted)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "BROWSE\nTHE GALLERY",
                style = GalleryMeta,
                color = OnCanvasMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
