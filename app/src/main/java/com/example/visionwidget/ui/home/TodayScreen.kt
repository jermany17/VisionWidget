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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.example.visionwidget.ui.ContentWidthFraction
import com.example.visionwidget.ui.components.CardFooterRow
import com.example.visionwidget.ui.components.CreateVisionRow
import com.example.visionwidget.ui.theme.AvatarFill
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.CardTheme
import com.example.visionwidget.ui.theme.CardThemes
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType
import com.example.visionwidget.ui.vision.Vision
import com.example.visionwidget.ui.vision.formatTargetDate
import com.example.visionwidget.ui.vision.formatWeeksLeft

// Mock data — pinned to the design reference until the real sources are wired up.
private const val MOCK_DATE = "SATURDAY 1 AUGUST"
private const val MOCK_GREETING = "Good morning, Jae."
private const val MOCK_INITIAL = "J"
private const val MOCK_STREAK = "17 days"
private const val MOCK_THIS_WEEK = "18 / 21"

/** Shown in place of tasks before any exist — examples of what belongs in the slot. */
private val TOP_3_PROMPTS = listOf(
    "What matters most today?",
    "What have you been putting off?",
    "One small thing for the vision"
)

// Which tasks are ticked, one bit per index. An Int needs no Saver to survive process
// death, where a Set or a List of flags would.
private fun Int.isTaskChecked(index: Int) = this and (1 shl index) != 0

private fun Int.toggleTask(index: Int) = this xor (1 shl index)

private fun Int.clearTask(index: Int) = this and (1 shl index).inv()

/** No row is being edited. */
private const val NoTaskEditing = -1

/**
 * The three slots. A null slot still shows its prompt; a string is a set task. "" stands
 * in for null on save so the list survives process death without a bespoke parcelable.
 */
private val TaskSlotsSaver: Saver<List<String?>, Any> = listSaver<List<String?>, String>(
    save = { slots -> slots.map { it.orEmpty() } },
    restore = { stored -> stored.map { it.ifEmpty { null } } }
)

private val CardShape = RoundedCornerShape(16.dp)

/** Section labels share a height so every card below one starts at the same offset. */
private val SectionLabelHeight = 20.dp

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
    vision: Vision? = null,
    onOpenVision: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userFont = UserFonts[userFontId]
    var checkedTasks by rememberSaveable { mutableIntStateOf(0) }
    var tasks by rememberSaveable(stateSaver = TaskSlotsSaver) {
        mutableStateOf(List<String?>(TOP_3_PROMPTS.size) { null })
    }
    var editingTask by rememberSaveable { mutableIntStateOf(NoTaskEditing) }
    var draft by rememberSaveable { mutableStateOf("") }

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
                action = if (vision != null) "OPEN" else null,
                trailing = if (vision != null) null else "NOT SET",
                onActionClick = onOpenVision
            )
            Spacer(Modifier.height(10.dp))
            if (vision != null) {
                VisionCard(
                    vision = vision,
                    theme = CardThemes[visionThemeId],
                    userFont = userFont,
                    onOpen = onOpenVision
                )
            } else {
                EmptyVisionCard(
                    theme = CardThemes[visionThemeId],
                    userFont = userFont,
                    onCreate = onOpenVision
                )
            }

            Spacer(Modifier.height(22.dp))
            val setCount = tasks.count { it != null }
            val doneCount = tasks.indices.count {
                tasks[it] != null && checkedTasks.isTaskChecked(it)
            }
            SectionLabel(
                label = "TODAY'S TOP 3",
                // Measured against the tasks that exist, not the full three — a lone
                // task ticked off reads as done, not a third done.
                trailing = if (setCount > 0) "$doneCount / $setCount" else "NOT SET"
            )
            Spacer(Modifier.height(10.dp))
            TopThreeCard(
                theme = CardThemes[topThreeThemeId],
                userFont = userFont,
                tasks = tasks,
                checkedTasks = checkedTasks,
                editingTask = editingTask,
                draft = draft,
                onToggleTask = { checkedTasks = checkedTasks.toggleTask(it) },
                onStartEdit = { index ->
                    draft = tasks[index].orEmpty()
                    editingTask = index
                },
                onDraftChange = { draft = it },
                onCommitEdit = {
                    // A blank field would leave a nameless row: on an unset slot the
                    // commit is dropped so the prompt stays, on a set slot the old
                    // text is kept.
                    val edited = draft.trim()
                    if (edited.isNotEmpty() && editingTask in tasks.indices) {
                        tasks = tasks.toMutableList().also { it[editingTask] = edited }
                    }
                    editingTask = NoTaskEditing
                },
                onRemoveTask = { index ->
                    // Back to a prompt, and the tick that belonged to the old task
                    // clears with it.
                    tasks = tasks.toMutableList().also { it[index] = null }
                    checkedTasks = checkedTasks.clearTask(index)
                    if (editingTask == index) editingTask = NoTaskEditing
                }
            )

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
                Text(text = action, style = VisionType.eyebrow, color = OnCanvas)
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
 * Today's three slots. Each is either a prompt waiting to be filled or a set task that
 * ticks off; the meter appears under them once any task exists.
 */
@Composable
private fun TopThreeCard(
    theme: CardTheme,
    userFont: UserFontChoice,
    tasks: List<String?>,
    checkedTasks: Int,
    editingTask: Int,
    draft: String,
    onToggleTask: (Int) -> Unit,
    onStartEdit: (Int) -> Unit,
    onDraftChange: (String) -> Unit,
    onCommitEdit: () -> Unit,
    onRemoveTask: (Int) -> Unit
) {
    ThemedCard(theme = theme) {
        // Less room above the first row than around it: the row's own 16dp and the
        // card's inset were stacking into too deep a gap at the top.
        Column(Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp)) {
            tasks.forEachIndexed { index, task ->
                if (index > 0) {
                    HorizontalDivider(color = theme.onSurfaceRule, thickness = 1.dp)
                }
                TaskRow(
                    task = task,
                    prompt = TOP_3_PROMPTS[index],
                    checked = checkedTasks.isTaskChecked(index),
                    editing = editingTask == index,
                    draft = draft,
                    theme = theme,
                    userFont = userFont,
                    onToggle = { onToggleTask(index) },
                    onStartEdit = { onStartEdit(index) },
                    onDraftChange = onDraftChange,
                    onCommitEdit = onCommitEdit,
                    onRemove = { onRemoveTask(index) }
                )
            }

            Spacer(Modifier.height(10.dp))
            if (tasks.all { it == null }) {
                // Same guidance as before the first task exists — the meter would only
                // read 0% and say nothing.
                Text(
                    text = "Choose three things that truly matter today. Three is the whole rule.",
                    style = VisionType.helperText(userFont),
                    color = theme.onSurfaceMuted
                )
            } else {
                TaskProgress(
                    done = tasks.indices.count {
                        tasks[it] != null && checkedTasks.isTaskChecked(it)
                    },
                    total = tasks.count { it != null },
                    theme = theme
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
    theme: CardTheme,
    userFont: UserFontChoice,
    onToggle: () -> Unit,
    onStartEdit: () -> Unit,
    onDraftChange: (String) -> Unit,
    onCommitEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (task == null) {
            DashedCheckMark(color = theme.onSurfaceMuted)
        } else {
            TaskCheck(checked = checked, theme = theme, onClick = onToggle)
        }
        Spacer(Modifier.width(14.dp))

        if (editing) {
            TaskField(
                draft = draft,
                theme = theme,
                userFont = userFont,
                onDraftChange = onDraftChange,
                onCommitEdit = onCommitEdit,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text(
                text = task ?: prompt,
                style = VisionType.bodyText(userFont).copy(
                    // Struck through and dimmed together: either alone reads as a
                    // styling quirk, the pair reads as done.
                    textDecoration = if (checked) TextDecoration.LineThrough else null
                ),
                // A prompt sits muted like a hint; a set task only dims once it's done.
                color = if (task == null || checked) theme.onSurfaceMuted else theme.onSurface,
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
                color = theme.onSurfaceMuted,
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
    theme: CardTheme,
    userFont: UserFontChoice,
    onDraftChange: (String) -> Unit,
    onCommitEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    // Tapping the text is the whole gesture: without this the field would open silently
    // and wait for a second tap to take focus.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = draft,
        onValueChange = onDraftChange,
        singleLine = true,
        textStyle = VisionType.bodyText(userFont).copy(color = theme.onSurface),
        cursorBrush = SolidColor(theme.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onCommitEdit() }),
        modifier = modifier
            .focusRequester(focusRequester)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val y = size.height - stroke / 2
                drawLine(
                    color = theme.onSurface,
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
private fun TaskCheck(checked: Boolean, theme: CardTheme, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (checked) {
                    Modifier.background(theme.onSurfaceMuted)
                } else {
                    Modifier.border(1.5.dp, theme.onSurfaceMuted, CircleShape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text(text = "✓", style = VisionType.eyebrow, color = theme.surface)
        }
    }
}

/**
 * Progress as a proportion of the three, rounded to a whole percent. The bar is filled
 * from the same rounded figure as the label, so the two can't disagree.
 */
@Composable
private fun TaskProgress(done: Int, total: Int, theme: CardTheme) {
    val percent = if (total == 0) 0 else Math.round(done * 100f / total)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .clip(CircleShape)
                .background(theme.onSurfaceRule)
        ) {
            if (percent > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(percent / 100f)
                        .fillMaxHeight()
                        .background(theme.onSurface)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = "$percent%",
            style = VisionType.eyebrow,
            color = theme.onSurfaceMuted
        )
    }
}

/** The empty circle that marks where a task's checkbox will sit. */
@Composable
private fun DashedCheckMark(color: Color) {
    Spacer(
        Modifier
            .size(22.dp)
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
    theme: CardTheme,
    userFont: UserFontChoice,
    onOpen: () -> Unit
) {
    ThemedCard(theme = theme, minHeight = 150.dp, onClick = onOpen) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = vision.goal,
                style = VisionType.cardTitle(userFont),
                color = theme.onSurface
            )
            // The reason is optional, so an empty one shouldn't still hold open the
            // line's height — that's what was reading as a stray gap above the rule.
            if (vision.why.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = vision.why,
                    style = VisionType.bodyText(userFont),
                    color = theme.onSurfaceMuted
                )
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = theme.onSurfaceRule, thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            // Same footer row as the empty state's, so the card keeps its height and
            // its gap under the divider whether or not a vision is set. Uppercased to
            // sit in the mono eyebrow beside the weeks.
            CardFooterRow(
                start = {
                    Text(
                        text = formatTargetDate(vision.targetDateMillis).uppercase(),
                        style = VisionType.eyebrow,
                        color = theme.onSurfaceMuted
                    )
                },
                end = {
                    Text(
                        text = formatWeeksLeft(vision.targetDateMillis),
                        style = VisionType.eyebrow,
                        color = theme.onSurfaceMuted
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
    theme: CardTheme,
    userFont: UserFontChoice,
    onCreate: () -> Unit
) {
    ThemedCard(theme = theme, minHeight = 150.dp) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "What's your vision?",
                style = VisionType.cardTitle(userFont),
                color = theme.onSurface
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "The future starts with one goal.",
                style = VisionType.bodyText(userFont),
                color = theme.onSurfaceMuted
            )

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = theme.onSurfaceRule, thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            CreateVisionRow(onClick = onCreate, mutedColor = theme.onSurfaceMuted)
        }
    }
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
