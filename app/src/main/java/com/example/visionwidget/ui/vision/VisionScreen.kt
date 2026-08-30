package com.example.visionwidget.ui.vision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.visionwidget.ui.ContentWidthFraction
import com.example.visionwidget.ui.components.CreateVisionRow
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType

private val IconSize = 96.dp
private val IconInnerSize = 78.dp

/** Angular size of one dash and of the gap after it, in degrees. */
private const val DashDegrees = 6f
private const val DashStepDegrees = 12f

@Composable
fun VisionScreen(
    contentPadding: PaddingValues = PaddingValues(),
    userFontId: Int = UserFonts.DEFAULT_ID,
    onCreateVision: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userFont = UserFonts[userFontId]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier
                .fillMaxWidth(ContentWidthFraction)
                .fillMaxHeight()
        ) {
            Spacer(Modifier.height(20.dp))
            Text(text = "VISION", style = VisionType.eyebrow, color = OnCanvas)

            // Equal weights above and below centre the prompt between the eyebrow
            // and the action row, rather than pinning it to a fixed offset.
            Spacer(Modifier.weight(1f))
            EmptyVisionMark()
            Spacer(Modifier.height(28.dp))
            Prompt(userFont)
            Spacer(Modifier.weight(1f))

            HorizontalDivider(color = Rule, thickness = 1.dp)
            Spacer(Modifier.height(18.dp))
            CreateVisionRow(userFont = userFont, onClick = onCreateVision)
            // The floating nav sits right under this row, so clear it by more than the
            // bar's own margin or the two read as one block.
            Spacer(Modifier.height(24.dp + contentPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun Prompt(userFont: UserFontChoice) {
    Text(
        text = "What's your vision?",
        style = VisionType.screenPromptTitle(userFont),
        color = OnCanvas
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "The future starts with one goal.",
        style = VisionType.promptSubtitle,
        color = OnCanvasMuted
    )
}

/**
 * Dashed ring with a small hollow centre — the "nothing here yet" mark.
 *
 * Drawn as evenly spaced arcs rather than a stroked circle with a dash effect, because
 * the dashes have to stay a fixed angle apart; a length-based dash pattern would drift.
 */
@Composable
private fun EmptyVisionMark() {
    val dash = OnCanvas.copy(alpha = 0.16f)
    val centreMark = OnCanvas.copy(alpha = 0.28f)

    Spacer(
        Modifier
            .size(IconSize)
            .drawBehind {
                val outerRadius = IconSize.toPx() / 2
                val innerRadius = IconInnerSize.toPx() / 2
                val ringWidth = outerRadius - innerRadius
                val ringRadius = (outerRadius + innerRadius) / 2

                var angle = 0f
                while (angle < 360f) {
                    drawArc(
                        color = dash,
                        startAngle = angle,
                        sweepAngle = DashDegrees,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = ringWidth)
                    )
                    angle += DashStepDegrees
                }

                drawCircle(
                    color = centreMark,
                    radius = 6.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
    )
}
