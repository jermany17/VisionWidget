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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visionwidget.data.VisionAppViewModel
import com.example.visionwidget.data.topThreeStats
import com.example.visionwidget.ui.home.TodayScreen
import com.example.visionwidget.ui.home.WISDOM
import com.example.visionwidget.ui.home.nextWisdomIndex
import com.example.visionwidget.ui.insights.InsightsScreen
import com.example.visionwidget.ui.studio.StudioScreen
import com.example.visionwidget.ui.onboarding.OnboardingData
import com.example.visionwidget.ui.onboarding.OnboardingFlow
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.NavBar
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnNavBar
import com.example.visionwidget.ui.theme.VisionType
import com.example.visionwidget.ui.vision.VisionScreen
import java.time.LocalDate

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
fun VisionApp(
    showOnboarding: Boolean = false,
    onFinishOnboarding: () -> Unit = {}
) {
    val viewModel: VisionAppViewModel = viewModel()
    var selectedTab by rememberSaveable { mutableStateOf(VisionTab.Today) }

    // Which vision the Vision tab happens to be showing — distinct from mainVisionId,
    // which is the one the widgets and Today read from. Navigation state, not data, so
    // it stays here rather than in the database.
    var selectedVisionId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Which quote is showing. Held here rather than inside Today because Studio previews
    // the same card, and the two would drift apart if each picked its own.
    var wisdomIndex by rememberSaveable { mutableIntStateOf(WISDOM.indices.random()) }

    val visions by viewModel.visions.collectAsStateWithLifecycle()
    val mainVisionId by viewModel.mainVisionId.collectAsStateWithLifecycle()
    val topThreeTasks by viewModel.topThreeTasks.collectAsStateWithLifecycle()
    val topThreeChecked by viewModel.topThreeChecked.collectAsStateWithLifecycle()
    val dayRecords by viewModel.dayRecords.collectAsStateWithLifecycle()

    // Today's header and the Insights tab read the same figures, so they're derived once
    // here rather than computed separately in each screen.
    val stats = remember(dayRecords) { topThreeStats(dayRecords, LocalDate.now().toEpochDay()) }

    // Today and the Studio preview both follow the main vision — it's the one the
    // widgets are bound to.
    val mainVision = visions.firstOrNull { it.id == mainVisionId } ?: visions.firstOrNull()

    // The nav bar floats above the content, so scrollable screens need room to
    // clear it before the system navigation inset starts.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val screenPadding = PaddingValues(bottom = NavBarHeight + NavBarMargin * 2 + navInset)

    if (showOnboarding) {
        OnboardingFlow(
            onSkip = onFinishOnboarding,
            onComplete = { data: OnboardingData ->
                // Every field is validated non-blank/non-null before the flow can
                // reach its last step, so this only guards against a stray call.
                val targetDateMillis = data.targetDateMillis
                if (data.goal.isNotBlank() && targetDateMillis != null) {
                    viewModel.createVision(data.goal, data.why, targetDateMillis)
                }
                onFinishOnboarding()
            }
        )
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Canvas)
    ) {
        when (selectedTab) {
            VisionTab.Today -> TodayScreen(
                contentPadding = screenPadding,
                vision = mainVision,
                streakDays = stats.streak,
                topThreeTasks = topThreeTasks,
                topThreeChecked = topThreeChecked,
                onSetTopThreeText = viewModel::setTopThreeText,
                onToggleTopThree = viewModel::toggleTopThreeChecked,
                onClearTopThree = viewModel::clearTopThree,
                wisdomIndex = wisdomIndex,
                onShuffleWisdom = { wisdomIndex = nextWisdomIndex(wisdomIndex) },
                onOpenVision = { selectedTab = VisionTab.Vision }
            )
            VisionTab.Vision -> VisionScreen(
                contentPadding = screenPadding,
                visions = visions,
                selectedVisionId = selectedVisionId,
                mainVisionId = mainVisionId,
                onSelectVision = { selectedVisionId = it },
                onSetMainVision = viewModel::setMainVision,
                onCreateVision = viewModel::createVision,
                onEditVision = viewModel::editVision,
                onDeleteVision = { id ->
                    viewModel.deleteVision(id)
                    // The fallback in VisionScreen picks another once this one is gone.
                    if (selectedVisionId == id) selectedVisionId = null
                },
                onAddMilestone = viewModel::addMilestone,
                onToggleMilestone = viewModel::toggleMilestone,
                onDeleteMilestone = viewModel::deleteMilestone
            )
            VisionTab.Studio -> StudioScreen(
                contentPadding = screenPadding,
                vision = mainVision,
                topThreeTasks = topThreeTasks,
                topThreeChecked = topThreeChecked,
                wisdomIndex = wisdomIndex
            )
            VisionTab.Insights -> InsightsScreen(
                contentPadding = screenPadding,
                records = dayRecords,
                onToggleTask = viewModel::toggleRecordedTask
            )
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
