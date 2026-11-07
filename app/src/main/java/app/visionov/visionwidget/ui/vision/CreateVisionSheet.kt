package app.visionov.visionwidget.ui.vision

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.visionov.visionwidget.ui.components.PromptDialog
import app.visionov.visionwidget.ui.components.rememberGuardedSheetState
import app.visionov.visionwidget.ui.theme.AvatarFill
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.NavBar
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.OnNavBar
import app.visionov.visionwidget.ui.theme.Rule
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.UserFonts
import app.visionov.visionwidget.ui.theme.VisionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The sheet's DM Mono labels. Named here because three sheets reach for them; the
 * figures themselves are [VisionType]'s, with the rest of the panel scale.
 */
internal val SheetLabel = VisionType.sheetLabel
internal val FieldLabel = VisionType.fieldLabel

/** Placeholder ink for the sheet's fields — fainter than [OnCanvasMuted] body text. */
internal val SheetHint = OnCanvas.copy(alpha = 0.3f)

/** A target date can't be in the past — today is the earliest the picker will take. */
@OptIn(ExperimentalMaterial3Api::class)
private object TodayOrLater : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val day = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return !day.isBefore(LocalDate.now())
    }

    override fun isSelectableYear(year: Int): Boolean = year >= LocalDate.now().year
}

/**
 * A bottom sheet that collects the goal, the reason it matters, and a target date — for
 * a new vision, or for [editing] an existing one, whose fields it opens pre-filled.
 *
 * The primary button stays on its disabled "NAME IT FIRST" label until the goal and a
 * date are both in, then flips to "CREATE VISION" (or "SAVE CHANGES" while editing).
 * [onCreate] hands back the trimmed text and the date as epoch millis — unformatted, so
 * the caller can recompute how long is left as the days pass; [onDismiss] fires on the
 * drag-to-close, the scrim, and CANCEL alike.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVisionSheet(
    onDismiss: () -> Unit,
    onCreate: (goal: String, why: String, targetDateMillis: Long) -> Unit,
    modifier: Modifier = Modifier,
    userFontId: Int = UserFonts.DEFAULT_ID,
    editing: Vision? = null
) {
    val userFont = UserFonts[userFontId]

    var goal by rememberSaveable { mutableStateOf(editing?.goal.orEmpty()) }
    var why by rememberSaveable { mutableStateOf(editing?.why.orEmpty()) }
    var targetDateMillis by rememberSaveable { mutableStateOf(editing?.targetDateMillis) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    // Bumped on every open so the picker is rebuilt seeded with the committed date,
    // rather than restoring whatever state it was left in last time.
    var datePickerGeneration by rememberSaveable { mutableIntStateOf(0) }

    val targetDateLabel = targetDateMillis?.let(::formatTargetDate).orEmpty()
    // The goal names the vision and the date bounds it; the reason is optional.
    val ready = goal.isNotBlank() && targetDateMillis != null

    // Whether anything has been written or changed since the sheet opened.
    //
    // Measured against what it was opened with, which is empty for a new vision and the
    // vision itself while one is being edited. That makes one rule serve both: starting
    // a vision and changing one are the same act of having something to lose, and
    // opening an existing vision only to close it again is not.
    //
    // Apart from [ready] on purpose. Whether it can be saved is what the button at the
    // foot of the sheet answers; this only asks whether there is anything here.
    val started = goal.trim() != editing?.goal.orEmpty().trim() ||
        why.trim() != editing?.why.orEmpty().trim() ||
        targetDateMillis != editing?.targetDateMillis

    var confirmLeaving by rememberSaveable { mutableStateOf(false) }

    /**
     * Closing the sheet, or asking first.
     *
     * A vision is only kept by the button at the foot of the sheet, so one begun and not
     * created — or changed and not saved — is lost by the drag, the scrim and Cancel
     * alike. The drag and the scrim are refused outright while there is something to
     * lose, so the sheet stays where it is with the question over it; this is what the
     * ways out that aren't gestures take.
     */
    val sheetState = rememberGuardedSheetState(
        hasUnsavedWork = { started },
        onAsk = { confirmLeaving = true }
    )
    val leaveOrAsk: () -> Unit = {
        if (started) confirmLeaving = true else onDismiss()
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
                text = if (editing != null) "EDIT VISION" else "NEW VISION",
                style = SheetLabel,
                color = OnCanvasMuted
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (editing != null) "Edit this vision" else "What are you actually after?",
                style = VisionType.sheetTitle(userFont),
                color = OnCanvas
            )

            Spacer(Modifier.height(22.dp))
            SheetField(
                label = "THE GOAL",
                value = goal,
                placeholder = "Name it in five words or fewer",
                textStyle = VisionType.sheetGoalField(userFont),
                // Clamped rather than called out, the way a task's own field is:
                // running out of room at the last character says itself.
                onValueChange = { value -> goal = value.take(MAX_VISION_GOAL_LENGTH) }
            )

            Spacer(Modifier.height(20.dp))
            SheetField(
                label = "WHY IT MATTERS · OPTIONAL",
                value = why,
                placeholder = "Why does this matter to you?",
                textStyle = VisionType.sheetWhyField(userFont),
                onValueChange = { value -> why = value.take(MAX_VISION_WHY_LENGTH) }
            )

            Spacer(Modifier.height(20.dp))
            TargetDateRow(
                date = targetDateLabel,
                userFont = userFont,
                onPick = {
                    datePickerGeneration++
                    showDatePicker = true
                }
            )

            Spacer(Modifier.height(24.dp))
            PrimaryAction(
                ready = ready,
                readyLabel = if (editing != null) "Save changes" else "Create vision",
                userFont = userFont,
                // Only reachable once both are set, so the date can't be absent here.
                onClick = {
                    targetDateMillis?.let { onCreate(goal.trim(), why.trim(), it) }
                }
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Cancel",
                style = VisionType.sheetCancel(userFont),
                color = OnCanvasMuted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = leaveOrAsk)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }

    if (confirmLeaving) {
        PromptDialog(
            eyebrow = if (editing != null) "EDIT VISION" else "NEW VISION",
            title = "Leave without saving?",
            body = if (editing != null) {
                "Your changes haven't been saved.\nIf you leave now, they'll be lost."
            } else {
                "This vision hasn't been created yet.\n" +
                    "If you leave now, your progress will be lost."
            },
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

    if (showDatePicker) {
        // Re-keyed per open so it seeds from the current pick, so "CHANGE" lands on
        // the chosen day and month.
        val pickerState = key(datePickerGeneration) {
            rememberDatePickerState(
                initialSelectedDateMillis = targetDateMillis,
                yearRange = LocalDate.now().year..DatePickerDefaults.YearRange.last,
                selectableDates = TodayOrLater
            )
        }
        val pickerColors = DatePickerDefaults.colors(
            containerColor = Canvas,
            selectedDayContainerColor = OnCanvas,
            selectedDayContentColor = Canvas,
            todayContentColor = OnCanvas,
            todayDateBorderColor = OnCanvas,
            selectedYearContainerColor = OnCanvas,
            selectedYearContentColor = Canvas
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        targetDateMillis = pickerState.selectedDateMillis
                        showDatePicker = false
                    },
                    // Backing out without a day left keeps the row on its placeholder.
                    enabled = pickerState.selectedDateMillis != null
                ) {
                    Text(text = "SET", style = SheetLabel, color = OnCanvas)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = "CANCEL", style = SheetLabel, color = OnCanvasMuted)
                }
            },
            colors = pickerColors
        ) {
            DatePicker(state = pickerState, showModeToggle = false, colors = pickerColors)
        }
    }
}

/**
 * A labelled line the user fills in. The placeholder sits in the same slot as the value
 * and in the same face, so committing text swaps tone without shifting the layout.
 */
@Composable
private fun SheetField(
    label: String,
    value: String,
    placeholder: String,
    textStyle: TextStyle,
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Text(text = label, style = FieldLabel, color = OnCanvasMuted)
    Spacer(Modifier.height(10.dp))
    Box {
        // The hint clears the moment the field is touched, not just once text exists.
        if (value.isEmpty() && !focused) {
            Text(text = placeholder, style = textStyle, color = SheetHint)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle.copy(color = OnCanvas),
            cursorBrush = SolidColor(OnCanvas),
            // Confirming drops focus so the caret stops blinking; the text is already
            // held in the sheet's state, so nothing else is needed to keep it.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = Rule, thickness = 1.dp)
}

/** The date line — a placeholder or the chosen date, tapped directly to open the picker. */
@Composable
private fun TargetDateRow(
    date: String,
    userFont: UserFontChoice,
    onPick: () -> Unit
) {
    Text(text = "TARGET DATE", style = FieldLabel, color = OnCanvasMuted)
    Spacer(Modifier.height(10.dp))
    Text(
        text = date.ifEmpty { "Pick a date" },
        style = VisionType.sheetDateField(userFont),
        color = if (date.isEmpty()) SheetHint else OnCanvas,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
    )
    Spacer(Modifier.height(10.dp))
    HorizontalDivider(color = Rule, thickness = 1.dp)
}

/**
 * The full-width pill. Black and live once the goal and date are in, reading
 * [readyLabel]; until then it wears the disabled fill and reads "Name it first", and
 * taps do nothing.
 */
@Composable
private fun PrimaryAction(
    ready: Boolean,
    readyLabel: String,
    userFont: UserFontChoice,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(if (ready) NavBar else AvatarFill)
            .clickable(enabled = ready, onClick = onClick)
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (ready) readyLabel else "Name it first",
            style = VisionType.sheetButton(userFont),
            color = if (ready) OnNavBar else OnCanvasMuted
        )
    }
}
