package com.example.visionwidget.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.visionwidget.ui.theme.AvatarFill
import com.example.visionwidget.ui.theme.Amber
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.Indigo
import com.example.visionwidget.ui.theme.Ink
import com.example.visionwidget.ui.theme.InkMuted
import com.example.visionwidget.ui.theme.Plum
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.VisionType
import com.example.visionwidget.ui.theme.VisionWidgetTheme

// Mock data — pinned to the design reference until the real sources are wired up.
private const val MOCK_DATE = "SATURDAY 1 AUGUST"
private const val MOCK_GREETING = "Good morning, Jae."
private const val MOCK_INITIAL = "J"
private const val MOCK_STREAK = "17 days"
private const val MOCK_THIS_WEEK = "18 / 21"
private const val MOCK_TOP_3_DONE = "0 / 3"

private val ScreenPadding = 20.dp
private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun TodayScreen(
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding)
    ) {
        Spacer(Modifier.height(20.dp))
        Header()

        Spacer(Modifier.height(20.dp))
        MetricsRow()

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Rule, thickness = 1.dp)
        Spacer(Modifier.height(20.dp))

        SectionLabel(label = "VISION", action = "Open", onActionClick = { /* TODO: open vision */ })
        Spacer(Modifier.height(10.dp))
        EmptyCard(height = 150.dp)

        Spacer(Modifier.height(22.dp))
        SectionLabel(label = "TODAY'S TOP 3", trailing = MOCK_TOP_3_DONE)
        Spacer(Modifier.height(10.dp))
        EmptyCard(height = 190.dp)

        Spacer(Modifier.height(22.dp))
        SectionLabel(label = "DAILY WISDOM")
        Spacer(Modifier.height(10.dp))
        EmptyCard(height = 120.dp)

        Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(text = MOCK_DATE, style = VisionType.eyebrow, color = InkMuted)
            Spacer(Modifier.height(8.dp))
            Text(text = MOCK_GREETING, style = VisionType.greeting, color = Ink)
        }
        Spacer(Modifier.size(12.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AvatarFill),
            contentAlignment = Alignment.Center
        ) {
            Text(text = MOCK_INITIAL, style = VisionType.avatar, color = InkMuted)
        }
    }
}

@Composable
private fun MetricsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Metric(label = "STREAK", value = MOCK_STREAK, valueColor = Indigo)
        Spacer(Modifier.weight(1f))
        Metric(label = "THIS WEEK", value = MOCK_THIS_WEEK, valueColor = Amber)
    }
}

@Composable
private fun Metric(label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = label,
            style = VisionType.eyebrow,
            color = InkMuted,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(text = value, style = VisionType.metricValue, color = valueColor)
    }
}

@Composable
private fun SectionLabel(
    label: String,
    action: String? = null,
    trailing: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = VisionType.eyebrow, color = InkMuted)
        when {
            action != null -> Text(
                text = action,
                style = VisionType.action,
                color = Amber,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = onActionClick != null) { onActionClick?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            trailing != null -> Text(
                text = trailing,
                style = VisionType.eyebrow,
                color = InkMuted
            )
        }
    }
}

/** Placeholder surface — the card frame only, contents land here later. */
@Composable
private fun EmptyCard(height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(CardShape)
            .background(Plum)
    )
}

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun TodayScreenPreview() {
    VisionWidgetTheme {
        TodayScreen()
    }
}
