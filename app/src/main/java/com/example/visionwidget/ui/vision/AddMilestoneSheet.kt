package com.example.visionwidget.ui.vision

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.visionwidget.ui.theme.AvatarFill
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.NavBar
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.OnNavBar
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var step by rememberSaveable { mutableStateOf("") }
    var dueDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val ready = step.isNotBlank() && dueDateMillis != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                style = VisionType.screenPromptTitle(userFont),
                color = OnCanvas
            )

            Spacer(Modifier.height(28.dp))
            Text(text = "ADDING", style = SheetLabel, color = OnCanvasMuted)
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AvatarFill)
                    .padding(16.dp)
            ) {
                Text(text = "STEP", style = SheetLabel, color = OnCanvasMuted)
                Spacer(Modifier.height(10.dp))
                StepField(
                    value = step,
                    onValueChange = { step = it },
                    textStyle = VisionType.cardTitle(userFont)
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Rule, thickness = 1.dp)
                Spacer(Modifier.height(16.dp))
                Text(text = "BY", style = SheetLabel, color = OnCanvasMuted)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dueDateMillis?.let(::formatShortDate) ?: "Pick a date",
                        style = VisionType.cardTitle(userFont),
                        color = if (dueDateMillis == null) SheetHint else OnCanvas,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = { showDatePicker = true }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (dueDateMillis == null) "PICK" else "CHANGE",
                            style = SheetLabel,
                            color = OnCanvasMuted,
                            modifier = Modifier.alignByBaseline()
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "→",
                            style = VisionType.glyph,
                            color = OnCanvasMuted,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (ready) NavBar else AvatarFill)
                    .clickable(enabled = ready) {
                        dueDateMillis?.let { onAdd(step.trim(), it) }
                    }
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ADD TO THIS VISION",
                    style = SheetLabel,
                    color = if (ready) OnNavBar else OnCanvasMuted
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = "CANCEL",
                style = SheetLabel,
                color = OnCanvasMuted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
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
            Text(text = "Add content", style = textStyle, color = SheetHint)
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
