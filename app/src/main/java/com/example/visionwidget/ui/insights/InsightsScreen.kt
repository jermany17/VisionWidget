package com.example.visionwidget.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import java.time.format.TextStyle
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import com.example.visionwidget.data.DayRecord
import com.example.visionwidget.data.TopThreeStats
import com.example.visionwidget.data.longestStreak
import com.example.visionwidget.data.topThreeStats
import com.example.visionwidget.ui.ContentWidthFraction
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.CardThemes
import com.example.visionwidget.ui.theme.NavBar
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.OnNavBar
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** The fixed line under the mode label — the screen says what it holds, not how it went. */
private const val RECORD_BLURB = "This is TOP 3 record"

private val CardShape = RoundedCornerShape(16.dp)
private val PillShape = RoundedCornerShape(percent = 50)
private val TooltipShape = RoundedCornerShape(8.dp)

/** The track the Week/Month switch sits in, a shade off the white canvas. */
private val SwitchTrack = Color(0xFFF1F1F1)

/** How many weeks the consistency grid looks back over, today's week included. */
private const val CONSISTENCY_WEEKS = 5

/**
 * The grid's ramp, lightest to darkest. A day with no record sits below a day that was
 * set and left undone — nothing attempted reads differently from nothing finished.
 */
private val CellNoRecord = Color(0xFFF2F2F2)
private val CellNone = Color(0xFFDEDEDE)
private val CellLow = Color(0xFFB2B2B2)
private val CellHalf = Color(0xFF8A8A8A)
private val CellHigh = Color(0xFF565656)
private val CellFull = Color(0xFF111111)

/** Sunday first, so the week reads the way the calendar app beside it does. */
private val WeekColumns = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY
)

/** Placeholder until the reflection is generated from the record itself. */
private const val REFLECTION_TEXT =
    "Work you started before 9am finished 84% of the time. " +
        "Everything you moved past noon finished 41%."
private const val REFLECTION_NOTE = "No advice. Just what happened."

/** The warm light surface from the shared palette, so the card isn't a one-off colour. */
private const val REFLECTION_THEME_ID = 3

private val DayFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
private val RangeDayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val MonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/** Whether the log below shows a week at a time or a whole month. */
enum class InsightsPeriod(val label: String) {
    Week("Week"),
    Month("Month")
}

/** Which days of the chosen period the log lists. */
private enum class DayFilter(val label: String) {
    All("ALL DAYS"),
    Completed("COMPLETED"),
    Undone("LEFT UNDONE")
}

/** The span a period/offset pair covers, both ends inclusive. */
private data class DateRange(val start: LocalDate, val end: LocalDate)

/** One square of the consistency grid, carrying the date so a tap can name it. */
private data class GridCell(val date: LocalDate, val record: DayRecord?)

private fun rangeFor(period: InsightsPeriod, offset: Int, today: LocalDate): DateRange =
    when (period) {
        InsightsPeriod.Week -> {
            // Weeks run Monday to Sunday, so stepping back always lands on a whole week
            // rather than a rolling seven days that drifts with the day it was opened.
            val start = today
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusWeeks(offset.toLong())
            DateRange(start, start.plusDays(6))
        }

        InsightsPeriod.Month -> {
            val start = today.withDayOfMonth(1).plusMonths(offset.toLong())
            DateRange(start, start.with(TemporalAdjusters.lastDayOfMonth()))
        }
    }

private fun DateRange.label(period: InsightsPeriod): String = when (period) {
    InsightsPeriod.Week -> "${start.format(RangeDayFormat)} — ${end.format(RangeDayFormat)}"
    InsightsPeriod.Month -> start.format(MonthFormat)
}

/**
 * The Top 3 log: all-time figures at the top, then one week or month of days at a time.
 *
 * The figures deliberately ignore the period being browsed — they're the user's whole
 * record, so stepping back through weeks reads the past without rewriting the totals.
 */
@Composable
fun InsightsScreen(
    records: List<DayRecord> = emptyList(),
    onToggleTask: (epochDay: Long, slotIndex: Int) -> Unit = { _, _ -> },
    contentPadding: PaddingValues = PaddingValues(),
    userFontId: Int = UserFonts.DEFAULT_ID,
    modifier: Modifier = Modifier
) {
    val userFont = UserFonts[userFontId]
    var period by rememberSaveable { mutableStateOf(InsightsPeriod.Week) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf(DayFilter.All) }

    // Read once per composition so a midnight crossing can't have the range and the
    // streak disagreeing about which day is today.
    val today = remember(records) { LocalDate.now() }
    val stats = remember(records, today) { topThreeStats(records, today.toEpochDay()) }

    val longest = remember(records) { longestStreak(records) }

    // Newest week on top, so the grid reads downward into the past like the log above it.
    val weeks = remember(records, today) {
        val byDay = records.associateBy { it.epochDay }
        val thisSunday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        (0 until CONSISTENCY_WEEKS).map { week ->
            val start = thisSunday.minusWeeks(week.toLong())
            (0..6).map { column ->
                val date = start.plusDays(column.toLong())
                GridCell(date, byDay[date.toEpochDay()])
            }
        }
    }

    val range = rangeFor(period, offset, today)
    val inRange = records.filter {
        it.epochDay >= range.start.toEpochDay() && it.epochDay <= range.end.toEpochDay()
    }
    val shown = when (filter) {
        DayFilter.All -> inRange
        DayFilter.Completed -> inRange.filter { it.isComplete }
        DayFilter.Undone -> inRange.filterNot { it.isComplete }
    }

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
            Text(
                text = period.label.uppercase(),
                style = VisionType.eyebrow,
                color = OnCanvas
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = RECORD_BLURB,
                style = VisionType.greeting(userFont),
                color = OnCanvas
            )

            Spacer(Modifier.height(22.dp))
            StatsRow(stats = stats, userFont = userFont)

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (period) {
                        InsightsPeriod.Week -> "THIS WEEK'S TOP 3"
                        InsightsPeriod.Month -> "THIS MONTH'S TOP 3"
                    },
                    style = VisionType.eyebrow,
                    color = OnCanvas
                )
                Text(
                    text = "${stats.daysRecorded} DAYS KEPT",
                    style = VisionType.eyebrow,
                    color = OnCanvasMuted
                )
            }

            Spacer(Modifier.height(14.dp))
            PeriodSwitch(
                selected = period,
                onSelect = {
                    // Week three of a month has no meaning as a month, so switching
                    // modes returns to the current period rather than keeping the step.
                    period = it
                    offset = 0
                }
            )

            Spacer(Modifier.height(18.dp))
            RangeStepper(
                label = range.label(period),
                userFont = userFont,
                onPrevious = { offset-- },
                onNext = { offset++ }
            )

            Spacer(Modifier.height(16.dp))
            FilterRow(selected = filter, onSelect = { filter = it })

            Spacer(Modifier.height(18.dp))
            if (shown.isEmpty()) {
                EmptyLog(filter = filter, userFont = userFont)
            } else {
                shown.forEachIndexed { index, record ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    DayCard(
                        record = record,
                        userFont = userFont,
                        onToggleTask = { slotIndex -> onToggleTask(record.epochDay, slotIndex) }
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
            Text(
                text = "CONSISTENCY · LAST $CONSISTENCY_WEEKS WEEKS",
                style = VisionType.eyebrow,
                color = OnCanvas
            )
            Spacer(Modifier.height(14.dp))
            ConsistencyGrid(weeks = weeks)

            Spacer(Modifier.height(30.dp))
            Text(text = "PATTERNS", style = VisionType.eyebrow, color = OnCanvas)
            Spacer(Modifier.height(6.dp))
            PatternRow(
                label = "Most consistent day",
                value = mostConsistentDay(weeks),
                userFont = userFont
            )
            PatternRow(
                label = "Longest streak",
                value = longest.let { if (it == 1) "1 day" else "$it days" },
                userFont = userFont
            )

            Spacer(Modifier.height(30.dp))
            ReflectionCard(userFont = userFont)

            Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
        }
    }
}

/**
 * Streak, completed days and the rate between them. Rate is the one that's filled rather
 * than outlined — it's the figure the other two are building toward.
 */
@Composable
private fun StatsRow(stats: TopThreeStats, userFont: UserFontChoice) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "STREAK",
            value = stats.streak.toString(),
            userFont = userFont,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "COMPLETED",
            value = stats.completed.toString(),
            userFont = userFont,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "RATE",
            value = "${stats.ratePercent}%",
            userFont = userFont,
            filled = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    userFont: UserFontChoice,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    val ink = if (filled) OnNavBar else OnCanvas
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(if (filled) NavBar else Canvas)
            .then(if (filled) Modifier else Modifier.border(1.dp, Rule, CardShape))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = VisionType.eyebrow,
            color = if (filled) ink.copy(alpha = 0.7f) else OnCanvasMuted
        )
        Spacer(Modifier.height(8.dp))
        Text(text = value, style = VisionType.cardTitle(userFont), color = ink)
    }
}

/** Two halves of one track — the selected side lifts to white, the other stays flush. */
@Composable
private fun PeriodSwitch(selected: InsightsPeriod, onSelect: (InsightsPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(SwitchTrack)
            .padding(4.dp)
    ) {
        InsightsPeriod.entries.forEach { entry ->
            val isSelected = entry == selected
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (isSelected) Canvas else Color.Transparent)
                    .clickable { onSelect(entry) }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = entry.label,
                    style = VisionType.navLabel,
                    color = if (isSelected) OnCanvas else OnCanvasMuted
                )
            }
        }
    }
}

/** The arrows sit at the edges with the range centred, so the label never shifts. */
@Composable
private fun RangeStepper(
    label: String,
    userFont: UserFontChoice,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepArrow(glyph = "←", onClick = onPrevious)
        Text(
            text = label,
            style = VisionType.metricValue(userFont),
            color = OnCanvas,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        StepArrow(glyph = "→", onClick = onNext)
    }
}

@Composable
private fun StepArrow(glyph: String, onClick: () -> Unit) {
    Text(
        text = glyph,
        style = VisionType.glyph,
        color = OnCanvasMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun FilterRow(selected: DayFilter, onSelect: (DayFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DayFilter.entries.forEach { entry ->
            val isSelected = entry == selected
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (isSelected) NavBar else Canvas)
                    .then(if (isSelected) Modifier else Modifier.border(1.dp, Rule, PillShape))
                    .clickable { onSelect(entry) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = entry.label,
                    style = VisionType.eyebrow,
                    color = if (isSelected) OnNavBar else OnCanvas
                )
            }
        }
    }
}

/**
 * One day's slots, grouped so each day reads as its own block. Every task stays tappable
 * however old it is — the point of keeping the record is being able to settle it later.
 */
@Composable
private fun DayCard(
    record: DayRecord,
    userFont: UserFontChoice,
    onToggleTask: (slotIndex: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Canvas)
            .border(1.dp, Rule, CardShape)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = LocalDate.ofEpochDay(record.epochDay).format(DayFormat),
                style = VisionType.metricValue(userFont),
                color = OnCanvas
            )
            Text(
                // Against the tasks that were set, not a fixed three — one task ticked
                // off reads as a finished day, not a third of one.
                text = "${record.doneCount} / ${record.total}",
                style = VisionType.eyebrow,
                color = OnCanvasMuted
            )
        }

        Spacer(Modifier.height(16.dp))
        record.entries.forEachIndexed { index, entry ->
            if (index > 0) Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RecordCheck(checked = entry.checked, onClick = { onToggleTask(entry.slotIndex) })
                Spacer(Modifier.width(14.dp))
                Text(
                    text = entry.task,
                    style = VisionType.bodyText(userFont).copy(
                        textDecoration = if (entry.checked) TextDecoration.LineThrough else null
                    ),
                    color = if (entry.checked) OnCanvasMuted else OnCanvas,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Share of a day's tasks that were ticked, or null on a day with no record at all. */
private fun DayRecord?.completion(): Float? =
    this?.let { doneCount.toFloat() / total }

private fun cellColour(fraction: Float?): Color = when {
    fraction == null -> CellNoRecord
    fraction <= 0f -> CellNone
    // Banded rather than matched exactly: a day holds one, two or three tasks, so the
    // ratio lands on thirds or halves and can't be compared for equality.
    fraction < 0.4f -> CellLow
    fraction < 0.6f -> CellHalf
    fraction < 1f -> CellHigh
    else -> CellFull
}

/**
 * Five weeks of days, newest on top. Days after today are simply days with no record —
 * the current week runs to its end like any other rather than being cut short.
 */
@Composable
private fun ConsistencyGrid(weeks: List<List<GridCell>>) {
    // Which square is showing its figures, as row-to-column. Cleared by tapping the
    // square again or anywhere outside it, so only one is ever open.
    var opened by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WeekColumns.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                    style = VisionType.eyebrow,
                    color = OnCanvasMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        weeks.forEachIndexed { index, week ->
            if (index > 0) Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEachIndexed { column, cell ->
                    val key = index to column
                    Box(Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellColour(cell.record.completion()))
                                .clickable { opened = if (opened == key) null else key }
                        )
                        if (opened == key) {
                            CellTooltip(cell = cell, onDismiss = { opened = null })
                        }
                    }
                }
            }
        }
    }
}

/**
 * Centres the tooltip under its square, then holds it inside the content column.
 *
 * The default positioning only keeps a popup within the window, which leaves the Sunday
 * and Saturday tooltips flush against the screen edge. Clamping to the same margin the
 * rest of the screen keeps means an edge square's tooltip lines up with the grid instead.
 */
private class BelowCellWithinContent(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val margin = ((1f - ContentWidthFraction) / 2f * windowSize.width).roundToInt()
        val centred = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val rightmost = windowSize.width - popupContentSize.width - margin
        return IntOffset(
            // A tooltip wider than the column itself would invert the bounds, so the
            // left margin wins rather than coerceIn throwing.
            x = centred.coerceIn(margin, rightmost.coerceAtLeast(margin)),
            y = anchorBounds.bottom + gap
        )
    }
}

/**
 * The tapped square's own figures, floating just under it.
 *
 * A focusable popup so a tap anywhere outside dismisses it, which is the gesture that
 * closes it — the square itself only toggles when tapped a second time.
 */
@Composable
private fun CellTooltip(cell: GridCell, onDismiss: () -> Unit) {
    val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
    Popup(
        popupPositionProvider = remember(gap) { BelowCellWithinContent(gap) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            Modifier
                .shadow(elevation = 8.dp, shape = TooltipShape, clip = false)
                .clip(TooltipShape)
                .background(NavBar)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val record = cell.record
            Text(
                text = buildString {
                    append(cell.date.format(RangeDayFormat).uppercase())
                    append(" · ")
                    // Against the tasks that day held, not a fixed three, so the figure
                    // matches the one on the day's own card in the log above.
                    append(if (record == null) "NO RECORD" else "${record.doneCount} / ${record.total}")
                },
                style = VisionType.eyebrow,
                color = OnNavBar
            )
        }
    }
}

/**
 * The weekday whose five cells add up to the most completion.
 *
 * Every tied day is named rather than one being picked to stand for the rest — choosing
 * arbitrarily would show a pattern the record doesn't actually hold. An empty record ties
 * all seven at zero, which is the one case that says nothing instead.
 */
private fun mostConsistentDay(weeks: List<List<GridCell>>): String {
    val totals = WeekColumns.indices.map { column ->
        weeks.sumOf { week -> (week[column].record.completion() ?: 0f).toDouble() }
    }
    val best = totals.max()
    if (best <= 0.0) return "Not yet"
    return WeekColumns.indices
        .filter { totals[it] == best }
        .joinToString(" · ") {
            WeekColumns[it].getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        }
}

/** A named figure over a hairline, the label held left and the value right. */
@Composable
private fun PatternRow(label: String, value: String, userFont: UserFontChoice) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Rule, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = VisionType.bodyText(userFont),
                color = OnCanvas,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = VisionType.bodyText(userFont),
                color = OnCanvas,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun ReflectionCard(userFont: UserFontChoice) {
    val theme = CardThemes[REFLECTION_THEME_ID]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(theme.surface)
            .padding(24.dp)
    ) {
        Text(text = "REFLECTION", style = VisionType.eyebrow, color = theme.onSurfaceMuted)
        Spacer(Modifier.height(16.dp))
        Text(text = REFLECTION_TEXT, style = VisionType.quote(userFont), color = theme.onSurface)
        Spacer(Modifier.height(16.dp))
        Text(
            text = REFLECTION_NOTE,
            style = VisionType.helperText(userFont),
            color = theme.onSurfaceMuted
        )
    }
}

/** Same mark as Today's rows, drawn for the white canvas instead of a themed card. */
@Composable
private fun RecordCheck(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (checked) Modifier.background(OnCanvas)
                else Modifier.border(1.5.dp, OnCanvasMuted, CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text(text = "✓", style = VisionType.eyebrow, color = Canvas)
        }
    }
}

@Composable
private fun EmptyLog(filter: DayFilter, userFont: UserFontChoice) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, Rule, CardShape)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (filter) {
                DayFilter.All -> "Nothing recorded in this stretch."
                DayFilter.Completed -> "No day was fully kept here."
                DayFilter.Undone -> "Nothing was left undone here."
            },
            style = VisionType.helperText(userFont),
            color = OnCanvasMuted,
            textAlign = TextAlign.Center
        )
    }
}
