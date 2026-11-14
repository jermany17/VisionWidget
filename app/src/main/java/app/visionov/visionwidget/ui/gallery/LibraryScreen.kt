package app.visionov.visionwidget.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.visionov.visionwidget.ui.ContentWidthFraction
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.Rule
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.UserFonts
import app.visionov.visionwidget.ui.theme.VisionType
import app.visionov.visionwidget.widget.Slot
import app.visionov.visionwidget.widget.WidgetSize

/** The empty card's own fill, a shade off the canvas as the plan row is. */
private val EmptyCardFill = Color(0xFFF0ECE5)

/**
 * The designs kept for the home screen.
 *
 * This is the list the launcher's setting screen reads: a design has to be here before
 * it can be put in a cell. Which is the whole point of it — there are forty-four
 * designs on the shelf, and a setting screen offering all of them is one nobody reads.
 *
 * Removing is immediate and unconfirmed. Nothing is lost by it: the design is still on
 * the shelf, and a widget already showing it on the home screen is left where it is.
 */
@Composable
fun LibraryScreen(
    designIds: List<String>,
    onRemove: (String) -> Unit,
    onPlace: (WidgetSize, Slot) -> Unit,
    onBrowse: () -> Unit,
    /** The directions for placing a widget, which the host shows as a page of its own. */
    onHowToAdd: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalog = remember { GalleryCatalog.load(context) }

    // Kept order, not catalogue order — the library reads as the user built it. A saved
    // id the catalogue no longer carries is dropped rather than shown as a gap.
    val designs = remember(catalog, designIds) {
        val byId = catalog.associateBy { it.id }
        designIds.mapNotNull { byId[it] }
    }

    // Which tile is showing its "Add to screen" button, if any.
    var offering by rememberSaveable { mutableStateOf<String?>(null) }

    val userFont = UserFonts[UserFonts.DEFAULT_ID]

    // The design being placed, if any — shown large in its own sheet so the last
    // thing seen before the launcher's generic confirmation is the real face.

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth(ContentWidthFraction)) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← BACK",
                    style = VisionType.eyebrow,
                    color = OnCanvasMuted,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                )
                // The directions for getting a widget onto the home screen, which is the
                // one thing this screen cannot do for the user — a launcher only takes
                // that from its own picker. Same sheet the settings row opens.
                HowToAddPill(userFont = userFont, onClick = onHowToAdd)
            }
            Spacer(Modifier.height(14.dp))
        }

        HorizontalDivider(color = Rule, thickness = 1.dp)

        Column(Modifier.fillMaxWidth(ContentWidthFraction)) {
            Spacer(Modifier.height(18.dp))
            Text(text = "MY LIBRARY", style = VisionType.eyebrow, color = OnCanvasMuted)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Total (${designs.size})",
                style = VisionType.greeting(userFont),
                color = OnCanvas
            )
            Spacer(Modifier.height(18.dp))

            // Nothing kept yet. The shelf is where designs are found, so the empty
            // library says where to go rather than sitting as an empty grid — there is
            // no gesture on this screen that would ever fill it.
            if (designs.isEmpty()) {
                EmptyLibrary(userFont = userFont, onBrowse = onBrowse)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(GalleryColumns),
            modifier = Modifier.fillMaxWidth(ContentWidthFraction),
            horizontalArrangement = Arrangement.spacedBy(GalleryGutter),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 30.dp
            )
        ) {
            items(designs, key = { it.id }) { design ->
                LibraryTile(
                    design = design,
                    // One tile at a time offers its button. Held here rather than in
                    // the tile, because a tile cannot know that another has been
                    // tapped — kept there, every tap left its own button up and the
                    // shelf filled with them.
                    offering = offering == design.id,
                    onTap = { offering = if (offering == design.id) null else design.id },
                    // Straight to the launcher's own dialog. A sheet of ours used to
                    // stand in front of it, showing the face large because that dialog
                    // cannot be given a picture of it — but it made adding a widget two
                    // confirmations deep, and the second one is the only one that
                    // actually places anything.
                    onAdd = { onPlace(sizeOf(design), Slot.Design(design.id)) },
                    onRemove = { onRemove(design.id) }
                )
            }
        }
    }

}

/** The way to the directions, sat opposite the way back. */
@Composable
private fun HowToAddPill(userFont: UserFontChoice, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Canvas)
            .border(1.dp, OnCanvas.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick)
            .padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(OnCanvas),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "?", style = VisionType.fieldLabel, color = Canvas)
        }
        Spacer(Modifier.size(8.dp))
        Text(
            text = "How to add",
            style = VisionType.userFace(userFont, 14.sp, 18.sp),
            color = OnCanvas
        )
    }
}

/** The card that stands where the shelf would be, and the way to fill it. */
@Composable
private fun EmptyLibrary(userFont: UserFontChoice, onBrowse: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EmptyCardFill)
            .padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Text(text = "NOTHING SAVED YET", style = VisionType.eyebrow, color = OnCanvasMuted)
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Find one you like\nin the Gallery.",
            style = VisionType.screenPromptTitle(userFont),
            color = OnCanvas
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Tap Save widget on any design.\nIt waits here until you add it.",
            style = VisionType.sheetLead(userFont),
            color = OnCanvasMuted
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(OnCanvas)
                .clickable(onClick = onBrowse)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Browse the Gallery",
                style = VisionType.buttonLabel(userFont),
                color = Canvas
            )
            Spacer(Modifier.size(10.dp))
            Text(text = "→", style = VisionType.glyph, color = Canvas)
        }
    }
}

/** Which cell a design belongs in — the catalogue only draws them at two sizes. */
private fun sizeOf(design: GalleryWidget): WidgetSize =
    if (WidgetSize.Small.holds(design)) WidgetSize.Small else WidgetSize.Medium

/**
 * One kept design: the face, a cross to drop it, and a tap to put it on the screen.
 *
 * The cross sits over the face rather than beside the name, so the whole row of them
 * reads at a glance — and so the tap that places the design has the rest of the tile
 * to itself.
 */
@Composable
private fun LibraryTile(
    design: GalleryWidget,
    offering: Boolean,
    onTap: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Column {
        DesignFace(
            widget = design,
            modifier = Modifier.clickable(onClick = onTap)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Canvas)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "×", style = VisionType.glyph, color = OnCanvas)
            }

            // Shown once the face is tapped rather than always, so the shelf stays a
            // shelf of designs rather than a shelf of buttons. Opens the sheet that
            // shows the face large, which is the last place it can be seen properly
            // before the launcher's own confirmation draws its generic mark instead.
            if (offering) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Canvas)
                        .clickable(onClick = onAdd)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(text = "Add to screen", style = GalleryMeta, color = OnCanvas)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(text = design.name, style = galleryName(), color = OnCanvas, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(text = design.meta, style = GalleryMeta, color = OnCanvasMuted)
    }
}
