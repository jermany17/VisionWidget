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

/** How many milestones one vision can hold. Four keeps the list a glance, not a scroll. */
const val MAX_MILESTONES_PER_VISION = 4

/**
 * A vision the user set. The date is kept as the epoch millis the picker handed back
 * rather than a formatted string, so the remaining time can be recomputed each day.
 */
data class Vision(
    val id: Long,
    val goal: String,
    val why: String,
    val targetDateMillis: Long,
    val milestones: List<Milestone> = emptyList()
)

/**
 * One concrete step toward a vision, with the date it's due by. Once added it can only
 * be checked or removed — there's no edit, so nothing here can drift out of sync with
 * itself once it exists.
 */
data class Milestone(
    val id: Long,
    val step: String,
    val dueDateMillis: Long,
    val checked: Boolean = false
)

/**
 * The share of a vision's milestones that are checked — the only progress the app
 * tracks. Zero until at least one milestone exists, rather than a placeholder value.
 */
val Vision.progressPercent: Int
    get() = if (milestones.isEmpty()) 0 else milestones.count { it.checked } * 100 / milestones.size

private val TargetDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

private val ShortDateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yy", Locale.ENGLISH)

/** The picker hands back UTC midnight of the chosen day, so read it back in UTC. */
private fun Long.asTargetDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** "1 December 2028". */
fun formatTargetDate(millis: Long): String = millis.asTargetDate().format(TargetDateFormat)

/** "5 Dec 27" — compact form used inline while picking a milestone's date. */
fun formatShortDate(millis: Long): String = millis.asTargetDate().format(ShortDateFormat)

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
 * Flattens each vision, and each of its milestones in turn, to its fields so the list
 * survives process death. A data class isn't Bundle-friendly on its own, and the field
 * order here — vision fields, then a milestone count, then that many milestone field
 * groups — is the only contract.
 */
val VisionListSaver: Saver<List<Vision>, Any> = listSaver<List<Vision>, List<Any>>(
    save = { visions ->
        visions.map { vision ->
            listOf(vision.id, vision.goal, vision.why, vision.targetDateMillis, vision.milestones.size) +
                vision.milestones.flatMap { listOf(it.id, it.step, it.dueDateMillis, it.checked) }
        }
    },
    restore = { stored ->
        stored.map { fields ->
            val milestoneCount = fields[4] as Int
            val milestones = (0 until milestoneCount).map { index ->
                val offset = 5 + index * 4
                Milestone(
                    id = fields[offset] as Long,
                    step = fields[offset + 1] as String,
                    dueDateMillis = fields[offset + 2] as Long,
                    checked = fields[offset + 3] as Boolean
                )
            }
            Vision(
                id = fields[0] as Long,
                goal = fields[1] as String,
                why = fields[2] as String,
                targetDateMillis = fields[3] as Long,
                milestones = milestones
            )
        }
    }
)
