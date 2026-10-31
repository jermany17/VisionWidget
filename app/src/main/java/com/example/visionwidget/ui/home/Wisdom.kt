package com.example.visionwidget.ui.home

import kotlin.random.Random

data class Wisdom(
    val category: String,
    val text: String
)

/** Seed list, shown until the DB is wired up. */
val WISDOM = listOf(
    Wisdom("motivation", "Motivation is a guest. Habit is a resident."),
    Wisdom("motivation", "The work you avoid is usually the work that counts."),
    Wisdom("motivation", "Start before you feel ready. Ready arrives later."),
    Wisdom("success", "Clarity comes from action, not from thinking about action."),
    Wisdom("success", "Consistency beats perfection, every single week."),
    Wisdom("success", "Three things. Nothing else today."),
    Wisdom("life", "You are allowed to move slowly and still arrive."),
    Wisdom("life", "Nobody is coming. That is the good news."),
    Wisdom("life", "Put the day down. It has been carried enough."),
    Wisdom("happiness", "Stay with what matters."),
    Wisdom("happiness", "One step is enough today."),
    Wisdom("happiness", "The good life is small, repeated, and yours."),
    Wisdom("wisdom", "You have power over your mind, not outside events."),
    Wisdom("wisdom", "Waste no more time arguing what a good person should be. Be one."),
    Wisdom("wisdom", "Tomorrow starts tonight.")
)

/**
 * The themes offered as a choice, in the order they're listed. Named here rather than at
 * each picker so onboarding and Studio can't drift into offering different sets.
 */
val WISDOM_THEMES = listOf(
    "Motivation",
    "Success",
    "Life",
    "Happiness",
    "Wisdom"
)

/**
 * Where the daily line is drawn from until a theme is chosen — skipping the question is
 * answered with the first theme it offered rather than with all of them at once.
 */
val DEFAULT_WISDOM_THEME = WISDOM_THEMES.first().lowercase()

/**
 * The line the user wrote themselves, which belongs to no theme in the list above.
 *
 * Held as a category rather than as a flag beside one: everywhere a theme is read, this
 * is the same kind of answer, and the card asks the same question of both.
 */
const val OWN_WISDOM_CATEGORY = "your own"

/** What that choice is called where the themes are offered. */
const val OWN_WISDOM_LABEL = "Your own"

/** Stands in until a line is written, so the card is never left blank. */
const val OWN_WISDOM_PLACEHOLDER = "Write a line of your own."

/**
 * How long the user's own line can run. The widest the quote is ever set is the 4x4
 * card, which holds about this much before it has to start cutting.
 */
const val MAX_OWN_WISDOM_LENGTH = 70

fun isOwnWisdom(category: String) = category.equals(OWN_WISDOM_CATEGORY, ignoreCase = true)

/**
 * The line a category is showing — the user's own, or the one at [index].
 *
 * Every card goes through here rather than reaching into [WISDOM] by index, since under
 * the user's own theme there is no index to reach by.
 */
fun wisdomFor(category: String, index: Int, ownText: String): Wisdom =
    if (isOwnWisdom(category)) {
        Wisdom(OWN_WISDOM_CATEGORY, ownText.ifBlank { OWN_WISDOM_PLACEHOLDER })
    } else {
        WISDOM[index.coerceIn(WISDOM.indices)]
    }

/**
 * The lines belonging to [category].
 *
 * A name with nothing under it falls back to the whole list rather than to nothing: an
 * empty theme would leave the card with no quote to draw at all.
 */
fun wisdomIndices(category: String): List<Int> {
    if (isOwnWisdom(category)) return emptyList()
    val matching = WISDOM.indices.filter { WISDOM[it].category.equals(category, ignoreCase = true) }
    return matching.ifEmpty { WISDOM.indices.toList() }
}

/** A line to open on, drawn from within [category]. */
fun randomWisdomIndex(category: String): Int = wisdomIndices(category).random()

/**
 * Picks an entry other than [current] from within [category], so a shuffle tap always
 * visibly changes the quote instead of sometimes landing on the one already showing.
 */
fun nextWisdomIndex(current: Int, category: String): Int {
    val pool = wisdomIndices(category)
    if (pool.size < 2) return pool.firstOrNull() ?: current
    val position = pool.indexOf(current)
    // Showing a line from outside the theme — it was just changed under the card — so
    // anything in the new theme is a move rather than a repeat.
    if (position < 0) return pool.random()
    return pool[(position + 1 + Random.nextInt(pool.size - 1)) % pool.size]
}
