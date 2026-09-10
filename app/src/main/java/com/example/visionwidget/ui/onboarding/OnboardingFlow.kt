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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionwidget.ui.ContentWidthFraction
import com.example.visionwidget.ui.theme.NavBar
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.OnNavBar
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType

/** Total questions in the flow — the denominator on every step's counter and bar. */
const val ONBOARDING_STEPS = 9

/** Onboarding paints on warm paper rather than the app's plain white canvas. */
private val Paper = Color(0xFFF4F1E8)

/** Inline validation ink — a red that still sits inside the paper palette. */
private val ErrorRed = Color(0xFFB3261E)

/** DM Mono chrome for the flow: the step counter, the nav words, buttons, chips. */
private val StepLabel = VisionType.eyebrow.copy(fontSize = 12.sp, lineHeight = 16.sp)

/** The goal presets on step 2. Shown uppercased on the chips; written as-is to the field. */
private val GoalPresets = listOf(
    "Launch my startup",
    "Run a marathon",
    "Harvard Law School",
    "Financial freedom"
)

/** Everything the flow collects. Grows a field per step; only the goal so far. */
data class OnboardingData(
    val goal: String = ""
)

/**
 * The first-run flow: nine short questions, each skippable, with a progress line and a
 * step counter across the top. Only the first two steps are built out; the rest are
 * navigable placeholders until they're filled in.
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
    var showSkipConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
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
                onBack = { step-- ; goalError = false },
                onSkip = { showSkipConfirm = true }
            )

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
                else -> PlaceholderStep(
                    step = step,
                    isLast = step == ONBOARDING_STEPS,
                    onNext = { step++ },
                    onFinish = { onComplete(OnboardingData(goal = goal.trim())) }
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

/** A hairline-thin bar filled from the left to the current step's share of nine. */
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
        text = "One goal. Three things a\nday. On your home screen.",
        style = VisionType.screenPromptTitle(userFont),
        color = OnCanvas
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = "Nine short questions. Skip any of them — nothing here is " +
            "required, and nothing is lost if you leave early.",
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
        GoalField(value = goal, error = error, userFont = userFont, onValueChange = onGoalChange)

        Spacer(Modifier.height(20.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoalPresets.forEach { preset ->
                GoalPresetChip(
                    label = preset,
                    selected = goal.trim().equals(preset, ignoreCase = true),
                    onClick = { onGoalChange(preset) }
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        StepPrimaryButton(label = "NEXT", onClick = onNext)
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * The goal line: a serif field with a hairline under it that turns red, plus a red note,
 * when the step is left empty.
 */
@Composable
private fun GoalField(
    value: String,
    error: Boolean,
    userFont: UserFontChoice,
    onValueChange: (String) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val style = VisionType.cardTitle(userFont)

    Box {
        // The hint clears the moment the field is touched, not just once text exists.
        if (value.isEmpty() && !focused) {
            Text(text = "Launch my startup", style = style, color = OnCanvas.copy(alpha = 0.3f))
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
            text = "Enter a goal to continue.",
            style = VisionType.bodyText(userFont),
            color = ErrorRed
        )
    }
}

/** One preset. Selected once its text matches the field; picking it fills the field. */
@Composable
private fun GoalPresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) OnCanvas else Paper)
            .then(if (selected) Modifier else Modifier.border(1.dp, Rule, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = StepLabel,
            color = if (selected) OnNavBar else OnCanvas
        )
    }
}

/** Steps 3–9 until they're built — keeps Back, Skip and the counter working. */
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
        containerColor = Paper,
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
