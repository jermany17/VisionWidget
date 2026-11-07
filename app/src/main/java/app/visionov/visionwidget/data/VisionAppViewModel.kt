package app.visionov.visionwidget.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.visionov.visionwidget.ui.theme.WidgetLook
import app.visionov.visionwidget.ui.theme.WidgetStyle
import app.visionov.visionwidget.ui.theme.WidgetTarget
import app.visionov.visionwidget.ui.home.isOwnWisdom
import app.visionov.visionwidget.ui.home.nextWisdomIndex
import app.visionov.visionwidget.ui.home.randomWisdomIndex
import app.visionov.visionwidget.ui.home.wisdomIndices
import app.visionov.visionwidget.ui.vision.Milestone
import app.visionov.visionwidget.ui.gallery.GalleryCatalog
import app.visionov.visionwidget.widget.GalleryTicker
import app.visionov.visionwidget.widget.updateDesignWidgets
import app.visionov.visionwidget.ui.vision.Vision
import kotlinx.coroutines.channels.Channel
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

    private val _libraryDesignIds = MutableStateFlow(preferences.libraryDesignIds)

    /**
     * The Gallery designs kept for the home screen, in the order they were kept.
     *
     * What the launcher's setting screen offers, and what My Library shows. The screen
     * reads it from preferences itself — it runs in its own activity, outside this
     * view model — so this flow is only for the app's own screens.
     */
    val libraryDesignIds: StateFlow<List<String>> = _libraryDesignIds.asStateFlow()

    /** Keeps [designId], or leaves the library alone if it is already there. */
    fun saveToLibrary(designId: String) {
        if (designId in _libraryDesignIds.value) return
        // Appended, so the library reads in the order things were put in it.
        val kept = _libraryDesignIds.value + designId
        preferences.libraryDesignIds = kept
        _libraryDesignIds.value = kept

        // Drawn now, long before anyone asks for it on a home screen.
        //
        // A widget's own update is a broadcast with a deadline, and painting a design
        // means running it through a web view — a second or two it does not have. Left
        // until then, the cell shows that it is still drawing and corrects itself
        // afterwards. Saving is the moment there is time to spare, so it is spent here.
        GalleryCatalog.load(getApplication())
            .firstOrNull { it.id == designId }
            ?.let { GalleryTicker.requestPaint(getApplication(), it) }
    }

    /**
     * Drops [designId] from the library.
     *
     * Widgets already on the home screen showing it are left alone. The library is what
     * can be placed from, not a list of what is placed — reaching onto the home screen
     * to take something away is the launcher's business, and the user's.
     */
    fun removeFromLibrary(designId: String) {
        val kept = _libraryDesignIds.value - designId
        if (kept.size == _libraryDesignIds.value.size) return
        preferences.libraryDesignIds = kept
        _libraryDesignIds.value = kept
    }

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
     * Turns the daily line over when the day has turned under it.
     *
     * Called at launch and again each time the app comes back to the front, because a
     * process can sit in the background through a midnight and a widget can turn the
     * line over while it does. So this resyncs from preferences rather than only
     * writing to them: the turn may already have happened somewhere else.
     */
    fun refreshWisdomForToday() {
        preferences.rollWisdomForDay(LocalDate.now().toEpochDay())
        if (_wisdomIndex.value != preferences.wisdomIndex) {
            _wisdomIndex.value = preferences.wisdomIndex
            redrawWidgets()
        }
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
        requests.trySend(Unit)
    }

    /**
     * Asks for a redraw without queuing one per request.
     *
     * A single edit sets off more than one of these — the write asks directly, and the
     * watcher below sees the same write land and asks again — and typing a task sets off
     * one per keystroke. Run in turn, those stack up behind each other and the last one,
     * which is the only one carrying what the user actually ended up with, arrives last
     * of all. Conflated, a request that comes in while one is running replaces anything
     * else waiting, so a burst costs one redraw and then one more with the final state.
     */
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        // The one place a redraw actually happens. A failure must not take the caller
        // with it, and must not end this loop — one unlucky update would otherwise
        // leave every later change unannounced for the rest of the session. Said out
        // loud all the same: a card that quietly keeps the picture it had looks exactly
        // like one nobody asked to redraw, and the two want telling apart.
        viewModelScope.launch {
            for (request in requests) {
                runCatching { updateDesignWidgets(getApplication()) }
                    .onFailure { Log.w(WidgetLogTag, "Widget redraw failed", it) }
            }
        }

        // Once per process start: if the live slots still belong to a day that's
        // passed, archive them and clear the board for today. The cards are showing
        // yesterday's three until they're told, so they're told.
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            ruleOfThreeDao.rolloverToToday(today)
            // After the board is cleared and before anything can be typed onto it, so a
            // plan can only ever land on empty slots.
            takeUpPlan(today)
            // The day brings a new line with it, whether or not anyone pressed shuffle.
            refreshWisdomForToday()
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
                // Through the same gate as every other request, so a write that asks
                // directly and then lands in the database counts as one redraw rather
                // than two.
                .collect { redrawWidgets() }
        }
    }

    val visions: StateFlow<List<Vision>> = visionDao.observeVisionsWithMilestones()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), emptyList())

    val mainVisionId: StateFlow<Long?> = visionDao.observeMainVisionId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), null)

    // The day's own three — the set the widget shows and the record is kept on.
    val topThreeTasks: StateFlow<List<String?>> = ruleOfThreeDao.observeSlots()
        .map { it.tasksOf(TOP_THREE_SLOTS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), List(BASE_TOP_THREE_TASKS) { null })

    val topThreeChecked: StateFlow<List<Boolean>> = ruleOfThreeDao.observeSlots()
        .map { it.checksOf(TOP_THREE_SLOTS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), List(BASE_TOP_THREE_TASKS) { false })

    // What comes after them, which only Today ever shows.
    val afterThreeTasks: StateFlow<List<String?>> = ruleOfThreeDao.observeSlots()
        .map { it.tasksOf(AFTER_THREE_SLOTS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), List(BASE_TOP_THREE_TASKS) { null })

    val afterThreeChecked: StateFlow<List<Boolean>> = ruleOfThreeDao.observeSlots()
        .map { it.checksOf(AFTER_THREE_SLOTS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), List(BASE_TOP_THREE_TASKS) { false })

    /** Whether the second set is in play at all — see [afterThreeUnlocked]. */
    val afterThreeOpen: StateFlow<Boolean> = ruleOfThreeDao.observeSlots()
        .map { afterThreeUnlocked(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), false)

    /** Whether the day's three are the card in front — see [topThreeLeads]. */
    val topThreeFirst: StateFlow<Boolean> = ruleOfThreeDao.observeSlots()
        .map { it.topThreeLeads() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), true)

    /** Whether what was lined up behind can be ticked yet — see [afterThreeTickable]. */
    val afterThreeTickable: StateFlow<Boolean> = ruleOfThreeDao.observeSlots()
        .map { it.afterThreeTickable() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), false)

    /** Each set's slots in the order its card reads them — see [rowOrder]. */
    val topThreeOrder: StateFlow<List<Int>> = ruleOfThreeDao.observeSlots()
        .map { it.rowOrder(TOP_THREE_SLOTS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), TOP_THREE_SLOTS.toList())

    val afterThreeOrder: StateFlow<List<Int>> = ruleOfThreeDao.observeSlots()
        .map { it.rowOrder(AFTER_THREE_SLOTS) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), AFTER_THREE_SLOTS.toList())

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

    // All three take a slot index across both sets rather than a place in one of them,
    // so the second set is written the same way the first is and neither has to know
    // which it is.
    fun setTopThreeText(index: Int, text: String) {
        viewModelScope.launch {
            val checked = ruleOfThreeDao.slots().checkedAt(index)
            ruleOfThreeDao.upsert(RuleOfThreeSlotEntity(index, text, checked))
            redrawWidgets()
        }
    }

    fun toggleTopThreeChecked(index: Int) {
        viewModelScope.launch {
            val slots = ruleOfThreeDao.slots()
            val task = slots.taskAt(index) ?: return@launch
            ruleOfThreeDao.upsert(RuleOfThreeSlotEntity(index, task, !slots.checkedAt(index)))
            redrawWidgets()
        }
    }

    fun clearTopThree(index: Int) {
        viewModelScope.launch {
            ruleOfThreeDao.upsert(RuleOfThreeSlotEntity(index, null, false))
            redrawWidgets()
        }
    }

    /** What is planned for the day ahead, in slot order — blanks for untouched slots. */
    private val _plannedTasks = MutableStateFlow(preferences.plannedTasks)
    val plannedTasks: StateFlow<List<String>> = _plannedTasks.asStateFlow()

    /**
     * Writes the plan for tomorrow, or clears it when nothing was left in it.
     *
     * Dated as it is saved rather than as it is taken up, so a plan made late at night
     * belongs to the morning that follows it and not to the one after that.
     */
    fun setPlan(tasks: List<String>) {
        val kept = tasks.map { it.trim() }
        if (kept.all { it.isEmpty() }) {
            preferences.clearPlan()
            _plannedTasks.value = emptyList()
            return
        }
        preferences.plannedDay = LocalDate.now().toEpochDay() + 1
        preferences.plannedTasks = kept
        _plannedTasks.value = kept
    }

    /**
     * Takes up the plan the day it was written for, or files it where it belongs.
     *
     * A plan the user never came back for is not today's work — it was a day that was
     * meant to happen and didn't, so it goes into the record under its own date rather
     * than following them forward. Either way it is spent: a plan is for one day, and
     * the day it was for has arrived or gone.
     */
    private suspend fun takeUpPlan(today: Long) {
        val day = preferences.plannedDay
        if (day == 0L || day > today) return

        val tasks = preferences.plannedTasks
        if (day == today) {
            tasks.forEachIndexed { index, task ->
                if (task.isNotBlank()) {
                    ruleOfThreeDao.upsert(RuleOfThreeSlotEntity(index, task, false))
                }
            }
        } else {
            val missed = tasks.mapIndexedNotNull { index, task ->
                task.takeIf { it.isNotBlank() }
                    ?.let { RuleOfThreeHistoryEntity(day, index, it, false) }
            }
            if (missed.isNotEmpty()) ruleOfThreeDao.archive(missed)
        }

        preferences.clearPlan()
        _plannedTasks.value = emptyList()
    }

}
