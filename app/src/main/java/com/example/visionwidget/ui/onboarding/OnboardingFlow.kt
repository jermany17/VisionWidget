package com.example.visionwidget.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionwidget.ui.ContentWidthFraction
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.NavBar
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.OnNavBar
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType
import com.example.visionwidget.ui.vision.formatTargetDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Total questions in the flow — the denominator on every step's counter and bar. */
const val ONBOARDING_STEPS = 5

/** Inline validation ink — a red held back enough to sit in the restrained palette. */
private val ErrorRed = Color(0xFFB3261E)

/** DM Mono chrome for the flow: the step counter, the nav words, buttons, chips. */
private val StepLabel = VisionType.eyebrow.copy(fontSize = 12.sp, lineHeight = 16.sp)

/** The goal presets on step 2. Picking one fills the goal field with its text. */
private val GoalPresets = listOf(
    "Launch my startup",
    "Run a marathon",
    "Harvard Law School",
    "Financial freedom"
)

/** The wisdom themes on step 5. The daily line is drawn from the one chosen. */
private val WisdomCategories = listOf(
    "Motivation",
    "Success",
    "Life",
    "Happiness",
    "Wisdom"
)

/** Everything the flow collects — the goal, its reason, a target date, a wisdom theme. */
data class OnboardingData(
    val goal: String = "",
    val why: String = "",
    val targetDateMillis: Long? = null,
    val wisdomCategory: String? = null
)

/**
 * The first-run flow: five short questions, each skippable, with a progress line and a
 * step counter across the top.
 *
 * [onSkip] drops everything entered and opens the home screen; [onComplete] hands back
 * the answers once the last step is done.
 */
@Composable
fun OnboardingFlow(
    onSkip: () -> Unit,
    onComplete: (OnboardingData) -> Unit,
    modifier: Modifier = Modifier,
    userFontId: Int = UserFonts.DEFAULT_ID
) {
    val userFont = UserFonts[userFontId]

    var step by rememberSaveable { mutableIntStateOf(1) }
    var goal by rememberSaveable { mutableStateOf("") }
    var goalError by rememberSaveable { mutableStateOf(false) }
    var why by rememberSaveable { mutableStateOf("") }
    var whyError by rememberSaveable { mutableStateOf(false) }
    var dateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateError by rememberSaveable { mutableStateOf(false) }
    var wisdomCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var wisdomError by rememberSaveable { mutableStateOf(false) }
    var showSkipConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        StepProgressBar(fraction = step / ONBOARDING_STEPS.toFloat())

        Column(
            Modifier
                .fillMaxWidth(ContentWidthFraction)
                .align(Alignment.CenterHorizontally)
                .weight(1f)
        ) {
            Spacer(Modifier.height(14.dp))
            StepHeader(
                step = step,
                canGoBack = step > 1,
                onBack = {
                    step--
                    goalError = false ; whyError = false ; dateError = false ; wisdomError = false
                },
                onSkip = { showSkipConfirm = true }
            )

            val finish = {
                onComplete(
                    OnboardingData(
                        goal = goal.trim(),
                        why = why.trim(),
                        targetDateMillis = dateMillis,
                        wisdomCategory = wisdomCategory?.lowercase()
                    )
                )
            }

            when (step) {
                1 -> IntroStep(userFont = userFont, onStart = { step = 2 })
                2 -> GoalStep(
                    userFont = userFont,
                    goal = goal,
                    error = goalError,
                    onGoalChange = { goal = it ; goalError = false },
                    onNext = {
                        if (goal.isBlank()) goalError = true else { goalError = false ; step = 3 }
                    }
                )
                3 -> WhyStep(
                    userFont = userFont,
                    why = why,
                    error = whyError,
                    onWhyChange = { why = it ; whyError = false },
                    onNext = {
                        if (why.isBlank()) whyError = true else { whyError = false ; step = 4 }
                    }
                )
                4 -> DateStep(
                    userFont = userFont,
                    dateMillis = dateMillis,
                    error = dateError,
                    onDateChange = { dateMillis = it ; dateError = false },
                    onNext = {
                        if (dateMillis == null) dateError = true else { dateError = false ; step = 5 }
                    }
                )
                5 -> WisdomStep(
                    userFont = userFont,
                    category = wisdomCategory,
                    error = wisdomError,
                    onCategoryChange = { wisdomCategory = it ; wisdomError = false },
                    // The last step — a chosen theme finishes the flow.
                    onDone = {
                        if (wisdomCategory == null) wisdomError = true else { wisdomError = false ; finish() }
                    }
                )
                else -> PlaceholderStep(
                    step = step,
                    isLast = step == ONBOARDING_STEPS,
                    onNext = { step++ },
                    onFinish = finish
                )
            }
        }
    }

    if (showSkipConfirm) {
        SkipConfirmDialog(
            onKeepGoing = { showSkipConfirm = false },
            onConfirmSkip = {
                // Nothing to wipe by hand — leaving this composable drops every
                // rememberSaveable it owns, which is all the flow has collected.
                showSkipConfirm = false
                onSkip()
            }
        )
    }
}

/** A hairline-thin bar filled from the left to the current step's share of five. */
@Composable
private fun StepProgressBar(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(OnCanvas.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(OnCanvas)
        )
    }
}

/**
 * Back on the left (hidden on step one), the counter centred, Skip on the right — three
 * equal slots so the counter stays centred whatever sits beside it.
 */
@Composable
private fun StepHeader(
    step: Int,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (canGoBack) {
                Text(
                    text = "BACK",
                    style = StepLabel,
                    color = OnCanvasMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = "STEP $step OF $ONBOARDING_STEPS",
                style = StepLabel,
                color = OnCanvasMuted,
                textAlign = TextAlign.Center
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = "SKIP",
                style = StepLabel,
                color = OnCanvas,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .border(1.dp, Rule, RoundedCornerShape(percent = 50))
                    .clickable(onClick = onSkip)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/** Step 1 — what the app is, and the button that starts the questions. */
@Composable
private fun ColumnScope.IntroStep(userFont: UserFontChoice, onStart: () -> Unit) {
    Spacer(Modifier.weight(1.1f))
    Text(text = "VISION WIDGET", style = VisionType.eyebrow, color = OnCanvasMuted)
    Spacer(Modifier.height(16.dp))
    Text(
        text = "One goal. Three things a day. On your home screen.",
        style = VisionType.screenPromptTitle(userFont),
        color = OnCanvas
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Five short questions. Skip any of them — nothing here is required, and nothing is lost if you leave early.",
        style = VisionType.bodyText(userFont),
        color = OnCanvasMuted
    )
    Spacer(Modifier.weight(1f))
    StepPrimaryButton(label = "START", onClick = onStart)
    Spacer(Modifier.height(24.dp))
}

/** Step 2 — the goal, either typed or tapped from a preset that fills the same field. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.GoalStep(
    userFont: UserFontChoice,
    goal: String,
    error: Boolean,
    onGoalChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Spacer(Modifier.height(28.dp))
        Text(text = "THE GOAL", style = VisionType.eyebrow, color = OnCanvasMuted)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "What do you want most?",
            style = VisionType.screenPromptTitle(userFont),
            color = OnCanvas
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Five words or fewer. You can change it any time.",
            style = VisionType.bodyText(userFont),
            color = OnCanvasMuted
        )

        Spacer(Modifier.height(28.dp))
        OnboardingField(
            value = goal,
            placeholder = "Launch my startup",
            error = error,
            errorMessage = "Enter a goal to continue.",
            userFont = userFont,
            onValueChange = onGoalChange
        )

        Spacer(Modifier.height(20.dp))
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoalPresets.forEach { preset ->
                OnboardingChip(
                    label = preset,
                    selected = goal.trim().equals(preset, ignoreCase = true),
                    userFont = userFont,
                    modifier = Modifier.weight(1f),
                    onClick = { onGoalChange(preset) }
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        StepPrimaryButton(label = "NEXT", onClick = onNext)
        Spacer(Modifier.height(24.dp))
    }
}

/** Step 3 — the reason behind the goal, the line the widget shows on a bad day. */
@Composable
private fun ColumnScope.WhyStep(
    userFont: UserFontChoice,
    why: String,
    error: Boolean,
    onWhyChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Spacer(Modifier.height(28.dp))
        Text(text = "WHY IT MATTERS", style = VisionType.eyebrow, color = OnCanvasMuted)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Why does it matter?",
            style = VisionType.screenPromptTitle(userFont),
            color = OnCanvas
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "The sentence you would say at 11pm. This is what the widget " +
                "shows you on a bad day.",
            style = VisionType.bodyText(userFont),
            color = OnCanvasMuted
        )

        Spacer(Modifier.height(28.dp))
        OnboardingField(
            value = why,
            placeholder = "Because nobody else is going to build it.",
            error = error,
            errorMessage = "Say why this goal matters to you.",
            userFont = userFont,
            onValueChange = onWhyChange
        )

        Spacer(Modifier.height(28.dp))
        StepPrimaryButton(label = "NEXT", onClick = onNext)
        Spacer(Modifier.height(24.dp))
    }
}

/** Step 4 — the date to aim for, chosen from the same picker the vision sheet uses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.DateStep(
    userFont: UserFontChoice,
    dateMillis: Long?,
    error: Boolean,
    onDateChange: (Long) -> Unit,
    onNext: () -> Unit
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    // Bumped on every open so the picker seeds from the committed date, not whatever
    // state it was left in last time.
    var pickerGeneration by rememberSaveable { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(28.dp))
        Text(text = "THE DATE", style = VisionType.eyebrow, color = OnCanvasMuted)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "When do you want it by?",
            style = VisionType.screenPromptTitle(userFont),
            color = OnCanvas
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Pick a day to aim for. You can move it later.",
            style = VisionType.bodyText(userFont),
            color = OnCanvasMuted
        )

        Spacer(Modifier.height(28.dp))
        val label = dateMillis?.let(::formatTargetDate)
        Text(
            text = label ?: "Pick a date",
            style = VisionType.cardTitle(userFont),
            color = if (label == null) OnCanvas.copy(alpha = 0.3f) else OnCanvas,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    pickerGeneration++
                    showPicker = true
                }
        )
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(
            color = if (error) ErrorRed else Rule,
            thickness = if (error) 2.dp else 1.dp
        )
        if (error) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Pick a date to continue.",
                style = VisionType.bodyText(userFont),
                color = ErrorRed
            )
        }

        Spacer(Modifier.height(28.dp))
        StepPrimaryButton(label = "NEXT", onClick = onNext)
        Spacer(Modifier.height(24.dp))
    }

    if (showPicker) {
        val pickerState = key(pickerGeneration) {
            rememberDatePickerState(
                initialSelectedDateMillis = dateMillis,
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
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onDateChange)
                        showPicker = false
                    },
                    enabled = pickerState.selectedDateMillis != null
                ) {
                    Text(text = "SET", style = StepLabel, color = OnCanvas)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(text = "CANCEL", style = StepLabel, color = OnCanvasMuted)
                }
            },
            colors = pickerColors
        ) {
            DatePicker(state = pickerState, showModeToggle = false, colors = pickerColors)
        }
    }
}

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
 * A serif input line with a hairline under it that turns red, plus a red note, when the
 * step is left empty. Shared by the goal and reason steps.
 */
@Composable
private fun OnboardingField(
    value: String,
    placeholder: String,
    error: Boolean,
    errorMessage: String,
    userFont: UserFontChoice,
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val style = VisionType.cardTitle(userFont)

    Box {
        // The hint clears the moment the field is touched, not just once text exists.
        if (value.isEmpty() && !focused) {
            Text(
                text = placeholder,
                style = style,
                color = OnCanvas.copy(alpha = 0.3f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = style.copy(color = OnCanvas),
            cursorBrush = SolidColor(OnCanvas),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
        )
    }
    Spacer(Modifier.height(10.dp))
    HorizontalDivider(
        color = if (error) ErrorRed else Rule,
        thickness = if (error) 2.dp else 1.dp
    )
    if (error) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = VisionType.bodyText(userFont),
            color = ErrorRed
        )
    }
}

/**
 * A pickable pill — a goal preset on step 2, a wisdom theme on step 5. Selected inverts
 * to black; set in the user's face, mixed case. Pass a [modifier] to size it (weights on
 * step 2 hold two to a row; step 5 lets them flow at their natural width).
 */
@Composable
private fun OnboardingChip(
    label: String,
    selected: Boolean,
    userFont: UserFontChoice,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) OnCanvas else Canvas)
            .then(if (selected) Modifier else Modifier.border(1.dp, Rule, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = VisionType.bodyText(userFont),
            color = if (selected) OnNavBar else OnCanvas,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/** Step 5 — the wisdom theme the daily line is drawn from, and the flow's last step. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.WisdomStep(
    userFont: UserFontChoice,
    category: String?,
    error: Boolean,
    onCategoryChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(28.dp))
        Text(text = "DAILY WISDOM", style = VisionType.eyebrow, color = OnCanvasMuted)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Which wisdom should we send you?",
            style = VisionType.screenPromptTitle(userFont),
            color = OnCanvas
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "One short line lands on your home screen every morning. Pick the " +
                "theme it's drawn from — you can change it later.",
            style = VisionType.bodyText(userFont),
            color = OnCanvasMuted
        )

        Spacer(Modifier.height(24.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WisdomCategories.forEach { theme ->
                OnboardingChip(
                    label = theme,
                    selected = theme.equals(category, ignoreCase = true),
                    userFont = userFont,
                    onClick = { onCategoryChange(theme) }
                )
            }
        }
        if (error) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Pick a theme to continue.",
                style = VisionType.bodyText(userFont),
                color = ErrorRed
            )
        }

        Spacer(Modifier.height(28.dp))
        StepPrimaryButton(label = "DONE", onClick = onDone)
        Spacer(Modifier.height(24.dp))
    }
}

/** A fallback for an out-of-range step — keeps Back, Skip and the counter working. */
@Composable
private fun ColumnScope.PlaceholderStep(
    step: Int,
    isLast: Boolean,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Spacer(Modifier.weight(1f))
    Text(
        text = "STEP $step",
        style = VisionType.eyebrow,
        color = OnCanvasMuted,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "NOT BUILT YET",
        style = StepLabel,
        color = OnCanvas,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
    Spacer(Modifier.weight(1f))
    StepPrimaryButton(
        label = if (isLast) "FINISH" else "NEXT",
        onClick = if (isLast) onFinish else onNext
    )
    Spacer(Modifier.height(24.dp))
}

/** The full-width black pill every step ends on. */
@Composable
private fun StepPrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(NavBar)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = StepLabel, color = OnNavBar)
    }
}

/** Confirms the skip, since it can't be undone — everything entered goes with it. */
@Composable
private fun SkipConfirmDialog(onKeepGoing: () -> Unit, onConfirmSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepGoing,
        containerColor = Canvas,
        title = { Text(text = "SKIP SETUP?", style = StepLabel, color = OnCanvas) },
        text = {
            Text(
                text = "SKIPPING CLEARS EVERYTHING YOU'VE ENTERED AND TAKES YOU " +
                    "STRAIGHT TO THE HOME SCREEN.",
                style = VisionType.eyebrow.copy(
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.6.sp
                ),
                color = OnCanvasMuted
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmSkip) {
                Text(text = "SKIP", style = StepLabel, color = OnCanvas)
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepGoing) {
                Text(text = "KEEP GOING", style = StepLabel, color = OnCanvasMuted)
            }
        }
    )
}
