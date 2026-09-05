package com.example.visionwidget.ui.vision

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** How many visions can exist at once. Three is the whole point of the screen. */
const val MAX_VISIONS = 3

/**
 * What every new vision reports until progress is computed from real work. A constant
 * rather than a field default, so the one place to change is here.
 */
const val PLACEHOLDER_PROGRESS = 25

/**
 * A vision the user set. The date is kept as the epoch millis the picker handed back
 * rather than a formatted string, so the remaining time can be recomputed each day.
 */
data class Vision(
    val id: Long,
    val goal: String,
    val why: String,
    val targetDateMillis: Long,
    val progressPercent: Int = PLACEHOLDER_PROGRESS
)

private val TargetDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

/** The picker hands back UTC midnight of the chosen day, so read it back in UTC. */
private fun Long.asTargetDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** "1 December 2028". */
fun formatTargetDate(millis: Long): String = millis.asTargetDate().format(TargetDateFormat)

/**
 * Whole days from today to the target, floored at zero — a date that has passed reads
 * as nothing left rather than as a negative countdown.
 */
private fun daysUntil(millis: Long): Long =
    ChronoUnit.DAYS.between(LocalDate.now(), millis.asTargetDate()).coerceAtLeast(0)

/** "121 weeks · 843 days" — the Vision tab's fuller line. */
fun formatRemaining(millis: Long): String {
    val days = daysUntil(millis)
    return "${days / 7} weeks · $days days"
}

/** "121 WEEKS LEFT" — the Today card's mono footer, weeks only. */
fun formatWeeksLeft(millis: Long): String = "${daysUntil(millis) / 7} WEEKS LEFT"

/**
 * Flattens each vision to its fields so the list survives process death. A data class
 * isn't Bundle-friendly on its own, and the field order here is the only contract.
 */
val VisionListSaver: Saver<List<Vision>, Any> = listSaver<List<Vision>, List<Any>>(
    save = { visions ->
        visions.map {
            listOf(it.id, it.goal, it.why, it.targetDateMillis, it.progressPercent)
        }
    },
    restore = { stored ->
        stored.map { fields ->
            Vision(
                id = fields[0] as Long,
                goal = fields[1] as String,
                why = fields[2] as String,
                targetDateMillis = fields[3] as Long,
                progressPercent = fields[4] as Int
            )
        }
    }
)
