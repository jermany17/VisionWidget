package com.example.visionwidget.ui.vision

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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.sp
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The sheet's DM Mono labels, a step up from the app's 10sp eyebrow so they hold their
 * own against the serif fields they sit beside. Local to this sheet on purpose.
 */
internal val SheetLabel = VisionType.eyebrow.copy(fontSize = 12.sp, lineHeight = 16.sp)

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                style = VisionType.screenPromptTitle(userFont),
                color = OnCanvas
            )

            Spacer(Modifier.height(28.dp))
            SheetField(
                label = "THE GOAL",
                value = goal,
                placeholder = "Name it in five words or fewer",
                textStyle = VisionType.cardTitle(userFont),
                onValueChange = { goal = it }
            )

            Spacer(Modifier.height(24.dp))
            SheetField(
                label = "WHY IT MATTERS · OPTIONAL",
                value = why,
                placeholder = "Why does this matter to you?",
                textStyle = VisionType.cardTitle(userFont),
                onValueChange = { why = it }
            )

            Spacer(Modifier.height(24.dp))
            TargetDateRow(
                date = targetDateLabel,
                userFont = userFont,
                onPick = {
                    datePickerGeneration++
                    showDatePicker = true
                }
            )

            Spacer(Modifier.height(28.dp))
            PrimaryAction(
                ready = ready,
                readyLabel = if (editing != null) "SAVE CHANGES" else "CREATE VISION",
                // Only reachable once both are set, so the date can't be absent here.
                onClick = {
                    targetDateMillis?.let { onCreate(goal.trim(), why.trim(), it) }
                }
            )

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

    Text(text = label, style = SheetLabel, color = OnCanvasMuted)
    Spacer(Modifier.height(12.dp))
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
    Text(text = "TARGET DATE", style = SheetLabel, color = OnCanvasMuted)
    Spacer(Modifier.height(12.dp))
    Text(
        text = date.ifEmpty { "Pick a date" },
        style = VisionType.cardTitle(userFont),
        color = if (date.isEmpty()) SheetHint else OnCanvas,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
    )
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = Rule, thickness = 1.dp)
}

/**
 * The full-width pill. Black and live once the goal and date are in, reading
 * [readyLabel]; until then it wears the disabled fill and reads "NAME IT FIRST", and
 * taps do nothing.
 */
@Composable
private fun PrimaryAction(ready: Boolean, readyLabel: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(if (ready) NavBar else AvatarFill)
            .clickable(enabled = ready, onClick = onClick)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (ready) readyLabel else "NAME IT FIRST",
            style = SheetLabel,
            color = if (ready) OnNavBar else OnCanvasMuted
        )
    }
}
