package com.example.visionwidget.data

/** How many slots one set holds. Three is the whole point of the screen. */
const val BASE_TOP_THREE_TASKS = 3

/** How many a day can hold in total: the two sets of three. */
const val MAX_TOP_THREE_TASKS = 6

/**
 * The day's two sets of three.
 *
 * A day is not one list that grows but two sets that take turns. The first is what the
 * day is actually judged on and the only one a widget shows; the second is held back
 * until the first is full, and takes its place once the first is kept. Which slot
 * indices belong to which is fixed, so a set is a position rather than a thing to be
 * kept track of.
 */
val TOP_THREE_SLOTS = 0 until BASE_TOP_THREE_TASKS
val AFTER_THREE_SLOTS = BASE_TOP_THREE_TASKS until MAX_TOP_THREE_TASKS

fun List<RuleOfThreeSlotEntity>.taskAt(index: Int): String? =
    find { it.slotIndex == index }?.task

fun List<RuleOfThreeSlotEntity>.checkedAt(index: Int): Boolean =
    find { it.slotIndex == index }?.checked == true

/** One set's words, in slot order, with the slots nobody filled left as nulls. */
fun List<RuleOfThreeSlotEntity>.tasksOf(set: IntRange): List<String?> = set.map { taskAt(it) }

fun List<RuleOfThreeSlotEntity>.checksOf(set: IntRange): List<Boolean> = set.map { checkedAt(it) }

/**
 * Whether the second set is in play.
 *
 * Filling the first set is what opens it, and holding anything of its own is what keeps
 * it open: once a task has been written there, emptying a slot in the first set no
 * longer takes it away, because there would be somewhere real to lose. With nothing in
 * it and the first set no longer full there is nothing to keep, and it closes again.
 *
 * Nothing records this. It is read off the slots themselves, so it cannot drift from
 * them, and it starts closed each morning because the slots do.
 */
fun afterThreeUnlocked(slots: List<RuleOfThreeSlotEntity>): Boolean {
    // Nothing written at all means nothing to come after: the day has not been set, and
    // a second card beside an empty first one would be offering to plan past something
    // that does not exist. What was lined up is kept, and comes back with the first task.
    if (TOP_THREE_SLOTS.none { slots.taskAt(it) != null }) return false
    return TOP_THREE_SLOTS.all { slots.taskAt(it) != null } ||
        AFTER_THREE_SLOTS.any { slots.taskAt(it) != null }
}

/** Whether [set] still holds something written and not ticked. */
fun List<RuleOfThreeSlotEntity>.hasUnfinished(set: IntRange): Boolean =
    set.any { taskAt(it) != null && !checkedAt(it) }

/**
 * Whether the day's three come first.
 *
 * One rule decides the order of the two cards, and it is read off the slots every time
 * rather than recorded: anything left in the day's three puts them in front, and only
 * once they are all kept does what was lined up behind take their place. With that
 * finished too the day's three come back, since there is nothing left to be getting on
 * with and they are the ones the day was about.
 *
 * Nothing moves between the sets to make this happen. A task stays in the slot it was
 * written into for the whole day, and what changes is which card is read first — so
 * ticking, unticking, writing and deleting all settle the order the same way, and none
 * of them can leave it somewhere a later one disagrees with.
 */
fun List<RuleOfThreeSlotEntity>.topThreeLeads(): Boolean =
    hasUnfinished(TOP_THREE_SLOTS) || !hasUnfinished(AFTER_THREE_SLOTS)

/**
 * Whether what was lined up behind can be ticked yet.
 *
 * Only once the day's three are done. Before that these are a plan rather than work in
 * hand, and a tick against one would be a tick against something the day has not got to
 * — the card says as much by standing a tone lighter.
 *
 * What was ticked while they were open stays ticked when the day's three reopen. It was
 * done; reopening something else does not undo it.
 */
fun List<RuleOfThreeSlotEntity>.afterThreeTickable(): Boolean =
    !hasUnfinished(TOP_THREE_SLOTS)

/**
 * The slots of [set], ordered as the card reads them.
 *
 * What is left to do first, what is done under it, and the slots nobody wrote in last.
 * A card is a list of what is outstanding, so the thing just ticked off drops below the
 * things that are not — and the thing just unticked rises back to the top of them.
 *
 * Order within each group is the order the slots were written in, so nothing shuffles
 * except across those three lines.
 */
fun List<RuleOfThreeSlotEntity>.rowOrder(set: IntRange): List<Int> =
    set.sortedWith(
        compareBy(
            { if (taskAt(it) == null) 1 else 0 },
            { if (checkedAt(it)) 1 else 0 }
        )
    )

/**
 * How long a single task's own words can run.
 *
 * The same thirty-eight every other field the user writes into is held to, so that what
 * can be said in one place can be said in all of them.
 */
const val MAX_TOP_THREE_TASK_LENGTH = 38


/** One slot of a day's Top 3, as Insights reads it — an empty slot never becomes an entry. */
data class TopThreeEntry(
    val slotIndex: Int,
    val task: String,
    val checked: Boolean
)

/**
 * A single day's Top 3. Only days with at least one task become a record, so "recorded"
 * and "the user set something that day" mean the same thing everywhere downstream.
 */
data class DayRecord(
    val epochDay: Long,
    val entries: List<TopThreeEntry>
) {
    val doneCount: Int get() = entries.count { it.checked }

    val total: Int get() = entries.size

    /**
     * Done means every task that was set got ticked — one task ticked off is a kept day
     * just as much as three, since the count was the user's own choice.
     */
    val isComplete: Boolean get() = entries.isNotEmpty() && entries.all { it.checked }
}

/**
 * The all-time figures above the log. These count every day on record rather than the
 * period being browsed, so moving back through weeks doesn't rewrite the user's totals.
 */
data class TopThreeStats(
    val streak: Int,
    val completed: Int,
    val ratePercent: Int,
    val daysRecorded: Int
)

/** Tasks ticked against tasks set, counted by task rather than by day. */
data class TaskTally(val done: Int, val total: Int)

/**
 * The tally across a stretch of days, both ends inclusive.
 *
 * Counted by task, not by day: a day where two of three were ticked contributes both
 * halves of what it was, where counting days would round it away entirely.
 */
fun tallyBetween(records: List<DayRecord>, fromEpochDay: Long, toEpochDay: Long): TaskTally {
    val within = records.filter { it.epochDay in fromEpochDay..toEpochDay }
    return TaskTally(
        done = within.sumOf { it.doneCount },
        total = within.sumOf { it.total }
    )
}

/**
 * Consecutive kept days ending yesterday. Today is deliberately excluded — it's still
 * open, so counting it would make the streak fall as the day starts and climb again by
 * evening. A yesterday that wasn't kept, or has no record at all, means no streak.
 */
private fun streakEndingYesterday(records: List<DayRecord>, today: Long): Int {
    val kept = records.filter { it.isComplete }.mapTo(HashSet()) { it.epochDay }
    var day = today - 1
    var streak = 0
    while (day in kept) {
        streak++
        day--
    }
    return streak
}

/**
 * The longest run of kept days the record holds, anywhere in it. Unlike the current
 * streak this ignores where it sits — it's the best the user has ever done, so it stands
 * even once the run it describes has been broken.
 */
fun longestStreak(records: List<DayRecord>): Int {
    val kept = records.filter { it.isComplete }.map { it.epochDay }.sorted()
    var best = 0
    var run = 0
    var previous: Long? = null
    for (day in kept) {
        run = if (previous != null && day == previous + 1) run + 1 else 1
        best = maxOf(best, run)
        previous = day
    }
    return best
}

fun topThreeStats(records: List<DayRecord>, today: Long): TopThreeStats {
    val completed = records.count { it.isComplete }
    val recorded = records.size
    return TopThreeStats(
        streak = streakEndingYesterday(records, today),
        completed = completed,
        // Share of the days the user actually set something, not of the calendar —
        // days they never opened the app aren't failures to divide by.
        ratePercent = if (recorded == 0) 0 else Math.round(completed * 100f / recorded),
        daysRecorded = recorded
    )
}
/**
 * Every day that holds at least one task, newest first.
 *
 * Archived days come from history; the live day is still sitting in the slots table and
 * is folded in under whichever date those slots belong to, so Insights shows today
 * alongside the rest instead of waiting for the rollover to file it.
 */
fun buildDayRecords(
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

