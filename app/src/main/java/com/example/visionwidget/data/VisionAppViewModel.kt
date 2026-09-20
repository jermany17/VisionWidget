package com.example.visionwidget.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.visionwidget.ui.vision.Milestone
import com.example.visionwidget.ui.vision.Vision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun MilestoneEntity.toDomain() = Milestone(
    id = id,
    step = step,
    dueDateMillis = dueDateMillis,
    checked = checked
)

private fun VisionWithMilestones.toDomain() = Vision(
    id = vision.id,
    goal = vision.goal,
    why = vision.why,
    targetDateMillis = vision.targetDateMillis,
    milestones = milestones.map { it.toDomain() }
)

/** How long a Flow keeps collecting with no observers before it's torn down. */
private const val StopTimeoutMillis = 5_000L

/**
 * Every day that holds at least one task, newest first.
 *
 * Archived days come from history; the live day is still sitting in the slots table and
 * is folded in under whichever date those slots belong to, so Insights shows today
 * alongside the rest instead of waiting for the rollover to file it.
 */
private fun buildDayRecords(
    history: List<RuleOfThreeHistoryEntity>,
    slots: List<RuleOfThreeSlotEntity>,
    liveDate: Long?
): List<DayRecord> {
    val byDay = history
        .groupBy { it.date }
        .mapValues { (_, rows) -> rows.toEntries { it.slotIndex to (it.task to it.checked) } }
        .toMutableMap()

    if (liveDate != null) {
        val live = slots.toEntries { it.slotIndex to (it.task to it.checked) }
        // An empty live day mustn't erase an archived record for the same date.
        if (live.isNotEmpty()) byDay[liveDate] = live
    }

    return byDay
        .filterValues { it.isNotEmpty() }
        .map { (day, entries) -> DayRecord(day, entries) }
        .sortedByDescending { it.epochDay }
}

/** Drops the slots that were never filled and orders what's left by position. */
private fun <T> List<T>.toEntries(read: (T) -> Pair<Int, Pair<String?, Boolean>>): List<TopThreeEntry> =
    mapNotNull { row ->
        val (slotIndex, value) = read(row)
        val (task, checked) = value
        task?.let { TopThreeEntry(slotIndex, it, checked) }
    }.sortedBy { it.slotIndex }

/**
 * Owns the Room database and exposes it as the reactive state and plain callbacks
 * [VisionApp] and its screens already expect — the database replaces the
 * rememberSaveable state that used to stand in for it, nothing above this changes.
 */
class VisionAppViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val visionDao = db.visionDao()
    private val ruleOfThreeDao = db.ruleOfThreeDao()
    private val preferences = AppPreferences(application)

    // Preferences don't emit on their own, so the stored value is read once and the
    // flow is what everything observes from then on.
    private val _widgetFontId = MutableStateFlow(preferences.widgetFontId)

    /** The face the widget cards render in. The rest of the app keeps the default. */
    val widgetFontId: StateFlow<Int> = _widgetFontId.asStateFlow()

    fun setWidgetFont(id: Int) {
        preferences.widgetFontId = id
        _widgetFontId.value = id
    }

    init {
        // Once per process start: if the live slots still belong to a day that's
        // passed, archive them and clear the board for today.
        viewModelScope.launch {
            ruleOfThreeDao.rolloverToToday(LocalDate.now().toEpochDay())
        }
    }

    val visions: StateFlow<List<Vision>> = visionDao.observeVisionsWithMilestones()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), emptyList())

    val mainVisionId: StateFlow<Long?> = visionDao.observeMainVisionId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), null)

    val topThreeTasks: StateFlow<List<String?>> = ruleOfThreeDao.observeSlots()
        .map { slots -> List(3) { index -> slots.find { it.slotIndex == index }?.task } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), List(3) { null })

    val topThreeChecked: StateFlow<List<Boolean>> = ruleOfThreeDao.observeSlots()
        .map { slots -> List(3) { index -> slots.find { it.slotIndex == index }?.checked ?: false } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), List(3) { false })

    /** Every day on record, newest first — what the Insights tab reads. */
    val dayRecords: StateFlow<List<DayRecord>> = combine(
        ruleOfThreeDao.observeHistory(),
        ruleOfThreeDao.observeSlots(),
        ruleOfThreeDao.observeLiveDate()
    ) { history, slots, liveDate -> buildDayRecords(history, slots, liveDate) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), emptyList())

    /**
     * Tick a slot on any day from Insights, including days long past. The live day still
     * lives in the slots table rather than history, so which table to write depends on
     * the date rather than on how old it looks.
     */
    fun toggleRecordedTask(epochDay: Long, slotIndex: Int) {
        viewModelScope.launch {
            if (epochDay == ruleOfThreeDao.liveDate()) {
                ruleOfThreeDao.toggleSlot(slotIndex)
            } else {
                ruleOfThreeDao.toggleHistory(epochDay, slotIndex)
            }
        }
    }

    fun createVision(goal: String, why: String, targetDateMillis: Long) {
        viewModelScope.launch {
            // The very first vision is main by default; later ones stay secondary
            // until the user says otherwise.
            val isFirst = visionDao.countVisions() == 0
            visionDao.insertVision(
                VisionEntity(goal = goal, why = why, targetDateMillis = targetDateMillis, isMain = isFirst)
            )
        }
    }

    fun editVision(id: Long, goal: String, why: String, targetDateMillis: Long) {
        viewModelScope.launch {
            visionDao.updateVision(id, goal, why, targetDateMillis)
        }
    }

    fun deleteVision(id: Long) {
        viewModelScope.launch {
            visionDao.deleteVision(id)
            // Main can't point at a vision that no longer exists — hand the job to
            // whichever one is left, if any.
            if (visionDao.countMain() == 0) {
                visionDao.firstVisionId()?.let { visionDao.setMain(it) }
            }
        }
    }

    fun setMainVision(id: Long) {
        viewModelScope.launch { visionDao.setMainVision(id) }
    }

    fun addMilestone(visionId: Long, step: String, dueDateMillis: Long) {
        viewModelScope.launch {
            visionDao.insertMilestone(
                MilestoneEntity(visionId = visionId, step = step, dueDateMillis = dueDateMillis, checked = false)
            )
        }
    }

    fun toggleMilestone(visionId: Long, milestoneId: Long) {
        viewModelScope.launch { visionDao.toggleMilestone(milestoneId) }
    }

    fun deleteMilestone(visionId: Long, milestoneId: Long) {
        viewModelScope.launch { visionDao.deleteMilestone(milestoneId) }
    }

    fun setTopThreeText(index: Int, text: String) {
        viewModelScope.launch {
            val checked = topThreeChecked.value.getOrElse(index) { false }
            ruleOfThreeDao.upsert(RuleOfThreeSlotEntity(index, text, checked))
        }
    }

    fun toggleTopThreeChecked(index: Int) {
        viewModelScope.launch {
            val task = topThreeTasks.value.getOrNull(index) ?: return@launch
            val checked = topThreeChecked.value.getOrElse(index) { false }
            ruleOfThreeDao.upsert(RuleOfThreeSlotEntity(index, task, !checked))
        }
    }

    fun clearTopThree(index: Int) {
        viewModelScope.launch {
            ruleOfThreeDao.upsert(RuleOfThreeSlotEntity(index, null, false))
        }
    }
}
