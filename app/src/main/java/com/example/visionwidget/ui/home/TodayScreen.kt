package com.example.visionwidget.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.visionwidget.ui.ContentWidthFraction
import com.example.visionwidget.ui.components.CreateVisionRow
import com.example.visionwidget.ui.theme.AvatarFill
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.CardTheme
import com.example.visionwidget.ui.theme.CardThemes
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType

// Mock data — pinned to the design reference until the real sources are wired up.
private const val MOCK_DATE = "SATURDAY 1 AUGUST"
private const val MOCK_GREETING = "Good morning, Jae."
private const val MOCK_INITIAL = "J"
private const val MOCK_STREAK = "17 days"
private const val MOCK_THIS_WEEK = "18 / 21"
private const val MOCK_TOP_3_DONE = "0 / 3"

private val CardCornerRadius = 16.dp
private val CardShape = RoundedCornerShape(CardCornerRadius)

/**
 * Card and font ids default to their registry defaults; once the DB is wired up the
 * caller passes the stored ids instead and nothing else here has to change.
 */
@Composable
fun TodayScreen(
    contentPadding: PaddingValues = PaddingValues(),
    visionThemeId: Int = CardThemes.DEFAULT_ID,
    topThreeThemeId: Int = CardThemes.DEFAULT_ID,
    wisdomThemeId: Int = CardThemes.DEFAULT_ID,
    userFontId: Int = UserFonts.DEFAULT_ID,
    hasVision: Boolean = false,
    onOpenVision: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userFont = UserFonts[userFontId]

    // Outer column owns the background, insets and scrolling at full width; the inner
    // one holds the content at a fixed fraction so every row shares the same edges.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth(ContentWidthFraction)) {
            Spacer(Modifier.height(20.dp))
            Header(userFont = userFont)

            Spacer(Modifier.height(20.dp))
            MetricsRow(userFont = userFont)

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Rule, thickness = 1.dp)
            Spacer(Modifier.height(20.dp))

            // With no vision set the trailing label is inert — nothing to open yet, so
            // the prompt inside the card carries the action instead.
            SectionLabel(
                label = "VISION",
                action = if (hasVision) "OPEN" else null,
                trailing = if (hasVision) null else "NOT SET",
                onActionClick = onOpenVision
            )
            Spacer(Modifier.height(10.dp))
            if (hasVision) {
                ThemedCard(
                    theme = CardThemes[visionThemeId],
                    minHeight = 150.dp,
                    onClick = onOpenVision
                )
            } else {
                EmptyVisionCard(userFont = userFont, onCreate = onOpenVision)
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel(label = "TODAY'S TOP 3", trailing = MOCK_TOP_3_DONE)
            Spacer(Modifier.height(10.dp))
            ThemedCard(theme = CardThemes[topThreeThemeId], minHeight = 190.dp)

            Spacer(Modifier.height(22.dp))
            SectionLabel(label = "DAILY WISDOM")
            Spacer(Modifier.height(10.dp))
            WisdomCard(theme = CardThemes[wisdomThemeId], userFont = userFont)

            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun Header(userFont: UserFontChoice) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(text = MOCK_DATE, style = VisionType.eyebrow, color = OnCanvas)
            Spacer(Modifier.height(8.dp))
            Text(text = MOCK_GREETING, style = VisionType.greeting(userFont), color = OnCanvas)
        }
        Spacer(Modifier.size(12.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AvatarFill),
            contentAlignment = Alignment.Center
        ) {
            Text(text = MOCK_INITIAL, style = VisionType.avatar(userFont), color = OnCanvas)
        }
    }
}

@Composable
private fun MetricsRow(userFont: UserFontChoice) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Metric(label = "STREAK", value = MOCK_STREAK, userFont = userFont)
        Spacer(Modifier.weight(1f))
        Metric(label = "THIS WEEK", value = MOCK_THIS_WEEK, userFont = userFont)
    }
}

@Composable
private fun Metric(label: String, value: String, userFont: UserFontChoice) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = label,
            style = VisionType.eyebrow,
            color = OnCanvas,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(text = value, style = VisionType.metricValue(userFont), color = OnCanvas)
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
        Text(text = label, style = VisionType.eyebrow, color = OnCanvas)
        when {
            action != null -> Text(
                text = action,
                style = VisionType.action,
                color = OnCanvas,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = onActionClick != null) { onActionClick?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            trailing != null -> Text(
                text = trailing,
                style = VisionType.eyebrow,
                color = OnCanvas
            )
        }
    }
}

/**
 * Placeholder shown until a vision exists. It sits on the canvas rather than on a card
 * surface — there is nothing to theme yet — so a dashed outline marks the slot instead.
 */
@Composable
private fun EmptyVisionCard(userFont: UserFontChoice, onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(color = Rule, cornerRadius = CardCornerRadius)
            .padding(20.dp)
    ) {
        Text(
            text = "What's your vision?",
            style = VisionType.promptTitle(userFont),
            color = OnCanvas
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "The future starts with one goal.",
            style = VisionType.promptSubtitle,
            color = OnCanvasMuted
        )

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = Rule, thickness = 1.dp)
        Spacer(Modifier.height(14.dp))

        CreateVisionRow(userFont = userFont, onClick = onCreate)
    }
}

/** Rounded dashed outline. Compose has no dashed equivalent of Modifier.border. */
private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.dp,
    dash: Dp = 6.dp,
    gap: Dp = 5.dp
) = drawBehind {
    val stroke = strokeWidth.toPx()
    drawRoundRect(
        color = color,
        // Inset by half the stroke so the outline sits fully inside the bounds.
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))
        )
    )
}

/**
 * Which quote is showing is local view state — a shuffle position, not something the
 * rest of the app needs — so it stays inside this composable. The index rather than
 * the entry itself is remembered, because an Int survives process death for free.
 */
@Composable
private fun WisdomCard(theme: CardTheme, userFont: UserFontChoice) {
    var index by rememberSaveable { mutableIntStateOf(WISDOM.indices.random()) }
    val wisdom = WISDOM[index]

    // No minimum height: a floor would leave slack under the footer on short quotes,
    // so the gap below the meta row would grow as the quote got shorter.
    ThemedCard(theme = theme) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = wisdom.text,
                style = VisionType.quote(userFont),
                color = theme.onSurface
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DAILY WISDOM · ${wisdom.category.uppercase()}",
                    style = VisionType.eyebrow,
                    color = theme.onSurfaceMuted
                )
                Text(
                    text = "SHUFFLE ↻",
                    style = VisionType.eyebrow,
                    color = theme.onSurfaceMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { index = nextWisdomIndex(index) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Card frame painted from [theme]. Anything placed inside inherits the theme's text
 * colour through [LocalContentColor], so contents can't drift from their surface.
 */
@Composable
private fun ThemedCard(
    theme: CardTheme,
    minHeight: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // A floor rather than a fixed height, so cards with content can grow.
            .heightIn(min = minHeight)
            .clip(CardShape)
            .background(theme.surface)
            .then(theme.border?.let { Modifier.border(1.dp, it, CardShape) } ?: Modifier)
            // Clickable last so the ripple lands inside the clipped, painted card.
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides theme.onSurface) {
            content()
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun TodayScreenPreview() {
    TodayScreen()
}
