package com.example.visionwidget.data

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
