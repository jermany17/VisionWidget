package com.example.visionwidget.ui.components

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
import com.example.visionwidget.R
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.VisionType

/** The disc the gear sits on — fixed, so it reads the same over every screen. */
private val IconButtonFill = Color(0xFFEFEBE3)
private val IconButtonBorder = Color(0xFF12110F).copy(alpha = 0.09f)

/** The gear, at the head of every tab. */
@Composable
fun SettingsButton(onClick: () -> Unit) {
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
            Text(text = "SETTINGS", style = VisionType.eyebrow, color = OnCanvasMuted)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Nothing here yet.",
                style = VisionType.sheetTitle(userFont),
                color = OnCanvas
            )
        }
    }
}
