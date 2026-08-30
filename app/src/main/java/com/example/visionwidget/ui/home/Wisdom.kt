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
    Wisdom("wisdom", "Tomorrow starts tonight."),
    Wisdom("mine", "Do the thing you said you would do at 6am."),
    Wisdom("mine", "You promised her you would finish it.")
)

/**
 * Picks an entry other than [current], so a shuffle tap always visibly changes the
 * quote instead of sometimes landing on the one already showing.
 */
fun nextWisdomIndex(current: Int): Int =
    if (WISDOM.size < 2) current
    else (current + 1 + Random.nextInt(WISDOM.size - 1)) % WISDOM.size
