package com.example.visionwidget.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
