package com.example.visionwidget.ui.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * One design, drawn as itself.
 *
 * A picture rather than a working page — see [DesignRenderer] for why, and for what the
 * page has to be laid out at for a design to come out in the proportions it was drawn
 * in. Nothing here is interactive; a tap belongs to the tile around this.
 */
@Composable
fun DesignPreview(widget: GalleryWidget, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    BoxWithConstraints(modifier) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight

        // Keyed on the design and the size it is wanted at, so a tile handed a different
        // widget asks for that one and a picture already painted is simply handed back.
        val bitmap: Bitmap? by produceState<Bitmap?>(null, widget.id, widthPx, heightPx) {
            value = DesignRenderer.render(context, widget, widthPx, heightPx)
        }

        // Until it has been painted the tile is its backdrop and nothing else, which
        // reads as a design still arriving rather than as a gap.
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = widget.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
