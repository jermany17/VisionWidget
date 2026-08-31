package com.example.visionwidget.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Height of the meta row under a card's divider. Fixed rather than wrapped, so a card
 * keeps the same footer geometry whether it holds plain labels or a tappable control —
 * otherwise a card's height, and the gap under its divider, would shift with its state.
 */
val CardFooterHeight = 24.dp

@Composable
fun CardFooterRow(
    modifier: Modifier = Modifier,
    start: @Composable () -> Unit,
    end: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CardFooterHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        start()
        end()
    }
}
