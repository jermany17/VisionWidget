package com.example.visionwidget.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.visionwidget.ui.theme.WidgetLook
import com.example.visionwidget.ui.theme.WidgetStyle
import com.example.visionwidget.ui.theme.WidgetTarget
import com.example.visionwidget.ui.home.isOwnWisdom
import com.example.visionwidget.ui.home.nextWisdomIndex
import com.example.visionwidget.ui.home.randomWisdomIndex
import com.example.visionwidget.ui.home.wisdomIndices
import com.example.visionwidget.ui.vision.Milestone
import com.example.visionwidget.widget.updateDesignWidgets
import com.example.visionwidget.ui.vision.Vision
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

/** Where a refused widget redraw is reported, since the home screen won't show it. */
private const val WidgetLogTag = "VisionWidget"

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

    // Preferences don't emit on their own, so the stored values are read once and the
    // flow is what everything observes from then on.
    private val _widgetLooks = MutableStateFlow(
        WidgetTarget.entries.associateWith {
            WidgetLook(preferences.widgetStyle(it), preferences.widgetPhotoUri(it))
        }
    )

    private val _wisdomCategory = MutableStateFlow(preferences.wisdomCategory)

    /** The theme the daily line is drawn from. */
    val wisdomCategory: StateFlow<String> = _wisdomCategory.asStateFlow()

    fun setWisdomCategory(category: String) {
        preferences.wisdomCategory = category
        _wisdomCategory.value = category
        // A theme the line no longer belongs to would leave the card showing something
        // from the one before it. The user's own theme has no pool to draw from, so
        // the index is left where it was for whenever they return to a real theme.
        if (!isOwnWisdom(category) && _wisdomIndex.value !in wisdomIndices(category)) {
            setWisdomIndex(randomWisdomIndex(category))
        } else {
            redrawWidgets()
        }
    }

    private val _wisdomIndex = MutableStateFlow(preferences.wisdomIndex)

    /** Which line the daily card is showing — the same one the widget draws. */
    val wisdomIndex: StateFlow<Int> = _wisdomIndex.asStateFlow()

    fun setWisdomIndex(index: Int) {
        preferences.wisdomIndex = index
        _wisdomIndex.value = index
        redrawWidgets()
    }

    private val _wisdomOwnText = MutableStateFlow(preferences.wisdomOwnText)

    /** The one line the user wrote themselves, shown under their own theme. */
    val wisdomOwnText: StateFlow<String> = _wisdomOwnText.asStateFlow()

    /**
     * Commits the daily line in one act — the theme, the line settled on within it, and
     * the user's own words.
     *
     * Studio drafts all three and holds them until the apply, so they have to land
     * together: written one at a time the card would redraw between them, showing a
     * theme against a line that doesn't belong to it.
     */
    fun applyWisdom(category: String, index: Int, ownText: String) {
        preferences.wisdomCategory = category
        preferences.wisdomIndex = index
        preferences.wisdomOwnText = ownText
        _wisdomCategory.value = category
        _wisdomIndex.value = index
        _wisdomOwnText.value = ownText
        redrawWidgets()
    }

    /** Moves to another line in the same theme, and takes the widget with it. */
    fun shuffleWisdom() {
        setWisdomIndex(nextWisdomIndex(_wisdomIndex.value, _wisdomCategory.value))
    }

    /**
     * How each of the three widgets is dressed. One map rather than a flow per value:
     * Studio may write one widget or all three in a single act, and a map moves as one.
     */
    val widgetLooks: StateFlow<Map<WidgetTarget, WidgetLook>> = _widgetLooks.asStateFlow()

    /**
     * Commits every part of a look at once, to as many widgets as the caller names —
     * Studio applies the five together, so they can't be left half-applied by a caller
     * that only sets one.
     */
    fun applyWidgetStyle(targets: Set<WidgetTarget>, style: WidgetStyle) {
        targets.forEach { preferences.setWidgetStyle(it, style) }
        _widgetLooks.update { looks ->
            looks.mapValues { (target, look) ->
                if (target in targets) look.copy(style = style) else look
            }
        }
        redrawWidgets()
    }

    /**
     * Saved as soon as it's chosen rather than waiting on apply: picking a picture is
     * its own deliberate act, and the preview has nothing to show until it lands.
     */
    fun setWidgetPhoto(target: WidgetTarget, uri: String?) {
        preferences.setWidgetPhotoUri(target, uri)
        _widgetLooks.update { looks ->
            looks + (target to (looks[target] ?: WidgetLook()).copy(photoUri = uri))
        }
        redrawWidgets()
    }

    /**
     * Tells whatever is already on the home screen that it no longer looks right.
     *
     * A widget is otherwise only asked to redraw on its own schedule, which is half an
     * hour away at best — far too long for a change the user just watched themselves
     * make.
     */
    private fun redrawWidgets() {
        // A failure to redraw must not take the caller with it, and must not cancel the
        // watcher below — one unlucky update would otherwise leave every later change
        // unannounced for the rest of the session. Said out loud all the same: a card
        // that quietly keeps the picture it had looks exactly like one nobody asked to
        // redraw, and the two want telling apart.
        viewModelScope.launch {
            runCatching { updateDesignWidgets(getApplication()) }
                .onFailure { Log.w(WidgetLogTag, "Widget redraw failed", it) }
        }
    }

    init {
        // Once per process start: if the live slots still belong to a day that's
        // passed, archive them and clear the board for today. The cards are showing
        // yesterday's three until they're told, so they're told.
        viewModelScope.launch {
            ruleOfThreeDao.rolloverToToday(LocalDate.now().toEpochDay())
            redrawWidgets()
        }

        // Everything the cards show, watched at the source — a backstop behind the
        // redrawWidgets() each write already calls directly. Room's own Flow re-query
        // runs on its own schedule rather than the instant the write returns, so it
        // isn't what a tap should wait on; it only catches a call site that forgot to
        // ask directly, or a write that reached the database some other way.
        viewModelScope.launch {
            combine(
                visionDao.observeVisionsWithMilestones(),
                ruleOfThreeDao.observeSlots()
            ) { visions, slots -> visions to slots }
                .distinctUntilChanged()
                // The first is only what the widgets are already showing.
                .drop(1)
                .collect {
                    runCatching { updateDesignWidgets(getApplication()) }
                        .onFailure { failure -> Log.w(WidgetLogTag, "Widget redraw failed", failure) }
                }
        }
    }

    val visions: StateFlow<List<Vision>> = visionDao.observeVisionsWithMilestones()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), emptyList())

    val mainVisionId: StateFlow<Long?> = visionDao.observeMainVisionId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), null)

    val topThreeTasks: StateFlow<List<String?>> = ruleOfThreeDao.observeSlots()
        .map { slots -> List(visibleSlotCount(slots)) { index -> slots.find { it.slotIndex == index }?.task } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), List(BASE_TOP_THREE_TASKS) { null })

    val topThreeChecked: StateFlow<List<Boolean>> = ruleOfThreeDao.observeSlots()
        .map { slots -> List(visibleSlotCount(slots)) { index -> slots.find { it.slotIndex == index }?.checked ?: false } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), List(BASE_TOP_THREE_TASKS) { false })

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
                // Today's own three are what the card shows, however far back in the
                // log the tap came from.
                redrawWidgets()
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
            redrawWidgets()
        }
    }

    fun editVision(id: Long, goal: String, why: String, targetDateMillis: Long) {
        viewModelScope.launch {
            visionDao.updateVision(id, goal, why, targetDateMillis)
            redrawWidgets()
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
            redrawWidgets()
        }
    }

    fun setMainVision(id: Long) {
        viewModelScope.launch {
            visionDao.setMainVision(id)
            redrawWidgets()
        }
    }

    fun addMilestone(visionId: Long, step: String, dueDateMillis: Long) {
        viewModelScope.launch {
            visionDao.insertMilestone(
                MilestoneEntity(visionId = visionId, step = step, dueDateMillis = dueDateMillis, checked = false)
            )
            redrawWidgets()
        }
    }

    fun toggleMilestone(visionId: Long, milestoneId: Long) {
        viewModelScope.launch {
            visionDao.toggleMilestone(milestoneId)
            redrawWidgets()
        }
    }

    fun deleteMilestone(visionId: Long, milestoneId: Long) {
        viewModelScope.launch {
            visionDao.deleteMilestone(milestoneId)
            redrawWidgets()
        }
    }

    fun setTopThreeText(index: Int, text: String) {
        viewModelScope.launch {
            val checked = topThreeChecked.value.getOrElse(index) { false }
            ruleOfThreeDao.upsert(RuleOfThreeSlotEntity(index, text, checked))
            redrawWidgets()
        }
    }

    fun toggleTopThreeChecked(index: Int) {
        viewModelScope.launch {
            val task = topThreeTasks.value.getOrNull(index) ?: return@launch
            val checked = topThreeChecked.value.getOrElse(index) { false }
            ruleOfThreeDao.upsert(RuleOfThreeSlotEntity(index, task, !checked))
            redrawWidgets()
        }
    }

    fun clearTopThree(index: Int) {
        viewModelScope.launch {
            ruleOfThreeDao.upsert(RuleOfThreeSlotEntity(index, null, false))
            redrawWidgets()
        }
    }
}
