package app.visionov.visionwidget.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.visionov.visionwidget.data.AFTER_THREE_SLOTS
import app.visionov.visionwidget.data.BASE_TOP_THREE_TASKS
import app.visionov.visionwidget.data.TOP_THREE_SLOTS
import app.visionov.visionwidget.ui.theme.ResolvedLook
import app.visionov.visionwidget.data.MAX_TOP_THREE_TASKS
import app.visionov.visionwidget.data.MAX_TOP_THREE_TASK_LENGTH
import app.visionov.visionwidget.ui.ContentWidthFraction
import app.visionov.visionwidget.ui.components.CardFooterRow
import app.visionov.visionwidget.ui.components.CreateVisionRow
import app.visionov.visionwidget.ui.components.SettingsButton
import app.visionov.visionwidget.ui.components.SettingsSheet
import app.visionov.visionwidget.ui.components.croppedPhotoBackground
import app.visionov.visionwidget.ui.components.rememberWidgetPhoto
import app.visionov.visionwidget.ui.theme.AlignChoice
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.Rule
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.UserFonts
import app.visionov.visionwidget.ui.theme.VisionType
import app.visionov.visionwidget.ui.theme.WidgetLook
import app.visionov.visionwidget.ui.theme.WidgetSkin
import app.visionov.visionwidget.ui.theme.resolve
import app.visionov.visionwidget.ui.vision.Vision
import app.visionov.visionwidget.ui.vision.formatTargetDate
import app.visionov.visionwidget.ui.vision.formatWeeksLeft
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "SUNDAY 13 SEPTEMBER" — the header's own date line, uppercased to sit in the eyebrow. */
private val HeaderDateFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

/** There's no account behind the app, so the header addresses the intent, not a name. */
private const val GREETING = "Build the life you want."

/**
 * Shown in place of tasks before any exist — what belongs in each slot, by position.
 *
 * The two sets ask for different things, so they prompt for different things: the first
 * is what the day is for, the second is what would be good to reach if it goes well.
 * Naming the position rather than asking a question of it keeps all six short enough to
 * sit on one line beside the ring.
 */
private val TOP_3_PROMPTS = listOf(
    "Priority for today",
    "A key task",
    "Vision in action"
)

private val AFTER_THREE_PROMPTS = listOf(
    "If time allows",
    "Extra task",
    "Later today"
)

/** No row is being edited. */
private const val NoTaskEditing = -1

private val CardShape = RoundedCornerShape(16.dp)

/** Section labels share a height so every card below one starts at the same offset. */
private val SectionLabelHeight = 20.dp

/**
 * Looks default to the registry defaults; the caller passes what Studio applied instead
 * and nothing else here has to change.
 */
@Composable
fun TodayScreen(
    contentPadding: PaddingValues = PaddingValues(),
    userFontId: Int = UserFonts.DEFAULT_ID,
    /**
     * How each card is dressed — they're the widgets, so they follow what Studio applied
     * to that widget in particular. Everything around them stays on [userFontId].
     */
    visionLook: WidgetLook = WidgetLook(),
    topThreeLook: WidgetLook = WidgetLook(),
    wisdomLook: WidgetLook = WidgetLook(),
    vision: Vision? = null,
    streakDays: Int = 0,
    /** Tasks ticked and tasks set across this week, for the header's second figure. */
    weekDone: Int = 0,
    weekTotal: Int = 0,
    topThreeTasks: List<String?> = List(TOP_3_PROMPTS.size) { null },
    topThreeChecked: List<Boolean> = List(TOP_3_PROMPTS.size) { false },
    /** The set held back until the day's three are full — see [afterThreeUnlocked]. */
    afterThreeTasks: List<String?> = List(TOP_3_PROMPTS.size) { null },
    afterThreeChecked: List<Boolean> = List(TOP_3_PROMPTS.size) { false },
    afterThreeOpen: Boolean = false,
    /** Each set's slots in the order its card reads them — see [rowOrder]. */
    topThreeOrder: List<Int> = TOP_THREE_SLOTS.toList(),
    afterThreeOrder: List<Int> = AFTER_THREE_SLOTS.toList(),
    /** Whether the day's three are the card in front — see [topThreeLeads]. */
    topThreeFirst: Boolean = true,
    /** Whether the set behind can be ticked yet — see [afterThreeTickable]. */
    afterThreeTickable: Boolean = false,
    /** What is already written for tomorrow, in slot order, or empty while nothing is. */
    plannedTasks: List<String> = emptyList(),
    onSetPlan: (tasks: List<String>) -> Unit = {},
    onSetTopThreeText: (index: Int, text: String) -> Unit = { _, _ -> },
    onToggleTopThree: (index: Int) -> Unit = {},
    onClearTopThree: (index: Int) -> Unit = {},
    wisdomIndex: Int = 0,
    wisdomCategory: String = DEFAULT_WISDOM_THEME,
    wisdomOwnText: String = "",
    onShuffleWisdom: () -> Unit = {},
    onOpenVision: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userFont = UserFonts[userFontId]
    // Resolved once each rather than per card: the three are styled apart now, so each
    // carries its own surface, face and corner.
    val visionCard = remember(visionLook) { visionLook.resolve() }
    val topThreeCard = remember(topThreeLook) { topThreeLook.resolve() }
    val wisdomCard = remember(wisdomLook) { wisdomLook.resolve() }
    // Which row is mid-edit and its draft text — purely a UI interaction, not data, so
    // it stays local rather than in the store the committed tasks live in.
    var editingTask by rememberSaveable { mutableIntStateOf(NoTaskEditing) }
    var draft by rememberSaveable { mutableStateOf("") }
    var planningTomorrow by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // A slot is named by its place across both sets, so reading one back has to cross
    // them the same way.
    val taskAtSlot = { slot: Int ->
        if (slot < BASE_TOP_THREE_TASKS) {
            topThreeTasks.getOrNull(slot)
        } else {
            afterThreeTasks.getOrNull(slot - BASE_TOP_THREE_TASKS)
        }
    }

    // Keeping what was typed is one act however the row was left — the tick, the
    // keyboard's Done, or a tap somewhere else entirely. Walking away from a field is
    // not a way of discarding it, so all three end up here.
    //
    // A blank field would leave a nameless row: on an unset slot the commit is dropped
    // so the prompt stays, on a set slot the old text is kept. An unset extra slot is
    // dropped the same way rather than left behind as a row with nothing in it.
    val commitDraft = {
        val edited = draft.trim()
        if (edited.isNotEmpty() && editingTask in 0 until MAX_TOP_THREE_TASKS) {
            onSetTopThreeText(editingTask, edited)
        }
        editingTask = NoTaskEditing
    }

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
            Header(userFont = userFont, onOpenSettings = { showSettings = true })

            Spacer(Modifier.height(20.dp))
            MetricsRow(
                streakDays = streakDays,
                weekDone = weekDone,
                weekTotal = weekTotal,
                userFont = userFont
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Rule, thickness = 1.dp)
            Spacer(Modifier.height(20.dp))

            // With no vision set the trailing label is inert — nothing to open yet, so
            // the prompt inside the card carries the action instead.
            SectionLabel(
                label = "VISION",
                action = if (vision != null) "OPEN" else null,
                trailing = if (vision != null) null else "NOT SET",
                onActionClick = onOpenVision
            )
            Spacer(Modifier.height(10.dp))
            if (vision != null) {
                VisionCard(
                    vision = vision,
                    skin = visionCard.skin,
                    photoUri = visionCard.photoUri,
                    shape = visionCard.shape,
                    userFont = visionCard.font,
                    align = visionCard.align,
                    onOpen = onOpenVision
                )
            } else {
                EmptyVisionCard(
                    skin = visionCard.skin,
                    photoUri = visionCard.photoUri,
                    shape = visionCard.shape,
                    userFont = visionCard.font,
                    align = visionCard.align,
                    onCreate = onOpenVision
                )
            }

            Spacer(Modifier.height(22.dp))
            TopThreeSection(
                card = topThreeCard,
                sets = listOf(topThreeTasks, afterThreeTasks),
                checks = listOf(topThreeChecked, afterThreeChecked),
                orders = listOf(topThreeOrder, afterThreeOrder),
                afterThreeOpen = afterThreeOpen,
                topThreeFirst = topThreeFirst,
                afterThreeTickable = afterThreeTickable,
                editingTask = editingTask,
                draft = draft,
                onToggleTask = onToggleTopThree,
                onStartEdit = { slot ->
                    // Whatever was open keeps what was typed into it before the next
                    // row takes over.
                    commitDraft()
                    draft = taskAtSlot(slot).orEmpty()
                    editingTask = slot
                },
                // Clamped rather than called out: running out of room at the fortieth
                // character says itself, without a notice that would flash past.
                onDraftChange = { value -> draft = value.take(MAX_TOP_THREE_TASK_LENGTH) },
                onCommitEdit = commitDraft,
                // Only for the row that is actually open. A field is let go of as it is
                // torn down too, and by then the edit may have already moved on — acting
                // on that would close the row that just took over.
                onFocusLost = { slot -> if (editingTask == slot) commitDraft() },
                onRemoveTask = { slot ->
                    // Back to a prompt, and the tick that belonged to the old task
                    // clears with it.
                    onClearTopThree(slot)
                    if (editingTask == slot) editingTask = NoTaskEditing
                }
            )

            Spacer(Modifier.height(22.dp))
            PlanTomorrowRow(
                day = LocalDate.now().plusDays(1),
                plannedCount = plannedTasks.count { it.isNotBlank() },
                userFont = userFont,
                onClick = {
                    // Whatever row was open keeps what was typed into it first — the
                    // sheet takes the keyboard, and the field under it is torn down.
                    commitDraft()
                    planningTomorrow = true
                }
            )

            Spacer(Modifier.height(22.dp))
            SectionLabel(label = "DAILY WISDOM")
            Spacer(Modifier.height(10.dp))
            WisdomCard(
                skin = wisdomCard.skin,
                photoUri = wisdomCard.photoUri,
                shape = wisdomCard.shape,
                userFont = wisdomCard.font,
                align = wisdomCard.align,
                wisdom = wisdomFor(wisdomCategory, wisdomIndex, wisdomOwnText),
                // Nothing to shuffle to when the line is the user's own — there is
                // one of it, and the control would do nothing but sit there.
                onShuffle = onShuffleWisdom.takeUnless { isOwnWisdom(wisdomCategory) }
            )

            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }
    }

    if (showSettings) {
        SettingsSheet(userFont = userFont, onDismiss = { showSettings = false })
    }

    if (planningTomorrow) {
        PlanTomorrowSheet(
            day = LocalDate.now().plusDays(1),
            planned = plannedTasks,
            userFontId = userFontId,
            onDismiss = { planningTomorrow = false },
            onSet = { tasks ->
                onSetPlan(tasks)
                planningTomorrow = false
            }
        )
    }
}

@Composable
private fun Header(userFont: UserFontChoice, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        // Held to the top rather than centred: the gear carries its own band, and
        // centring this row would drop it below where the single-line tabs put it.
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                // Formatted per composition rather than remembered, so an app left open
                // overnight shows the new day rather than yesterday's date.
                text = LocalDate.now().format(HeaderDateFormat).uppercase(),
                style = VisionType.eyebrow,
                color = OnCanvas
            )
            Spacer(Modifier.height(8.dp))
            Text(text = GREETING, style = VisionType.greeting(userFont), color = OnCanvas)
        }
        SettingsButton(onClick = onOpenSettings)
    }
}

@Composable
private fun MetricsRow(
    streakDays: Int,
    weekDone: Int,
    weekTotal: Int,
    userFont: UserFontChoice
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Metric(
            label = "STREAK",
            // Same figure the Insights tab shows: days kept in a row up to yesterday.
            value = if (streakDays == 1) "1 day" else "$streakDays days",
            userFont = userFont
        )
        Spacer(Modifier.weight(1f))
        Metric(
            // Tasks ticked out of tasks set this week, not days out of seven — a week
            // is only ever measured against what was actually asked of it.
            label = "THIS WEEK",
            value = "$weekDone / $weekTotal",
            userFont = userFont
        )
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
    /**
     * Underline the action.
     *
     * For the one that moves between the two sets of three rather than opening another
     * screen: it sits where the figures usually do, and without a line under it reads
     * as another label rather than something to press.
     */
    underlineAction: Boolean = false,
    onActionClick: (() -> Unit)? = null
) {
    // Fixed height, and the action carries no padding of its own: the two trailing
    // labels swap places as the state changes, so any difference in their box would
    // move the row and the card under it.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SectionLabelHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = VisionType.eyebrow, color = OnCanvas)
        when {
            action != null -> Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(enabled = onActionClick != null) { onActionClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = action,
                    style = VisionType.eyebrow.copy(
                        textDecoration = if (underlineAction) TextDecoration.Underline else null
                    ),
                    color = OnCanvas
                )
            }

            trailing != null -> Text(
                text = trailing,
                style = VisionType.eyebrow,
                color = OnCanvas
            )
        }
    }
}

/**
 * The way into tomorrow, under the day's own three.
 *
 * Reads as an invitation until there is a plan and as a summary once there is, because
 * those are the two things it can usefully say. The date is kept on the right either
 * way — the plan is for a particular day, and a plan made late at night is the one
 * moment that is easy to be wrong about.
 */
@Composable
private fun PlanTomorrowRow(
    day: LocalDate,
    plannedCount: Int,
    userFont: UserFontChoice,
    onClick: () -> Unit
) {
    HorizontalDivider(color = Rule, thickness = 1.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (plannedCount > 0) "Tomorrow · $plannedCount planned" else "Plan tomorrow",
            // A row, not a card headline: it names what is through it rather than
            // carrying the thing itself, so it is set well below the cards above.
            style = VisionType.cardTitle(userFont)
                .copy(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.16).sp),
            color = OnCanvas,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (plannedCount > 0) "EDIT  →" else "${planDayLabel(day)}  →",
            style = VisionType.eyebrow.copy(fontSize = 9.5.sp, letterSpacing = 1.9.sp),
            color = OnCanvasMuted
        )
    }
    HorizontalDivider(color = Rule, thickness = 1.dp)
}

/**
 * The day's three, and what the user lined up to follow them.
 *
 * Two cards side by side rather than one list that grows. The second is only reachable
 * once the first is full, which is what [afterThreeUnlocked] decides; until then this is
 * the single card it has always been, with nothing to say it could be anything else.
 *
 * The label above carries whichever set is showing and its own figures, and the control
 * opposite it names the other one. Both the swipe and that control move between them, so
 * the page can be reached whether or not the gesture is discovered.
 */
@Composable
private fun TopThreeSection(
    card: ResolvedLook,
    /** Both sets, the day's three at index 0 — their slots, not their place on screen. */
    sets: List<List<String?>>,
    checks: List<List<Boolean>>,
    /** Each set's slots in the order its own card reads them. */
    orders: List<List<Int>>,
    afterThreeOpen: Boolean,
    /** Whether the day's three are the card in front — see [topThreeLeads]. */
    topThreeFirst: Boolean,
    /** Whether the set behind can be ticked yet — see [afterThreeTickable]. */
    afterThreeTickable: Boolean,
    editingTask: Int,
    draft: String,
    onToggleTask: (Int) -> Unit,
    onStartEdit: (Int) -> Unit,
    onDraftChange: (String) -> Unit,
    onCommitEdit: () -> Unit,
    onFocusLost: (Int) -> Unit,
    onRemoveTask: (Int) -> Unit
) {
    val pageCount = if (afterThreeOpen) 2 else 1
    val pager = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    // Which set each page holds. The order is not kept anywhere — it is read off the
    // slots every time, so the card in front is whichever one has work left in it.
    val onPage = if (topThreeFirst) listOf(0, 1) else listOf(1, 0)

    // Back to the front of the row whenever the order changes under it. The card that
    // has just moved to the front is the one being asked for next, and being left
    // looking at the other one is being left behind by your own tick.
    LaunchedEffect(topThreeFirst) {
        if (pager.currentPage != 0) pager.animateScrollToPage(0)
    }

    // The page the pager is on can outlive the second set: empty it and close it while
    // standing on it, and the pager is left pointing past the end for a frame.
    val page = pager.currentPage.coerceIn(0, pageCount - 1)
    val shown = onPage[page]
    val tasks = sets[shown]
    val checked = checks[shown]
    val setCount = tasks.count { it != null }
    val doneCount = tasks.indices.count { tasks[it] != null && checked[it] }

    val nameOf = { set: Int -> if (set == 0) "TODAY'S TOP 3" else "AFTER THREE" }
    val name = nameOf(shown)
    SectionLabel(
        // Measured against the tasks that exist, not the full three — a lone task
        // ticked off reads as done, not a third done.
        label = if (setCount > 0) "$name · $doneCount / $setCount" else name,
        action = when {
            !afterThreeOpen -> null
            // Names the card it would take you to, which is the other set whichever way
            // round the two happen to be standing.
            page == 0 -> "${if (shown == 0) "AFTER THREE" else "TOP 3"}  →"
            else -> "←  ${if (shown == 0) "AFTER THREE" else "TOP 3"}"
        },
        trailing = if (setCount == 0 && !afterThreeOpen) "NOT SET" else null,
        underlineAction = true,
        onActionClick = { scope.launch { pager.animateScrollToPage(1 - page) } }
    )

    Spacer(Modifier.height(10.dp))
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    // Where the card beside this one is given room to show: the screen's own side
    // margin, worked back from the share of it the content column takes.
    //
    // The page itself stays the width of every other card on the screen. One that shrank
    // to make room for its neighbour would sit inside the vision above it and read as a
    // different kind of thing — so instead the pager is widened past the column by that
    // margin on either side, and the neighbour shows in the screen's edge rather than in
    // the card's. The box centres it, which puts the overhang evenly on both.
    val sideMargin = if (pageCount > 1) {
        maxWidth * ((1f / ContentWidthFraction - 1f) / 2f)
    } else {
        0.dp
    }
    HorizontalPager(
        state = pager,
        pageSpacing = 10.dp,
        contentPadding = PaddingValues(horizontal = sideMargin),
        // Kept off while a row is open: the field is inside the page, and dragging to
        // place a caret would otherwise carry the whole card away instead.
        userScrollEnabled = editingTask == NoTaskEditing,
        modifier = Modifier.requiredWidth(maxWidth + sideMargin * 2)
    ) { index ->
        val set = onPage[index]
        TopThreeCard(
            skin = card.skin,
            photoUri = card.photoUri,
            shape = card.shape,
            userFont = card.font,
            align = card.align,
            tasks = sets[set],
            checkedTasks = checks[set],
            slotOffset = set * BASE_TOP_THREE_TASKS,
            order = orders[set],
            // The set behind is a plan until the day's three are done, and says so by
            // standing a tone lighter with its rings left open.
            pending = set == 1 && !afterThreeTickable,
            editingTask = editingTask,
            draft = draft,
            onToggleTask = onToggleTask,
            onStartEdit = onStartEdit,
            onDraftChange = onDraftChange,
            onCommitEdit = onCommitEdit,
            onFocusLost = onFocusLost,
            onRemoveTask = onRemoveTask
        )
    }
    }

    if (pageCount > 1) {
        Spacer(Modifier.height(12.dp))
        PagerDots(count = pageCount, current = page)
    }
}

/** Which of the two cards is showing. The one being read is drawn as a bar, not a dot. */
@Composable
private fun PagerDots(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val here = index == current
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (here) 18.dp else 6.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (here) OnCanvas else Rule)
            )
        }
    }
}

/**
 * One set's three slots. Each is either a prompt waiting to be filled or a set task that
 * ticks off; the meter appears under them once any task exists.
 */
@Composable
private fun TopThreeCard(
    skin: WidgetSkin,
    photoUri: String?,
    shape: RoundedCornerShape,
    userFont: UserFontChoice,
    align: AlignChoice,
    tasks: List<String?>,
    checkedTasks: List<Boolean>,
    /**
     * Where this set's slots begin, so a row can name itself to the store.
     *
     * The two sets are three slots each at fixed positions, so a row's place on the
     * card and its place in the day are the same thing plus this.
     */
    slotOffset: Int,
    /**
     * The set's slots in the order the card reads them — see [rowOrder].
     *
     * What is left to do rises to the top and what is done sinks below it, so a card is
     * always a list of what is outstanding. The numbers here are slots across both sets,
     * which is how a row names itself to the store however far it has moved.
     */
    order: List<Int>,
    /** The set waiting its turn — see [TaskRow]'s own note. */
    pending: Boolean,
    editingTask: Int,
    draft: String,
    onToggleTask: (Int) -> Unit,
    onStartEdit: (Int) -> Unit,
    onDraftChange: (String) -> Unit,
    onCommitEdit: () -> Unit,
    /** Which row's field was let go of. */
    onFocusLost: (Int) -> Unit,
    onRemoveTask: (Int) -> Unit
) {
    ThemedCard(skin = skin, photoUri = photoUri, shape = shape, lifted = pending) {
        // Less room above the first row than around it: the row's own 16dp and the
        // card's inset were stacking into too deep a gap at the top.
        Column(Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp)) {
            order.forEachIndexed { position, slot ->
                if (position > 0) {
                    HorizontalDivider(color = skin.onSurfaceRule, thickness = 1.dp)
                }
                val within = slot - slotOffset
                val task = tasks[within]
                // The prompt belongs to the slot rather than to where it has ended up,
                // so an empty one asks the same thing wherever the order has put it.
                val prompts = if (slotOffset == 0) TOP_3_PROMPTS else AFTER_THREE_PROMPTS
                TaskRow(
                    task = task,
                    prompt = prompts.getOrElse(within) { "" },
                    checked = checkedTasks[within],
                    editing = editingTask == slot,
                    draft = draft,
                    skin = skin,
                    userFont = userFont,
                    align = align,
                    pending = pending,
                    onToggle = { if (!pending) onToggleTask(slot) },
                    onStartEdit = { onStartEdit(slot) },
                    onDraftChange = onDraftChange,
                    onCommitEdit = onCommitEdit,
                    onFocusLost = { onFocusLost(slot) },
                    onRemove = { onRemoveTask(slot) }
                )
            }

            Spacer(Modifier.height(10.dp))
            when {
                // Says what the set is for rather than how it is going. Nothing here is
                // ticked off while it waits, so a meter would read nothing but zero.
                pending -> Text(
                    text = align.format("Fills in after your three are done."),
                    style = VisionType.helperText(userFont),
                    color = skin.onSurfaceMuted,
                    textAlign = align.textAlign,
                    modifier = Modifier.fillMaxWidth()
                )

                // Same guidance as before the first task exists — the meter would only
                // read 0% and say nothing.
                tasks.all { it == null } -> Text(
                    text = align.format(
                        "Choose three things that truly matter today. Three is the whole rule."
                    ),
                    style = VisionType.helperText(userFont),
                    color = skin.onSurfaceMuted,
                    textAlign = align.textAlign,
                    modifier = Modifier.fillMaxWidth()
                )

                else -> TaskProgress(
                    done = tasks.indices.count { tasks[it] != null && checkedTasks[it] },
                    total = tasks.count { it != null },
                    skin = skin
                )
            }
        }
    }
}


/**
 * One slot: its prompt, the field that fills it, or the task it now holds. The leading
 * mark stays a dashed ring until a task exists; the trailing control follows the mode —
 * absent on a prompt, a confirm while editing, a dismiss on a set task.
 */
@Composable
private fun TaskRow(
    task: String?,
    prompt: String,
    checked: Boolean,
    editing: Boolean,
    draft: String,
    skin: WidgetSkin,
    userFont: UserFontChoice,
    align: AlignChoice,
    /**
     * The row belongs to the set waiting its turn.
     *
     * An unticked one wears the open ring an unfilled slot does, so a set written ahead
     * of time reads as lined up rather than under way. Ticked, it is drawn like any
     * other — it has been done, whichever set it was sitting in at the time.
     */
    pending: Boolean = false,
    onToggle: () -> Unit,
    onStartEdit: () -> Unit,
    onDraftChange: (String) -> Unit,
    onCommitEdit: () -> Unit,
    onFocusLost: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (task == null || (pending && !checked)) {
            DashedCheckMark(
                color = skin.onSurfaceMuted,
                // An empty slot has nothing to tick and a locked one is not the day's
                // work yet, so neither takes the tap.
                onClick = null
            )
        } else {
            // Faded while the set is locked: the tick was earned and is not taken away,
            // but it belongs to work the day has not come round to yet.
            Box(Modifier.alpha(if (pending) 0.45f else 1f)) {
                TaskCheck(checked = checked, skin = skin, onClick = onToggle)
            }
        }
        Spacer(Modifier.width(14.dp))

        if (editing) {
            TaskField(
                draft = draft,
                skin = skin,
                userFont = userFont,
                onDraftChange = onDraftChange,
                onCommitEdit = onCommitEdit,
                onFocusLost = onFocusLost,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = align.format(task ?: prompt),
                style = VisionType.bodyText(userFont).copy(
                    // Struck through and dimmed together: either alone reads as a
                    // styling quirk, the pair reads as done.
                    textDecoration = if (checked) TextDecoration.LineThrough else null
                ),
                // A prompt sits muted like a hint; a set task only dims once it's done.
                color = if (task == null || checked) skin.onSurfaceMuted else skin.onSurface,
                textAlign = align.textAlign,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onStartEdit)
            )
        }

        if (editing || task != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (editing) "✓" else "✕",
                style = VisionType.glyph,
                color = skin.onSurfaceMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = if (editing) onCommitEdit else onRemove)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TaskField(
    draft: String,
    skin: WidgetSkin,
    userFont: UserFontChoice,
    onDraftChange: (String) -> Unit,
    onCommitEdit: () -> Unit,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    // Tapping the text is the whole gesture: without this the field would open silently
    // and wait for a second tap to take focus.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // A field starts out unfocused and only takes focus once the request above lands,
    // so losing it counts for something only after it has actually held it — otherwise
    // the row would close itself the moment it opened.
    var hadFocus by remember { mutableStateOf(false) }

    BasicTextField(
        value = draft,
        onValueChange = onDraftChange,
        singleLine = true,
        textStyle = VisionType.bodyText(userFont).copy(color = skin.onSurface),
        cursorBrush = SolidColor(skin.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onCommitEdit() }),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) hadFocus = true else if (hadFocus) onFocusLost()
            }
            .drawBehind {
                val stroke = 1.dp.toPx()
                val y = size.height - stroke / 2
                drawLine(
                    color = skin.onSurface,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = stroke
                )
            }
            .padding(bottom = 4.dp)
    )
}

/** Empty ring until ticked, then a filled disc carrying the mark. */
@Composable
private fun TaskCheck(checked: Boolean, skin: WidgetSkin, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(skin.onSurfaceMuted)
                } else {
                    Modifier.border(1.5.dp, skin.onSurfaceMuted, CircleShape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text(text = "✓", style = VisionType.eyebrow, color = skin.onInk)
        }
    }
}

/**
 * Progress as a proportion of the three, rounded to a whole percent. The bar is filled
 * from the same rounded figure as the label, so the two can't disagree.
 */
@Composable
private fun TaskProgress(done: Int, total: Int, skin: WidgetSkin) {
    val percent = if (total == 0) 0 else Math.round(done * 100f / total)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .clip(CircleShape)
                .background(skin.onSurfaceRule)
        ) {
            if (percent > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(percent / 100f)
                        .fillMaxHeight()
                        .background(skin.onSurface)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = "$percent%",
            style = VisionType.eyebrow,
            color = skin.onSurfaceMuted
        )
    }
}

/** The empty circle that marks where a task's checkbox will sit. */
@Composable
private fun DashedCheckMark(color: Color, onClick: (() -> Unit)? = null) {
    Spacer(
        Modifier
            .size(22.dp)
            // An empty slot has nothing to tick, so it takes no tap. A slot in the
            // waiting set does: it wears the open ring to say it is not under way yet,
            // not to say it is out of reach.
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clip(CircleShape).clickable(onClick = onClick)
                }
            )
            .drawBehind {
                val stroke = 1.5.dp.toPx()
                drawCircle(
                    color = color,
                    // Inset by half the stroke so the ring stays inside the 22dp box.
                    radius = (size.minDimension - stroke) / 2,
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(3.dp.toPx(), 3.dp.toPx())
                        )
                    )
                )
            }
    )
}

/**
 * The vision itself, once one is set — the goal and its reason, over the target date
 * and how long is left. Tapping anywhere on it opens the Vision tab.
 */
@Composable
private fun VisionCard(
    vision: Vision,
    skin: WidgetSkin,
    photoUri: String?,
    shape: RoundedCornerShape,
    userFont: UserFontChoice,
    align: AlignChoice,
    onOpen: () -> Unit
) {
    ThemedCard(skin = skin, photoUri = photoUri, shape = shape, minHeight = 150.dp, onClick = onOpen) {
        Column(Modifier.padding(20.dp), horizontalAlignment = align.horizontal) {
            Text(
                text = align.format(vision.goal),
                style = VisionType.cardTitle(userFont),
                color = skin.onSurface,
                textAlign = align.textAlign,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                // The reason is optional, so it can be absent here — matches the
                // Vision tab's own fallback rather than leaving the line blank.
                text = align.format(vision.why.ifBlank { "No reason set." }),
                style = VisionType.bodyText(userFont),
                color = skin.onSurfaceMuted,
                textAlign = align.textAlign,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = skin.onSurfaceRule, thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            // Same footer row as the empty state's, so the card keeps its height and
            // its gap under the divider whether or not a vision is set. Uppercased to
            // sit in the mono eyebrow beside the weeks.
            CardFooterRow(
                start = {
                    Text(
                        text = formatTargetDate(vision.targetDateMillis).uppercase(),
                        style = VisionType.eyebrow,
                        color = skin.onSurfaceMuted
                    )
                },
                end = {
                    Text(
                        text = formatWeeksLeft(vision.targetDateMillis),
                        style = VisionType.eyebrow,
                        color = skin.onSurfaceMuted
                    )
                }
            )
        }
    }
}

/**
 * Placeholder shown until a vision exists. Takes the same themed surface as the filled
 * card, so the slot keeps its colour whether or not a vision is set.
 */
@Composable
private fun EmptyVisionCard(
    skin: WidgetSkin,
    photoUri: String?,
    shape: RoundedCornerShape,
    userFont: UserFontChoice,
    align: AlignChoice,
    onCreate: () -> Unit
) {
    ThemedCard(skin = skin, photoUri = photoUri, shape = shape, minHeight = 150.dp) {
        Column(Modifier.padding(20.dp), horizontalAlignment = align.horizontal) {
            Text(
                text = align.format("What's your vision?"),
                style = VisionType.cardTitle(userFont),
                color = skin.onSurface,
                textAlign = align.textAlign,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = align.format("The future starts with one goal."),
                style = VisionType.bodyText(userFont),
                color = skin.onSurfaceMuted,
                textAlign = align.textAlign,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = skin.onSurfaceRule, thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            CreateVisionRow(onClick = onCreate, mutedColor = skin.onSurfaceMuted)
        }
    }
}

/**
 * Which quote is showing is held by the caller rather than here: Studio previews the
 * widget alongside this card, and the two would drift apart if each picked its own.
 */
@Composable
private fun WisdomCard(
    skin: WidgetSkin,
    photoUri: String?,
    shape: RoundedCornerShape,
    userFont: UserFontChoice,
    align: AlignChoice,
    wisdom: Wisdom,
    /** Null when there is nothing to shuffle to, which leaves the control off. */
    onShuffle: (() -> Unit)?
) {
    // No minimum height: a floor would leave slack under the footer on short quotes,
    // so the gap below the meta row would grow as the quote got shorter.
    ThemedCard(skin = skin, photoUri = photoUri, shape = shape) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = align.format(wisdom.text),
                style = VisionType.quote(userFont),
                color = skin.onSurface,
                textAlign = align.textAlign,
                modifier = Modifier.fillMaxWidth()
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
                    color = skin.onSurfaceMuted
                )
                if (onShuffle != null) {
                    Text(
                        text = "SHUFFLE ↻",
                        style = VisionType.eyebrow,
                        color = skin.onSurfaceMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onShuffle)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
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
    skin: WidgetSkin,
    photoUri: String?,
    shape: RoundedCornerShape,
    minHeight: Dp = 0.dp,
    /**
     * The card is not in play yet, and is drawn a tone lighter to say so.
     *
     * A wash of the page's own colour over the surface rather than a second colour of
     * its own: whatever the card was dressed in, lifting it towards the page is the one
     * move that reads the same on all of them — and it says the card is further off
     * without saying it is a different kind of card.
     */
    lifted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit = {}
) {
    // A chosen picture replaces the fill entirely; the scrim over it is what keeps the
    // words readable, so it's painted whether the picture loaded or not.
    val photo = rememberWidgetPhoto(photoUri.takeIf { skin.usesPhoto })
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // A floor rather than a fixed height, so cards with content can grow.
            .heightIn(min = minHeight)
            .clip(shape)
            .then(
                if (photo != null) {
                    Modifier.croppedPhotoBackground(photo)
                } else {
                    Modifier.background(skin.background)
                }
            )
            .then(skin.overlay?.let { Modifier.background(it) } ?: Modifier)
            .then(if (lifted) Modifier.background(Canvas.copy(alpha = 0.3f)) else Modifier)
            .then(skin.border?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
            // Clickable last so the ripple lands inside the clipped, painted card.
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides skin.onSurface) {
            content()
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 900)
@Composable
private fun TodayScreenPreview() {
    TodayScreen()
}
