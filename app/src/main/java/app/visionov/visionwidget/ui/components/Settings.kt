package app.visionov.visionwidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.visionov.visionwidget.R
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.VisionType

/** The disc the gear sits on — fixed, so it reads the same over every screen. */
private val IconButtonFill = Color(0xFFEFEBE3)
private val IconButtonBorder = Color(0xFF12110F).copy(alpha = 0.09f)

/**
 * The band the gear is centred in — one line of the title face, whatever the header
 * around it is made of.
 *
 * Vision and Studio head their tabs with a single line and Today and Insights with an
 * eyebrow above one, so a gear centred on its own header sits at two different heights
 * and steps down the screen as the tabs are switched. Centring it on the first line's
 * height instead puts it in the same place on all four.
 */
private val TitleLine = 40.dp

/** The gear, at the head of every tab. */
@Composable
fun SettingsButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.height(TitleLine),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(IconButtonFill)
                .border(1.dp, IconButtonBorder, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = OnCanvas,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/**
 * What the gear opens.
 *
 * Shared rather than one per tab. Settings are the app's own, not a screen's, so the
 * gear has to mean the same thing wherever it is pressed — and there is only one of
 * these to fill in when there is finally something to put in it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(userFont: UserFontChoice, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Canvas,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(text = "SETTINGS", style = VisionType.sheetLabel, color = OnCanvasMuted)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Nothing here yet.",
                style = VisionType.sheetTitle(userFont),
                color = OnCanvas
            )
        }
    }
}
