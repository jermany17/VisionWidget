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
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.visionwidget.ui.vision.Milestone
import com.example.visionwidget.ui.vision.Vision
import com.example.visionwidget.ui.vision.VisionListSaver
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

    // The visions live here rather than in the Vision tab, because Today shows the
    // first of them too — one list, so the two tabs can't disagree. Stands in for the
    // DB until it exists.
    var visions by rememberSaveable(stateSaver = VisionListSaver) {
        mutableStateOf(emptyList<Vision>())
    }
    var selectedVisionId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Ids are handed out here and never reused, so a chip's identity survives a
    // neighbour being removed.
    var nextVisionId by rememberSaveable { mutableLongStateOf(1L) }
    // One counter shared across every vision's milestones, same reasoning as above.
    var nextMilestoneId by rememberSaveable { mutableLongStateOf(1L) }

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
                // Today follows the oldest vision, not the one the Vision tab happens
                // to have selected.
                vision = visions.firstOrNull(),
                onOpenVision = { selectedTab = VisionTab.Vision }
            )
            VisionTab.Vision -> VisionScreen(
                contentPadding = screenPadding,
                visions = visions,
                selectedVisionId = selectedVisionId,
                onSelectVision = { selectedVisionId = it },
                onCreateVision = { goal, why, targetDateMillis ->
                    val created = Vision(
                        id = nextVisionId,
                        goal = goal,
                        why = why,
                        targetDateMillis = targetDateMillis
                    )
                    nextVisionId++
                    visions = visions + created
                    // A vision just made is the one the user wants to look at.
                    selectedVisionId = created.id
                },
                onEditVision = { id, goal, why, targetDateMillis ->
                    visions = visions.map {
                        if (it.id == id) {
                            it.copy(goal = goal, why = why, targetDateMillis = targetDateMillis)
                        } else {
                            it
                        }
                    }
                },
                onDeleteVision = { id ->
                    visions = visions.filterNot { it.id == id }
                    // The fallback in VisionScreen picks another once this one is gone.
                    if (selectedVisionId == id) selectedVisionId = null
                },
                onAddMilestone = { visionId, step, dueDateMillis ->
                    val milestone = Milestone(
                        id = nextMilestoneId,
                        step = step,
                        dueDateMillis = dueDateMillis
                    )
                    nextMilestoneId++
                    visions = visions.map {
                        if (it.id == visionId) it.copy(milestones = it.milestones + milestone) else it
                    }
                },
                onToggleMilestone = { visionId, milestoneId ->
                    visions = visions.map { vision ->
                        if (vision.id != visionId) return@map vision
                        vision.copy(
                            milestones = vision.milestones.map {
                                if (it.id == milestoneId) it.copy(checked = !it.checked) else it
                            }
                        )
                    }
                },
                onDeleteMilestone = { visionId, milestoneId ->
                    visions = visions.map { vision ->
                        if (vision.id != visionId) return@map vision
                        vision.copy(milestones = vision.milestones.filterNot { it.id == milestoneId })
                    }
                }
            )
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
