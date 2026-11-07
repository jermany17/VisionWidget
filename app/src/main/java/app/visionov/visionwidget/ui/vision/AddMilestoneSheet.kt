package app.visionov.visionwidget.ui.vision

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import app.visionov.visionwidget.ui.theme.UserFonts
import app.visionov.visionwidget.ui.theme.VisionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** A milestone's date can't come before today or after the vision's own target date. */
@OptIn(ExperimentalMaterial3Api::class)
private class WithinVisionWindow(private val visionTargetMillis: Long) : SelectableDates {
    private fun Long.toUtcDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val day = utcTimeMillis.toUtcDate()
        return !day.isBefore(LocalDate.now()) && !day.isAfter(visionTargetMillis.toUtcDate())
    }

    override fun isSelectableYear(year: Int): Boolean =
        year in LocalDate.now().year..visionTargetMillis.toUtcDate().year
}

/**
 * A bottom sheet that adds one milestone to a vision: a short step and a date no
 * earlier than today and no later than the vision's own target. Milestones can't be
 * edited once added — only removed — so there's nothing here to pre-fill.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMilestoneSheet(
    visionTargetDateMillis: Long,
    onDismiss: () -> Unit,
    onAdd: (step: String, dueDateMillis: Long) -> Unit,
    modifier: Modifier = Modifier,
    userFontId: Int = UserFonts.DEFAULT_ID
) {
    val userFont = UserFonts[userFontId]

    var step by rememberSaveable { mutableStateOf("") }
    var dueDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val ready = step.isNotBlank() && dueDateMillis != null

    var confirmLeaving by rememberSaveable { mutableStateOf(false) }

    /**
     * Closing the sheet, or asking first.
     *
     * A milestone is only kept by the button at the foot of the sheet, so one that is
     * started but not added is lost by every other way out — the drag, the scrim and
     * Cancel alike.
     *
     * Asked on anything begun rather than on a milestone whole enough to add. The two
     * are not the same question: whether it can be added is what the button at the foot
     * answers, and this one only asks whether there is anything to lose. A step typed
     * without a date is still a step the user typed, and the answer offered is to come
     * back to it rather than to add it.
     *
     * The drag and the scrim are refused outright while there is something to lose, so
     * the sheet stays put with the question over it; this is what the ways out that
     * aren't gestures take.
     */
    val started = step.isNotBlank() || dueDateMillis != null
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
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(text = "ADD A MILESTONE", style = SheetLabel, color = OnCanvasMuted)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "What's the next step?",
                style = VisionType.sheetTitle(userFont),
                color = OnCanvas
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "One step, one date.",
                style = VisionType.sheetLead(userFont),
                color = OnCanvasMuted
            )

            // The two fields stand on the sheet itself rather than inside a panel of
            // their own. There are only two of them and the sheet holds nothing else, so
            // the box was drawing a boundary around the whole of the content — and it
            // put this sheet's fields on a different footing from the ones the vision is
            // written in, which are the same two questions asked the same way.
            Spacer(Modifier.height(22.dp))
            StepField(
                value = step,
                // Clamped rather than called out, the way the goal and the task fields
                // are: running out of room at the last character says itself.
                onValueChange = { value -> step = value.take(MAX_MILESTONE_STEP_LENGTH) },
                textStyle = VisionType.sheetRowField(userFont)
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Rule, thickness = 1.dp)

            Spacer(Modifier.height(18.dp))
            Text(text = "BY", style = FieldLabel, color = OnCanvasMuted)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { showDatePicker = true }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    // Written out in full. The date is read once, here, to be agreed
                    // with — and the short form the vision list uses is for a row that
                    // has a column's width to say it in.
                    text = dueDateMillis?.let(::formatTargetDate) ?: "Pick a date",
                    style = VisionType.sheetRowField(userFont),
                    color = if (dueDateMillis == null) SheetHint else OnCanvas,
                    modifier = Modifier.weight(1f)
                )
                // Says the line can be tapped. The date alone looks like something
                // already settled, and there is nothing else on it to suggest otherwise.
                Text(text = "CHANGE  →", style = FieldLabel, color = OnCanvasMuted)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Rule, thickness = 1.dp)

            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (ready) NavBar else AvatarFill)
                    .clickable(enabled = ready) {
                        dueDateMillis?.let { onAdd(step.trim(), it) }
                    }
                    .padding(vertical = 17.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add milestone",
                    style = VisionType.sheetButton(userFont),
                    color = if (ready) OnNavBar else OnCanvasMuted
                )
            }

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
            eyebrow = "MILESTONE",
            title = "Leave without saving?",
            // Says what has not happened and what leaving costs, and nothing about how
            // to save: the button at the foot of the sheet is the only thing that adds
            // a milestone, and a second way to do it from here would be a second thing
            // to keep right.
            body = "This milestone hasn't been added yet.\n" +
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

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateMillis,
            yearRange = LocalDate.now().year..Instant.ofEpochMilli(visionTargetDateMillis)
                .atZone(ZoneOffset.UTC).toLocalDate().year,
            selectableDates = remember { WithinVisionWindow(visionTargetDateMillis) }
        )
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
                        dueDateMillis = pickerState.selectedDateMillis
                        showDatePicker = false
                    },
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
 * The step-name field inside the "adding" box — the same hint-swap technique as the
 * create/edit vision sheet's fields, just placed on a tinted background.
 */
@Composable
private fun StepField(value: String, onValueChange: (String) -> Unit, textStyle: TextStyle) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box {
        if (value.isEmpty() && !focused) {
            Text(text = "Add a milestone", style = textStyle, color = SheetHint)
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
}
