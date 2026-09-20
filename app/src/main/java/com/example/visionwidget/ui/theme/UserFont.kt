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
    /**
     * What the face is called where the user picks it — the mood it sets rather than
     * the typeface's own name, so a face can be swapped without renaming the choice.
     * Title case here and uppercased at the point of display, like [CardTheme.name].
     */
    val name: String,
    val family: FontFamily,
    val weight: FontWeight
)

object UserFonts {
    /** Used until the DB is wired up, and as the fallback for an unknown id. */
    const val DEFAULT_ID = 1

    private val byId = listOf(
        // ── Free (1–8) ──
        UserFontChoice(1, "Editorial", InstrumentSerif, FontWeight.Normal),
        UserFontChoice(2, "Modern", DMSans, FontWeight.SemiBold),
        UserFontChoice(3, "Typewriter", DMMono, FontWeight.Normal),
        UserFontChoice(4, "Minimal", DMSans, FontWeight.Light),
        UserFontChoice(5, "Literary", LibreBaskerville, FontWeight.Normal),
        UserFontChoice(6, "Classic", Newsreader, FontWeight.Normal),
        UserFontChoice(7, "Romantic", Lora, FontWeight.Medium),
        UserFontChoice(8, "Antique", EBGaramond, FontWeight.Medium),

        // ── Vision+ (9–21) ──
        UserFontChoice(9, "Quill", CrimsonPro, FontWeight.Normal),
        UserFontChoice(10, "Couture", PlayfairDisplay, FontWeight.Medium),
        UserFontChoice(11, "Soft Serif", Fraunces, FontWeight.SemiBold),
        UserFontChoice(12, "Grotesque", SpaceGrotesk, FontWeight.Medium),
        UserFontChoice(13, "Display", BricolageGrotesque, FontWeight.SemiBold),
        UserFontChoice(14, "Geometric", Outfit, FontWeight.Medium),
        UserFontChoice(15, "Humanist", WorkSans, FontWeight.Medium),
        UserFontChoice(16, "Neutral", PublicSans, FontWeight.SemiBold),
        UserFontChoice(17, "Rounded", Manrope, FontWeight.Bold),
        UserFontChoice(18, "Brutal", Syne, FontWeight.ExtraBold),
        UserFontChoice(19, "Technical", IBMPlexMono, FontWeight.Medium),
        UserFontChoice(20, "Console", JetBrainsMono, FontWeight.Normal),
        UserFontChoice(21, "Stamped", SpaceGrotesk, FontWeight.Bold)
    ).associateBy { it.id }

    /** Every face, in the order the picker lists them. */
    val all: List<UserFontChoice> = byId.values.toList()

    /**
     * Faces above this id are part of the paid tier. Subscription state isn't wired up
     * yet, so they're selectable — the picker only marks them.
     */
    const val LAST_FREE_ID = 8

    private val default: UserFontChoice get() = byId.getValue(DEFAULT_ID)

    /** Falls back to the default rather than crashing on an id the DB adds later. */
    operator fun get(id: Int): UserFontChoice = byId[id] ?: default
}
