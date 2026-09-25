package com.example.visionwidget.ui.theme

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign

/**
 * How a widget's own words are set out.
 *
 * Poster isn't a third alignment so much as a treatment: centred, and in capitals. The
 * two travel together, so they're one choice rather than two the user has to combine.
 */
data class AlignChoice(
    val id: Int,
    val name: String,
    val textAlign: TextAlign,
    val uppercase: Boolean
) {
    /** The same choice as a layout alignment, for rows and columns rather than text. */
    val horizontal: Alignment.Horizontal =
        if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start

    /** Applies the treatment to a line of the user's own writing. */
    fun format(text: String): String = if (uppercase) text.uppercase() else text
}

object Alignments {
    /** Used until the DB is wired up, and as the fallback for an unknown id. */
    const val DEFAULT_ID = 1

    private val byId = listOf(
        AlignChoice(1, "Left", TextAlign.Start, uppercase = false),
        AlignChoice(2, "Centered", TextAlign.Center, uppercase = false),
        AlignChoice(3, "Poster", TextAlign.Center, uppercase = true)
    ).associateBy { it.id }

    /** Every layout, in the order the picker lists them. */
    val all: List<AlignChoice> = byId.values.toList()

    private val default: AlignChoice get() = byId.getValue(DEFAULT_ID)

    /** Falls back to the default rather than crashing on an id the DB adds later. */
    operator fun get(id: Int): AlignChoice = byId[id] ?: default
}
