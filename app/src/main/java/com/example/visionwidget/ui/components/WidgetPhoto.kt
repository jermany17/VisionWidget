package com.example.visionwidget.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The picture behind the widget cards, decoded from the URI the system picker returned.
 *
 * Null while it loads, and null again if the URI no longer resolves — a picture can be
 * deleted from the gallery long after it was chosen, and the cards fall back to their
 * placeholder rather than to an empty frame.
 */
@Composable
fun rememberWidgetPhoto(uri: String?): ImageBitmap? {
    val context = LocalContext.current
    var photo by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        photo = uri?.let { source ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(source)).use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    return photo
}

/**
 * Fills the node with [photo], cropped from the centre.
 *
 * The crop is taken out of the source rather than scaled into place, so the draw always
 * covers exactly the bounds the card was measured at. Nothing about the picture's own
 * dimensions reaches the layout — a tall photo can't stretch the card it sits behind.
 */
fun Modifier.croppedPhotoBackground(photo: ImageBitmap): Modifier = drawBehind {
    if (size.width <= 0f || size.height <= 0f) return@drawBehind

    val targetRatio = size.width / size.height
    val sourceRatio = photo.width.toFloat() / photo.height.toFloat()

    // Take the widest strip the target shape allows, then the tallest — whichever the
    // picture has to spare is what gets trimmed.
    val sourceWidth: Int
    val sourceHeight: Int
    if (sourceRatio > targetRatio) {
        sourceHeight = photo.height
        sourceWidth = (photo.height * targetRatio).roundToInt().coerceAtMost(photo.width)
    } else {
        sourceWidth = photo.width
        sourceHeight = (photo.width / targetRatio).roundToInt().coerceAtMost(photo.height)
    }

    drawImage(
        image = photo,
        srcOffset = IntOffset((photo.width - sourceWidth) / 2, (photo.height - sourceHeight) / 2),
        srcSize = IntSize(sourceWidth, sourceHeight),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
    )
}
