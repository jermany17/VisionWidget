package com.example.visionwidget.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * A typeface the user can pick in Studio, paired with the weight it renders at.
 *
 * The database stores only the numeric id, so the family/weight pairing lives here.
 */
data class UserFontChoice(
    val id: Int,
    val family: FontFamily,
    val weight: FontWeight
)

object UserFonts {
    /** Used until the DB is wired up, and as the fallback for an unknown id. */
    const val DEFAULT_ID = 1

    private val byId = listOf(
        // 1 = Editorial (Instrument Serif 400). Further faces get added here.
        UserFontChoice(1, InstrumentSerif, FontWeight.Normal)
    ).associateBy { it.id }

    private val default: UserFontChoice get() = byId.getValue(DEFAULT_ID)

    /** Falls back to the default rather than crashing on an id the DB adds later. */
    operator fun get(id: Int): UserFontChoice = byId[id] ?: default
}
