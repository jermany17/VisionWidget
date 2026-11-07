package app.visionov.visionwidget.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.VisionType

/**
 * The app's way of asking something it will not take silence for.
 *
 * Two answers and no third, so there is nothing to dismiss it with: a way out that said
 * neither would leave the same question to be asked again on the next move. [quietLabel]
 * is the answer that gives something up and [loudLabel] the one that holds onto it,
 * whichever way round that falls for the question being put.
 *
 * Shared rather than written where it is used. These appear at the two moments the app
 * can lose something the user made — a design chosen and not applied, a milestone
 * written and not added — and a question asked twice in two voices reads as two
 * different kinds of warning.
 */
@Composable
fun PromptDialog(
    /** Where the question is coming from: STUDIO, MILESTONE. */
    eyebrow: String,
    title: String,
    body: String,
    userFont: UserFontChoice,
    quietLabel: String,
    onQuiet: () -> Unit,
    loudLabel: String,
    onLoud: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Canvas
        ) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
                Text(text = eyebrow, style = VisionType.modalLabel, color = OnCanvasMuted)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    style = VisionType.modalTitle(userFont),
                    color = OnCanvas
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = body,
                    style = VisionType.modalBody(userFont),
                    color = OnCanvasMuted
                )
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onQuiet,
                        shape = RoundedCornerShape(percent = 50),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = quietLabel,
                            style = VisionType.modalButton(userFont),
                            color = OnCanvas,
                            maxLines = 1
                        )
                    }
                    Button(
                        onClick = onLoud,
                        shape = RoundedCornerShape(percent = 50),
                        colors = ButtonDefaults.buttonColors(containerColor = OnCanvas),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = loudLabel,
                            style = VisionType.modalButton(userFont),
                            color = Canvas,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
