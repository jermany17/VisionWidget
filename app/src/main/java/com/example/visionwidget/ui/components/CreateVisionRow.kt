package com.example.visionwidget.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.VisionType

/**
 * The "create a vision" call to action, shared by the Today card and the Vision tab so
 * the two empty states can't drift apart. Both halves trigger [onClick].
 *
 * Colours default to the canvas pair; on a themed card the caller passes the card's
 * own pair so the row never sits on a surface it wasn't coloured for.
 */
@Composable
fun CreateVisionRow(
    userFont: UserFontChoice,
    onClick: () -> Unit,
    contentColor: Color = OnCanvas,
    mutedColor: Color = OnCanvasMuted,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Create vision",
            style = VisionType.promptAction(userFont),
            color = contentColor,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Aligned by baseline, not by box: the arrow is set larger than the label,
            // so centring their boxes would drop the glyph below the label's midline.
            Text(
                text = "ONE MINUTE",
                style = VisionType.eyebrow,
                color = mutedColor,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(6.dp))
            // Full strength rather than muted: the arrow is the affordance, and at this
            // size a 55% black one reads as a smudge.
            Text(
                text = "→",
                style = VisionType.arrow,
                color = contentColor,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}
