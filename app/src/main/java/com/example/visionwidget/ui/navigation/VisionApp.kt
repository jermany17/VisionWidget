package com.example.visionwidget.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.visionwidget.ui.theme.CreamMuted
import com.example.visionwidget.ui.theme.Ink
import com.example.visionwidget.ui.theme.InkMuted
import com.example.visionwidget.ui.theme.Plum
import com.example.visionwidget.ui.theme.VisionType

enum class VisionTab(val label: String) {
    Today("Today"),
    Vision("Vision"),
    Studio("Studio"),
    Insights("Insights")
}

private val NavBarHeight = 52.dp
private val NavBarMargin = 12.dp

@Composable
fun VisionApp() {
    var selectedTab by rememberSaveable { mutableStateOf(VisionTab.Today) }

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
            VisionTab.Today -> TodayScreen(contentPadding = screenPadding)
            VisionTab.Vision -> PlaceholderScreen(VisionTab.Vision.label)
            VisionTab.Studio -> PlaceholderScreen(VisionTab.Studio.label)
            VisionTab.Insights -> PlaceholderScreen(VisionTab.Insights.label)
        }

        BottomNav(
            selected = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = NavBarMargin)
        )
    }
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
            .fillMaxWidth()
            .height(NavBarHeight)
            // The wisdom card behind the bar is the same plum, so the bar needs a
            // shadow to read as floating rather than merging into it.
            .shadow(elevation = 12.dp, shape = barShape, clip = false)
            .clip(barShape)
            .background(Plum)
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
            .background(if (isSelected) Canvas else Plum)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tab.label,
            style = VisionType.navLabel,
            color = if (isSelected) Ink else CreamMuted,
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
        Text(text = title.uppercase(), style = VisionType.eyebrow, color = InkMuted)
    }
}
