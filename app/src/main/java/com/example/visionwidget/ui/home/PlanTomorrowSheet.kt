package com.example.visionwidget.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionwidget.data.BASE_TOP_THREE_TASKS
import com.example.visionwidget.data.MAX_TOP_THREE_TASKS
import com.example.visionwidget.data.MAX_TOP_THREE_TASK_LENGTH
import com.example.visionwidget.ui.components.PromptDialog
import com.example.visionwidget.ui.theme.AvatarFill
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.NavBar
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.OnNavBar
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "THU 5" — the day the plan is for, as the row and the sheet both name it. */
private val PlanDayFormat = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH)

fun planDayLabel(day: LocalDate): String = day.format(PlanDayFormat).uppercase()

/**
 * What each slot asks for when nothing has been written in it.
 *
 * The same shape Today's prompts take, said for a day that has not started: the first
 * three are what the day is to be about, the rest what would be good to reach as well.
 */
private val PLAN_TOP_PROMPTS = listOf(
    "Priority for tomorrow",
    "A key task",
    "Vision in action"
)

private val PLAN_AFTER_PROMPTS = listOf(
    "If time allows",
    "Extra task",
    "Later that day"
)

/** Placeholder ink — fainter than body text, as the other sheets set it. */
private val PlanHint = OnCanvas.copy(alpha = 0.3f)

/**
 * Writes the day ahead before it starts.
 *
 * The same two sets the day itself has: three that are what the day is for, and up to
 * three more behind them. The second set is asked for a line at a time rather than laid
 * out empty — a plan is written in one sitting, and three blank rows below three full
 * ones read as work left undone before the day has even begun.
 *
 * Nothing here is ticked. These are not tasks yet; they become them when the day
 * arrives, and until then the only question is what they are.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTomorrowSheet(
    day: LocalDate,
    /** What was planned before, in slot order, if this is being opened to be changed. */
    planned: List<String>,
    onDismiss: () -> Unit,
    onSet: (tasks: List<String>) -> Unit,
    modifier: Modifier = Modifier,
    userFontId: Int = UserFonts.DEFAULT_ID
) {
    val userFont = UserFonts[userFontId]
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val tasks = remember {
        mutableStateListOf<String>().apply {
            repeat(MAX_TOP_THREE_TASKS) { index -> add(planned.getOrElse(index) { "" }) }
        }
    }

    // How many of the second set's rows are on the sheet. It opens on however many were
    // already written, so a plan being changed comes back as it was left.
    var afterShown by rememberSaveable {
        mutableIntStateOf(
            (BASE_TOP_THREE_TASKS until MAX_TOP_THREE_TASKS)
                .count { planned.getOrElse(it) { "" }.isNotBlank() }
        )
    }

    val topFull = (0 until BASE_TOP_THREE_TASKS).all { tasks[it].isNotBlank() }
    val afterWritten = (BASE_TOP_THREE_TASKS until MAX_TOP_THREE_TASKS).any { tasks[it].isNotBlank() }

    // The same rule the day itself opens its second set by: filling the first three is
    // what offers another, and anything written there is what keeps the offer standing.
    val canAddMore = (topFull || afterWritten) && afterShown < BASE_TOP_THREE_TASKS
    // Emptying the first three takes the offer back, but only while there is nothing
    // behind it to lose.
    if (!topFull && !afterWritten && afterShown > 0) afterShown = 0

    val ready = tasks.any { it.isNotBlank() }

    // Whether anything has been written or changed since the sheet opened. Measured
    // against what it was opened with rather than against empty, so coming back to a
    // plan and leaving it exactly as it was is not something to be asked about.
    val started = (0 until MAX_TOP_THREE_TASKS).any {
        tasks[it].trim() != planned.getOrElse(it) { "" }.trim()
    }

    var confirmLeaving by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    /**
     * Closing the sheet, or asking first.
     *
     * The plan is only kept by the button at the foot of the sheet, so one begun and not
     * set is lost by the drag, the scrim and the handle alike. The sheet is shown again
     * as the question goes up: a drag has carried it most of the way out by then, and a
     * question hanging over nothing is worse than the loss it is asking about.
     */
    val leaveOrAsk: () -> Unit = {
        if (started) {
            confirmLeaving = true
            scope.launch { sheetState.show() }
        } else {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = leaveOrAsk,
        sheetState = sheetState,
        containerColor = Canvas,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                text = "TOMORROW · ${planDayLabel(day)}",
                style = VisionType.eyebrow.copy(fontSize = 9.5.sp, letterSpacing = 1.9.sp),
                color = OnCanvasMuted
            )

            Spacer(Modifier.height(18.dp))
            repeat(BASE_TOP_THREE_TASKS) { index ->
                PlanRow(
                    value = tasks[index],
                    placeholder = PLAN_TOP_PROMPTS[index],
                    userFont = userFont,
                    onValueChange = { tasks[index] = it.take(MAX_TOP_THREE_TASK_LENGTH) }
                )
            }

            if (afterShown > 0) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "AFTER THREE",
                    // A step under the sheet's own label: it divides the rows rather
                    // than naming the sheet, so it is the quieter of the two.
                    style = VisionType.eyebrow.copy(fontSize = 9.sp, letterSpacing = 1.8.sp),
                    color = OnCanvasMuted
                )
                Spacer(Modifier.height(8.dp))
                repeat(afterShown) { offset ->
                    val index = BASE_TOP_THREE_TASKS + offset
                    PlanRow(
                        value = tasks[index],
                        placeholder = PLAN_AFTER_PROMPTS[offset],
                        userFont = userFont,
                        onValueChange = { tasks[index] = it.take(MAX_TOP_THREE_TASK_LENGTH) }
                    )
                }
            }

            if (canAddMore) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "+ One more",
                    style = VisionType.bodyText(userFont),
                    color = OnCanvasMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { afterShown++ }
                        .padding(vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (ready) NavBar else AvatarFill)
                    .clickable(enabled = ready) { onSet(tasks.toList()) }
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Set tomorrow",
                    style = VisionType.buttonLabel(userFont).copy(fontSize = 15.sp),
                    color = if (ready) OnNavBar else OnCanvasMuted
                )
            }
        }
    }

    if (confirmLeaving) {
        PromptDialog(
            eyebrow = "TOMORROW",
            title = "Leave without saving?",
            body = "Tomorrow's plan hasn't been set yet.\n" +
                "If you leave now, your progress will be lost.",
            userFont = userFont,
            quietLabel = "Discard",
            onQuiet = {
                confirmLeaving = false
                onDismiss()
            },
            loudLabel = "Keep editing",
            onLoud = { confirmLeaving = false }
        )
    }
}

/** One line of the plan: what it is, and the rule under it. */
@Composable
private fun PlanRow(
    value: String,
    placeholder: String,
    userFont: UserFontChoice,
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    // Drawn a hair tighter than the face sets it, which is what keeps a line of
    // thirty-eight characters from running wide of the rule under it.
    val textStyle = VisionType.sheetField(userFont).copy(letterSpacing = (-0.19).sp)

    Box(Modifier.padding(vertical = 12.dp)) {
        if (value.isEmpty() && !focused) {
            Text(text = placeholder, style = textStyle, color = PlanHint)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle.copy(color = OnCanvas),
            cursorBrush = SolidColor(OnCanvas),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
    }
    HorizontalDivider(color = Rule, thickness = 1.dp)
}
