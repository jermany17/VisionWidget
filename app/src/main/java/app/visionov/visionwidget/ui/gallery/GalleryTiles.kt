package app.visionov.visionwidget.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.visionov.visionwidget.ui.theme.DMMono
import app.visionov.visionwidget.ui.theme.InstrumentSerif
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import kotlin.math.absoluteValue

/**
 * A Gallery design as a tile, wherever one is shown.
 *
 * Three places show the same tile now — the shelf in Studio, My Library, and the setting
 * screen the launcher opens when a widget is placed — so the tile lives here rather than
 * privately inside any one of them. What differs between the three is what sits under
 * the tile and what a tap does, which is the caller's business.
 */

/** Two faces across, with room between them for a shelf to read as a grid. */
val GalleryGutter = 14.dp
const val GalleryColumns = 2

// Every tile is the same square whatever shape the design inside it is. The inset is
// what keeps the backdrop reading as a frame rather than as a strip down either side of
// a design that has covered it.
val TileShape = RoundedCornerShape(20.dp)
private const val TileInset = 0.84f

/**
 * The washes a tile is backed with, cycled by name.
 *
 * Muted and close in weight, so forty of them down a shelf read as one surface the
 * designs are laid on rather than as a set of colours competing with them.
 */
private val TileBackdrops = listOf(
    listOf(Color(0xFF6F7A64), Color(0xFF3E4A38)),
    listOf(Color(0xFFD3C7C1), Color(0xFF8C7F79)),
    listOf(Color(0xFFBFC4C9), Color(0xFF6E757C)),
    listOf(Color(0xFFD8CDBA), Color(0xFF978970)),
    listOf(Color(0xFFC9BCC6), Color(0xFF7C6E79)),
    listOf(Color(0xFFBCC6C3), Color(0xFF6B7975))
)

/**
 * The wash behind a design.
 *
 * Picked by name rather than chosen per design: there are forty of these and the
 * backdrop is only there to set the design off the page. Taken from the name's own hash
 * so a design keeps the same backdrop between runs, and neighbours on the shelf differ.
 */
fun backdropFor(id: String): List<Color> =
    TileBackdrops[(id.hashCode().absoluteValue) % TileBackdrops.size]

/** A face's name. The app's own serif, as the labels on the Design shelf are. */
fun galleryName() = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 19.sp
)

/** What the face is, under its name — size and grid, set like every other eyebrow. */
val GalleryMeta = TextStyle(
    fontFamily = DMMono,
    fontWeight = FontWeight.Normal,
    fontSize = 8.sp,
    lineHeight = 12.sp,
    letterSpacing = 1.2.sp
)

/**
 * The square a design sits in, on its own backdrop.
 *
 * Every tile is square whatever shape the design in it is, so a shelf reads as a grid
 * rather than as rows of different heights. A wide design sits across the middle of its
 * square with the backdrop above and below it; a square one is inset far enough that the
 * backdrop still frames it rather than being covered over.
 *
 * [overlay] is drawn over the square — the delete cross in My Library, the tick in the
 * setting screen — and is given the same box so it can be placed in a corner of it.
 */
@Composable
fun DesignFace(
    widget: GalleryWidget,
    modifier: Modifier = Modifier,
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(TileShape)
            .background(Brush.linearGradient(backdropFor(widget.id))),
        contentAlignment = Alignment.Center
    ) {
        // Left unclipped: each design draws its own corner, and a second one over
        // the top of it would cut the first.
        DesignPreview(
            widget = widget,
            modifier = Modifier
                .fillMaxWidth(TileInset)
                .aspectRatio(widget.ratio)
        )
        overlay()
    }
}

/** The face with its name and what it is underneath — the shelf's whole tile, less any control. */
@Composable
fun DesignTile(
    widget: GalleryWidget,
    modifier: Modifier = Modifier,
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {}
) {
    Column(modifier) {
        DesignFace(widget, overlay = overlay)
        Spacer(Modifier.height(10.dp))
        Text(text = widget.name, style = galleryName(), color = OnCanvas, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(text = widget.meta, style = GalleryMeta, color = OnCanvasMuted)
    }
}
