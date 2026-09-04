package com.example.visionwidget.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.visionwidget.ui.home.TodayScreen
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.NavBar
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnNavBar
import com.example.visionwidget.ui.theme.VisionType
import com.example.visionwidget.ui.vision.VisionScreen

enum class VisionTab(val label: String) {
    Today("Today"),
    Vision("Vision"),
    Studio("Studio"),
    Insights("Insights")
}

private val NavBarHeight = 52.dp
private val NavBarMargin = 12.dp
private const val NavBarWidthFraction = 0.8f

@Composable
fun VisionApp() {
    var selectedTab by rememberSaveable { mutableStateOf(VisionTab.Today) }
    // Stands in for the DB until it exists, driven by the dev panel below.
    var hasVision by rememberSaveable { mutableStateOf(true) }

    // The nav bar floats above the content, so scrollable screens need room to
    // clear it before the system navigation inset starts.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val screenPadding = PaddingValues(bottom = NavBarHeight + NavBarMargin * 2 + navInset)

    Box(
        Modifier
            .fillMaxSize()
            .background(Canvas)
    ) {
        when (selectedTab) {
            VisionTab.Today -> TodayScreen(
                contentPadding = screenPadding,
                hasVision = hasVision,
                onOpenVision = { selectedTab = VisionTab.Vision }
            )
            VisionTab.Vision -> VisionScreen(contentPadding = screenPadding)
            VisionTab.Studio -> PlaceholderScreen(VisionTab.Studio.label)
            VisionTab.Insights -> PlaceholderScreen(VisionTab.Insights.label)
        }

        BottomNav(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(vertical = NavBarMargin)
        )

        DevPanel(
            hasVision = hasVision,
            onToggleVision = { hasVision = !hasVision },
            // Sits clear of the nav bar, on the left so it can't cover the wisdom
            // card's shuffle control.
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 20.dp, bottom = NavBarHeight + NavBarMargin * 2)
        )
    }
}

/**
 * Temporary switch for the one state a database will still own before it exists —
 * whether a vision is set. Collapsed behind a chip so it stays out of the way. Delete
 * this along with the state it drives once the DB is wired up.
 */
@Composable
private fun DevPanel(
    hasVision: Boolean,
    onToggleVision: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Bottom-anchored, so opening the panel grows it upward and the trigger stays put.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (expanded) {
            DevChip(
                text = if (hasVision) "VISION ON" else "VISION OFF",
                onClick = onToggleVision
            )
        }
        DevChip(
            text = if (expanded) "DEV −" else "DEV +",
            onClick = { expanded = !expanded }
        )
    }
}

@Composable
private fun DevChip(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = VisionType.eyebrow,
        color = OnNavBar,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(NavBar)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun BottomNav(
    selected: VisionTab,
    onSelect: (VisionTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(NavBarHeight / 2)
    Row(
        modifier = modifier
            // A fraction rather than fixed side margins, so the bar keeps its
            // proportions instead of stretching wide on large screens.
            .fillMaxWidth(NavBarWidthFraction)
            .height(NavBarHeight)
            // A card may sit directly behind the bar in the same colour, so the
            // shadow is what makes it read as floating rather than merging in.
            .shadow(elevation = 12.dp, shape = barShape, clip = false)
            .clip(barShape)
            .background(NavBar)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VisionTab.entries.forEach { tab ->
            NavItem(
                tab = tab,
                isSelected = tab == selected,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavItem(
    tab: VisionTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(pillShape)
            // Selected tab inverts the bar: white pill, black label.
            .background(if (isSelected) OnNavBar else NavBar)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tab.label,
            style = VisionType.navLabel,
            color = if (isSelected) NavBar else OnNavBar,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title.uppercase(), style = VisionType.eyebrow, color = OnCanvas)
    }
}
