package com.example.visionwidget.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * A label that gives up size rather than breaking.
 *
 * The chrome is set in a mono face with wide tracking, which reads well and measures
 * badly: a word like COMPLETED or SHUFFLE is far wider than it looks, and in a third of
 * a row on a narrow screen it wraps mid-word into COMPLET / ED. These are labels, not
 * prose — a broken one is always worse than a slightly smaller one.
 *
 * Shrinks a step at a time until the line fits, down to [minScale]. Tracking comes down
 * with the size, since it is the tracking as much as the letters that overruns.
 */
private const val SHRINK_STEP = 0.94f

@Composable
fun FittedText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    minScale: Float = 0.62f
) {
    var scale by remember(text, style) { mutableFloatStateOf(1f) }

    Text(
        text = text,
        style = style.copy(
            fontSize = style.fontSize * scale,
            letterSpacing = style.letterSpacing * scale
        ),
        color = color,
        maxLines = 1,
        softWrap = false,
        textAlign = textAlign,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.didOverflowWidth && scale > minScale) scale *= SHRINK_STEP
        }
    )
}
