package app.visionov.visionwidget.ui.navigation

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.visionov.visionwidget.billing.BillingManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.visionov.visionwidget.data.VisionAppViewModel
import app.visionov.visionwidget.data.tallyBetween
import app.visionov.visionwidget.data.topThreeStats
import app.visionov.visionwidget.ui.gallery.LibraryScreen
import app.visionov.visionwidget.ui.home.TodayScreen
import app.visionov.visionwidget.widget.requestPinWidget
import app.visionov.visionwidget.ui.home.nextWisdomIndex
import app.visionov.visionwidget.ui.home.randomWisdomIndex
import app.visionov.visionwidget.ui.home.wisdomIndices
import app.visionov.visionwidget.ui.insights.InsightsScreen
import app.visionov.visionwidget.ui.studio.StudioScreen
import app.visionov.visionwidget.ui.onboarding.OnboardingData
import app.visionov.visionwidget.ui.onboarding.OnboardingFlow
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.NavBar
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnNavBar
import app.visionov.visionwidget.ui.theme.VisionType
import app.visionov.visionwidget.ui.theme.WidgetLook
import app.visionov.visionwidget.ui.theme.WidgetTarget
import app.visionov.visionwidget.ui.vision.VisionScreen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Falls back to the defaults rather than throwing if a target was never written. */
private fun Map<WidgetTarget, WidgetLook>.lookOf(target: WidgetTarget) =
    this[target] ?: WidgetLook()

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
    onFinishOnboarding: () -> Unit = {},
    /** A Gallery widget's own tap asks for this — Studio's Gallery tab, not Today. */
    openGallery: Boolean = false,
    /** A Vision widget's own tap asks for this — the Vision tab, not Today. */
    openVision: Boolean = false
) {
    val viewModel: VisionAppViewModel = viewModel()
    var selectedTab by rememberSaveable {
        mutableStateOf(
            when {
                openGallery -> VisionTab.Studio
                openVision -> VisionTab.Vision
                else -> VisionTab.Today
            }
        )
    }

    // Where the user has asked to go while Studio still has something unapplied.
    //
    // Studio's drafts live inside Studio, so it is the one that can say whether leaving
    // would lose anything and the one that asks. The tab is held here until it answers:
    // a design chosen and not applied still has the widgets showing the one before it,
    // and walking away from that silently is the one thing that cannot be undone from
    // the screen you land on.
    var pendingTab by remember { mutableStateOf<VisionTab?>(null) }

    // Which vision the Vision tab happens to be showing — distinct from mainVisionId,
    // which is the one the widgets and Today read from. Navigation state, not data, so
    // it stays here rather than in the database.
    var selectedVisionId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Whether My Library is up over the tabs. An overlay rather than a fifth tab: it is
    // reached from the Gallery shelf and from the launcher's own setting screen, and
    // belongs under Studio rather than beside it. Navigation state, as above.
    var showLibrary by rememberSaveable { mutableStateOf(false) }

    /** A launcher that won't be asked to place a widget, which has to be said out loud. */
    var pinRefused by rememberSaveable { mutableStateOf(false) }

    val billing = BillingManager.get(LocalContext.current)

    // A process can sit in the background through a midnight, and a widget can turn the
    // daily line over while it does, so the day is checked on every return to the front
    // rather than at launch alone.
    //
    // Play is asked again on the same beat. A subscription can be bought, cancelled or
    // refunded in the Play app with this one in the background, and nothing announces
    // it — the line to Play is dropped when the process sleeps, so returning is the
    // moment to put it back up and ask what is actually held.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshWisdomForToday()
        billing.refresh()
        onPauseOrDispose { }
    }

    val libraryDesignIds by viewModel.libraryDesignIds.collectAsStateWithLifecycle()

    val wisdomCategory by viewModel.wisdomCategory.collectAsStateWithLifecycle()

    // Which quote is showing. Kept in the store rather than here: Studio previews the
    // same card and the widget draws it, and three copies of one choice would drift.
    val wisdomIndex by viewModel.wisdomIndex.collectAsStateWithLifecycle()

    /** The one line the user wrote themselves, shown when the theme is their own. */
    val wisdomOwnText by viewModel.wisdomOwnText.collectAsStateWithLifecycle()

    val visions by viewModel.visions.collectAsStateWithLifecycle()
    val mainVisionId by viewModel.mainVisionId.collectAsStateWithLifecycle()
    val topThreeTasks by viewModel.topThreeTasks.collectAsStateWithLifecycle()
    val topThreeChecked by viewModel.topThreeChecked.collectAsStateWithLifecycle()
    val afterThreeTasks by viewModel.afterThreeTasks.collectAsStateWithLifecycle()
    val afterThreeChecked by viewModel.afterThreeChecked.collectAsStateWithLifecycle()
    val afterThreeOpen by viewModel.afterThreeOpen.collectAsStateWithLifecycle()
    val topThreeOrder by viewModel.topThreeOrder.collectAsStateWithLifecycle()
    val afterThreeOrder by viewModel.afterThreeOrder.collectAsStateWithLifecycle()
    val topThreeFirst by viewModel.topThreeFirst.collectAsStateWithLifecycle()
    val afterThreeTickable by viewModel.afterThreeTickable.collectAsStateWithLifecycle()
    val plannedTasks by viewModel.plannedTasks.collectAsStateWithLifecycle()
    val dayRecords by viewModel.dayRecords.collectAsStateWithLifecycle()
    val widgetLooks by viewModel.widgetLooks.collectAsStateWithLifecycle()

    // Today's header and the Insights tab read the same figures, so they're derived once
    // here rather than computed separately in each screen.
    val stats = remember(dayRecords) { topThreeStats(dayRecords, LocalDate.now().toEpochDay()) }

    // Weeks run Sunday to Saturday here as they do in Insights, so the two screens
    // can't disagree about which days "this week" covers.
    val weekTally = remember(dayRecords) {
        val start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        tallyBetween(dayRecords, start.toEpochDay(), start.plusDays(6).toEpochDay())
    }

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
                // The theme the last step asked for. Skipping it leaves the daily line
                // drawn from all of them, which is what it was before being asked.
                data.wisdomCategory?.let(viewModel::setWisdomCategory)
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
                // Each card wears what Studio applied to that widget — the three can be
                // styled apart, so none of them borrows another's look.
                visionLook = widgetLooks.lookOf(WidgetTarget.Vision),
                topThreeLook = widgetLooks.lookOf(WidgetTarget.TopThree),
                wisdomLook = widgetLooks.lookOf(WidgetTarget.Wisdom),
                vision = mainVision,
                streakDays = stats.streak,
                weekDone = weekTally.done,
                weekTotal = weekTally.total,
                topThreeTasks = topThreeTasks,
                topThreeChecked = topThreeChecked,
                afterThreeTasks = afterThreeTasks,
                afterThreeChecked = afterThreeChecked,
                afterThreeOpen = afterThreeOpen,
                topThreeOrder = topThreeOrder,
                afterThreeOrder = afterThreeOrder,
                topThreeFirst = topThreeFirst,
                afterThreeTickable = afterThreeTickable,
                plannedTasks = plannedTasks,
                onSetPlan = viewModel::setPlan,
                onSetTopThreeText = viewModel::setTopThreeText,
                onToggleTopThree = viewModel::toggleTopThreeChecked,
                onClearTopThree = viewModel::clearTopThree,
                wisdomIndex = wisdomIndex,
                wisdomCategory = wisdomCategory,
                wisdomOwnText = wisdomOwnText,
                onShuffleWisdom = viewModel::shuffleWisdom,
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
                looks = widgetLooks,
                onApplyStyle = viewModel::applyWidgetStyle,
                onPickPhoto = viewModel::setWidgetPhoto,
                wisdomCategory = wisdomCategory,
                wisdomOwnText = wisdomOwnText,
                onApplyWisdom = viewModel::applyWisdom,
                vision = mainVision,
                topThreeTasks = topThreeTasks,
                topThreeChecked = topThreeChecked,
                wisdomIndex = wisdomIndex,
                startOnGallery = openGallery,
                libraryIds = libraryDesignIds,
                onSaveToLibrary = viewModel::saveToLibrary,
                onOpenLibrary = { showLibrary = true },
                leaveRequested = pendingTab != null,
                onLeaveResolved = { allowed ->
                    if (allowed) pendingTab?.let { selectedTab = it }
                    pendingTab = null
                }
            )
            VisionTab.Insights -> InsightsScreen(
                contentPadding = screenPadding,
                records = dayRecords,
                onToggleTask = viewModel::toggleRecordedTask
            )
        }

        // Over the tabs rather than beside them, so the nav bar goes with them — the
        // library has its own way back and nothing below it to reach for.
        if (showLibrary) {
            val context = LocalContext.current
            LibraryScreen(
                designIds = libraryDesignIds,
                onRemove = viewModel::removeFromLibrary,
                // A handful of launchers refuse to be asked. Saying so beats a tap that
                // looks like it worked and left nothing on the screen.
                onPlace = { size, slot ->
                    if (!requestPinWidget(context, size, slot)) pinRefused = true
                },
                onBrowse = {
                    showLibrary = false
                    selectedTab = VisionTab.Studio
                },
                onBack = { showLibrary = false },
                contentPadding = screenPadding
            )
            BackHandler { showLibrary = false }

            if (pinRefused) {
                AlertDialog(
                    onDismissRequest = { pinRefused = false },
                    confirmButton = {
                        TextButton(onClick = { pinRefused = false }) { Text("OK") }
                    },
                    title = { Text("Add it from the home screen") },
                    text = {
                        Text(
                            "This launcher won't let an app place a widget for you. " +
                                "Press and hold the home screen, find Vision Widget, " +
                                "and pick the size you want."
                        )
                    },
                    containerColor = Canvas
                )
            }
        } else {
            BottomNav(
                selected = selectedTab,
                // Leaving Studio is asked of Studio rather than done to it. Every other
                // move is immediate — there is nothing anywhere else that a tab change
                // would lose.
                onSelect = { tab ->
                    if (selectedTab == VisionTab.Studio && tab != VisionTab.Studio) {
                        pendingTab = tab
                    } else {
                        selectedTab = tab
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(vertical = NavBarMargin)
            )
        }
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
