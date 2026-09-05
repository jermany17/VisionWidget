package com.example.visionwidget.ui.vision

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.visionwidget.ui.ContentWidthFraction
import com.example.visionwidget.ui.components.CreateVisionRow
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.NavBar
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.OnNavBar
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType

private val IconSize = 96.dp
private val IconInnerSize = 78.dp

/** Angular size of one dash and of the gap after it, in degrees. */
private const val DashDegrees = 6f
private const val DashStepDegrees = 12f

private val RingSize = 92.dp
private val RingStroke = 9.dp

/** The ring's unfilled remainder — faint enough to read as a groove, not a second arc. */
private val RingTrack = OnCanvas.copy(alpha = 0.09f)

private val ChipShape = RoundedCornerShape(percent = 50)

/** The one spot of color in an otherwise monochrome app — reserved for what can't be undone. */
private val DestructiveFill = Color(0xFF7A3A3A)

/**
 * The Vision tab. With nothing set it is a single prompt; once visions exist it becomes
 * a switcher over them, showing the selected one in full underneath.
 *
 * The list and the selection are owned by the caller, because the Today tab reads the
 * same visions — this screen only reports what the user did to them.
 */
@Composable
fun VisionScreen(
    visions: List<Vision>,
    selectedVisionId: Long?,
    onSelectVision: (Long) -> Unit,
    onCreateVision: (goal: String, why: String, targetDateMillis: Long) -> Unit,
    onEditVision: (id: Long, goal: String, why: String, targetDateMillis: Long) -> Unit,
    onDeleteVision: (id: Long) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    userFontId: Int = UserFonts.DEFAULT_ID,
    modifier: Modifier = Modifier
) {
    val userFont = UserFonts[userFontId]
    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    var showLimitAlert by rememberSaveable { mutableStateOf(false) }
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // Falling back to the first vision covers both the first visit and a selection whose
    // vision is gone, so the screen always has something to show.
    val selected = visions.firstOrNull { it.id == selectedVisionId } ?: visions.firstOrNull()

    // A full set has to say so rather than opening a sheet that can't be submitted.
    val requestCreate = {
        if (visions.size >= MAX_VISIONS) showLimitAlert = true else showCreateSheet = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier
                .fillMaxWidth(ContentWidthFraction)
                .fillMaxHeight()
        ) {
            if (selected == null) {
                EmptyVision(
                    userFont = userFont,
                    contentPadding = contentPadding,
                    onCreate = { showCreateSheet = true }
                )
            } else {
                VisionDetail(
                    visions = visions,
                    selected = selected,
                    userFont = userFont,
                    contentPadding = contentPadding,
                    onSelectVision = onSelectVision,
                    onRequestCreate = requestCreate,
                    onRequestEdit = { showEditSheet = true },
                    onRequestDelete = { showDeleteDialog = true }
                )
            }
        }
    }

    if (showCreateSheet) {
        CreateVisionSheet(
            userFontId = userFontId,
            onDismiss = { showCreateSheet = false },
            onCreate = { goal, why, targetDateMillis ->
                onCreateVision(goal, why, targetDateMillis)
                showCreateSheet = false
            }
        )
    }

    if (showEditSheet && selected != null) {
        CreateVisionSheet(
            userFontId = userFontId,
            editing = selected,
            onDismiss = { showEditSheet = false },
            onCreate = { goal, why, targetDateMillis ->
                onEditVision(selected.id, goal, why, targetDateMillis)
                showEditSheet = false
            }
        )
    }

    if (showLimitAlert) {
        VisionLimitAlert(userFont = userFont, onDismiss = { showLimitAlert = false })
    }

    if (showDeleteDialog && selected != null) {
        DeleteVisionDialog(
            goal = selected.goal,
            userFont = userFont,
            onKeep = { showDeleteDialog = false },
            onDelete = {
                onDeleteVision(selected.id)
                showDeleteDialog = false
            }
        )
    }
}

/** The tab before any vision exists — one prompt and the row that starts the flow. */
@Composable
private fun ColumnScope.EmptyVision(
    userFont: UserFontChoice,
    contentPadding: PaddingValues,
    onCreate: () -> Unit
) {
    Spacer(Modifier.height(20.dp))
    Text(text = "VISION", style = VisionType.eyebrow, color = OnCanvas)

    // Equal weights above and below centre the prompt between the eyebrow
    // and the action row, rather than pinning it to a fixed offset.
    Spacer(Modifier.weight(1f))
    EmptyVisionMark()
    Spacer(Modifier.height(28.dp))
    Prompt(userFont)
    Spacer(Modifier.weight(1f))

    HorizontalDivider(color = Rule, thickness = 1.dp)
    Spacer(Modifier.height(18.dp))
    CreateVisionRow(onClick = onCreate)
    // The floating nav sits right under this row, so clear it by more than the
    // bar's own margin or the two read as one block.
    Spacer(Modifier.height(24.dp + contentPadding.calculateBottomPadding()))
}

/** The switcher and the selected vision in full. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VisionDetail(
    visions: List<Vision>,
    selected: Vision,
    userFont: UserFontChoice,
    contentPadding: PaddingValues,
    onSelectVision: (Long) -> Unit,
    onRequestCreate: () -> Unit,
    onRequestEdit: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "YOUR VISIONS · ${visions.size} / $MAX_VISIONS",
                style = VisionType.eyebrow,
                color = OnCanvas
            )
            Text(text = "TAP TO SWITCH", style = VisionType.eyebrow, color = OnCanvasMuted)
        }

        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visions.forEach { vision ->
                VisionChip(
                    vision = vision,
                    selected = vision.id == selected.id,
                    onClick = { onSelectVision(vision.id) }
                )
            }
            // Always offered: at the limit it explains itself rather than disappearing.
            NewVisionChip(onClick = onRequestCreate)
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = selected.goal,
            style = VisionType.screenPromptTitle(userFont),
            color = OnCanvas
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Rule, thickness = 1.dp)
        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(percent = selected.progressPercent, userFont = userFont)
            Spacer(Modifier.width(24.dp))
            Column {
                Text(text = "TARGET", style = VisionType.eyebrow, color = OnCanvasMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTargetDate(selected.targetDateMillis),
                    style = VisionType.bodyText(userFont),
                    color = OnCanvas
                )
                Spacer(Modifier.height(14.dp))
                Text(text = "REMAINING", style = VisionType.eyebrow, color = OnCanvasMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatRemaining(selected.targetDateMillis),
                    style = VisionType.bodyText(userFont),
                    color = OnCanvas
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Rule, thickness = 1.dp)
        Spacer(Modifier.height(24.dp))

        Text(text = "WHY", style = VisionType.eyebrow, color = OnCanvasMuted)
        Spacer(Modifier.height(10.dp))
        Text(
            // The reason is optional at creation, so it can be absent here.
            text = selected.why.ifBlank { "No reason set." },
            style = VisionType.screenPromptTitle(userFont),
            color = if (selected.why.isBlank()) OnCanvasMuted else OnCanvas
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Rule, thickness = 1.dp)
        Spacer(Modifier.height(20.dp))
        VisionActions(
            userFont = userFont,
            onEdit = onRequestEdit,
            onDelete = onRequestDelete
        )
        Spacer(Modifier.height(24.dp + contentPadding.calculateBottomPadding()))
    }
}

/**
 * The selected vision's two actions. Edit takes the row so it reads as the vision's own
 * button; Delete stays compact and outlined, sized to its label rather than sharing the
 * weight, so it can't be mistaken for the primary action.
 */
@Composable
private fun VisionActions(userFont: UserFontChoice, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(ChipShape)
                .background(NavBar)
                .clickable(onClick = onEdit)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Edit vision", style = VisionType.cardTitle(userFont), color = OnNavBar)
        }
        Box(
            modifier = Modifier
                .clip(ChipShape)
                .background(Canvas)
                .border(1.dp, Rule, ChipShape)
                .clickable(onClick = onDelete)
                .padding(horizontal = 28.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Delete", style = VisionType.cardTitle(userFont), color = OnCanvas)
        }
    }
}

/**
 * One vision in the switcher. The selected chip inverts to the nav bar's black so the
 * current vision reads at a glance; the rest carry the canvas hairline.
 */
@Composable
private fun VisionChip(vision: Vision, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .background(if (selected) NavBar else Canvas)
            .then(if (selected) Modifier else Modifier.border(1.dp, Rule, ChipShape))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = vision.goal,
            style = VisionType.navLabel,
            color = if (selected) OnNavBar else OnCanvas
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${vision.progressPercent}%",
            style = VisionType.eyebrow,
            color = if (selected) OnNavBar.copy(alpha = 0.7f) else OnCanvasMuted
        )
    }
}

/** The add control, dashed and held back so it reads as a slot rather than a vision. */
@Composable
private fun NewVisionChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(ChipShape)
            .clickable(onClick = onClick)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRoundRect(
                    color = OnCanvasMuted,
                    // Inset by half the stroke so the dashes stay inside the chip.
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(size.height / 2),
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                        )
                    )
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = "+ New", style = VisionType.navLabel, color = OnCanvasMuted)
    }
}

/**
 * Progress as a ring: a full faint track with the completed share drawn over it from
 * twelve o'clock, and the figure itself sitting in the hole.
 */
@Composable
private fun ProgressRing(percent: Int, userFont: UserFontChoice) {
    Box(
        modifier = Modifier
            .size(RingSize)
            .drawBehind {
                val stroke = RingStroke.toPx()
                // The arc is centred on the stroke, so pull the radius in by half of it
                // to keep the painted band inside the box.
                val radius = (size.minDimension - stroke) / 2
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)

                drawArc(
                    color = RingTrack,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = OnCanvas,
                    // Degrees run clockwise from three o'clock, so start a quarter back.
                    startAngle = -90f,
                    sweepAngle = percent * 3.6f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$percent%", style = VisionType.ringValue(userFont), color = OnCanvas)
    }
}

/** Shown when the add control is used with all three slots taken. */
@Composable
private fun VisionLimitAlert(userFont: UserFontChoice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Canvas,
        title = {
            Text(text = "THREE IS THE LIMIT", style = SheetLabel, color = OnCanvas)
        },
        text = {
            Text(
                text = "You've already created three visions. " +
                    "Remove one before adding another.",
                style = VisionType.bodyText(userFont),
                color = OnCanvasMuted
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "OK", style = SheetLabel, color = OnCanvas)
            }
        }
    )
}

/**
 * Confirms before a vision is removed. Keeping it is the plain, outlined choice; deleting
 * gets the one splash of color the app allows itself, so it can't be mistaken for routine.
 */
@Composable
private fun DeleteVisionDialog(
    goal: String,
    userFont: UserFontChoice,
    onKeep: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onKeep, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Canvas)
                .padding(24.dp)
        ) {
            Text(text = "DELETE VISION", style = SheetLabel, color = OnCanvasMuted)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Delete “$goal”?",
                style = VisionType.screenPromptTitle(userFont),
                color = OnCanvas
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Its four milestones go with it. Your Top 3 and everything in " +
                    "Insights stay exactly as they are.",
                style = VisionType.bodyText(userFont),
                color = OnCanvasMuted
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ChipShape)
                        .background(Canvas)
                        .border(1.dp, Rule, ChipShape)
                        .clickable(onClick = onKeep)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "KEEP IT", style = SheetLabel, color = OnCanvas)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ChipShape)
                        .background(DestructiveFill)
                        .clickable(onClick = onDelete)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "DELETE VISION", style = SheetLabel, color = OnNavBar)
                }
            }
        }
    }
}

@Composable
private fun Prompt(userFont: UserFontChoice) {
    Text(
        text = "What's your vision?",
        style = VisionType.screenPromptTitle(userFont),
        color = OnCanvas
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "The future starts with one goal.",
        style = VisionType.bodyText(userFont),
        color = OnCanvasMuted
    )
}

/**
 * Dashed ring with a small hollow centre — the "nothing here yet" mark.
 *
 * Drawn as evenly spaced arcs rather than a stroked circle with a dash effect, because
 * the dashes have to stay a fixed angle apart; a length-based dash pattern would drift.
 */
@Composable
private fun EmptyVisionMark() {
    val dash = OnCanvas.copy(alpha = 0.16f)
    val centreMark = OnCanvas.copy(alpha = 0.28f)

    Spacer(
        Modifier
            .size(IconSize)
            .drawBehind {
                val outerRadius = IconSize.toPx() / 2
                val innerRadius = IconInnerSize.toPx() / 2
                val ringWidth = outerRadius - innerRadius
                val ringRadius = (outerRadius + innerRadius) / 2

                var angle = 0f
                while (angle < 360f) {
                    drawArc(
                        color = dash,
                        startAngle = angle,
                        sweepAngle = DashDegrees,
                        useCenter = false,
                        topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                        size = Size(ringRadius * 2, ringRadius * 2),
                        style = Stroke(width = ringWidth)
                    )
                    angle += DashStepDegrees
                }

                drawCircle(
                    color = centreMark,
                    radius = 6.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
    )
}
