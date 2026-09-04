package com.example.visionwidget.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.VisionType

/**
 * The "create a vision" call to action, shared by the Today card and the Vision tab so
 * the two empty states can't drift apart.
 *
 * Only "ONE MINUTE →" triggers [onClick]; "Create vision" reads as the heading for it.
 *
 * The whole row is set in [mutedColor]; on a themed card the caller passes the card's
 * own muted tone so the row never sits on a surface it wasn't coloured for.
 */
@Composable
fun CreateVisionRow(
    onClick: () -> Unit,
    mutedColor: Color = OnCanvasMuted,
    modifier: Modifier = Modifier
) {
    CardFooterRow(
        modifier = modifier,
        start = {
            Text(
                text = "CREATE VISION",
                style = VisionType.eyebrow,
                color = mutedColor
            )
        },
        end = {
            // Fills the row's height for a usable tap target, but adds no padding of
            // its own — that would inset the arrow from the edge the labels sit on.
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aligned by baseline, not by box: the arrow is set larger than the
                // label, so centring their boxes would drop it below the label's midline.
                Text(
                    text = "ONE MINUTE",
                    style = VisionType.eyebrow,
                    color = mutedColor,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "→",
                    style = VisionType.glyph,
                    color = mutedColor,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
    )
}
