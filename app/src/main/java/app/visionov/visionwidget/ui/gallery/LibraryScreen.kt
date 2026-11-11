package app.visionov.visionwidget.ui.gallery

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.visionov.visionwidget.ui.ContentWidthFraction
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.UserFonts
import app.visionov.visionwidget.ui.theme.VisionType
import app.visionov.visionwidget.widget.Slot
import app.visionov.visionwidget.widget.WidgetSize

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

    // The design being placed, if any — shown large in its own sheet so the last
    // thing seen before the launcher's generic confirmation is the real face.
    var placing by remember { mutableStateOf<GalleryWidget?>(null) }

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
                Text(
                    text = "? HOW TO ADD",
                    style = VisionType.eyebrow,
                    color = OnCanvasMuted,
                    modifier = Modifier
                        .clickable(onClick = onBrowse)
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(text = "MY LIBRARY", style = VisionType.eyebrow, color = OnCanvasMuted)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Total (${designs.size})",
                style = VisionType.greeting(UserFonts[UserFonts.DEFAULT_ID]),
                color = OnCanvas
            )
            Spacer(Modifier.height(18.dp))
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
                    onTap = { placing = design },
                    onRemove = { onRemove(design.id) }
                )
            }
        }
    }

    placing?.let { design ->
        PlaceSheet(
            design = design,
            onPlace = {
                onPlace(sizeOf(design), Slot.Design(design.id))
                placing = null
            },
            onDismiss = { placing = null }
        )
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
    onTap: () -> Unit,
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
        }

        Spacer(Modifier.height(10.dp))
        Text(text = design.name, style = galleryName(), color = OnCanvas, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(text = design.meta, style = GalleryMeta, color = OnCanvasMuted)
    }
}

/**
 * The design, shown large, with the one button that puts it on a home screen.
 *
 * Here because the launcher's own confirmation cannot be given a picture. One UI draws
 * the provider's static image there, and a provider is a size now rather than a
 * design — so whatever is chosen, that dialog shows the same plain mark. Three ways of
 * handing it the real thing were tried on a device and it took none of them.
 *
 * It cannot be skipped either; there is no way to place a widget without the launcher
 * asking. So the design is shown properly here instead, immediately before. The last
 * thing seen before confirming is the thing being added.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceSheet(
    design: GalleryWidget,
    onPlace: () -> Unit,
    onDismiss: () -> Unit
) {
    val userFont = UserFonts[UserFonts.DEFAULT_ID]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DesignFace(design, modifier = Modifier.fillMaxWidth(0.72f))

            Spacer(Modifier.height(16.dp))
            Text(text = design.name, style = VisionType.modalTitle(userFont), color = OnCanvas)
            Spacer(Modifier.height(5.dp))
            Text(text = design.meta, style = GalleryMeta, color = OnCanvasMuted)

            Spacer(Modifier.height(18.dp))
            Text(
                text = "This face goes on your home screen. Android will ask you to " +
                    "confirm — its own picture there is just our app mark, not this.",
                style = VisionType.modalBody(userFont),
                color = OnCanvasMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onPlace,
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(containerColor = OnCanvas, contentColor = Canvas),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(text = "Add to home screen", style = VisionType.modalButton(userFont))
            }

            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Cancel", style = VisionType.modalButton(userFont), color = OnCanvasMuted)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

