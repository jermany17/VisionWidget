package com.example.visionwidget.ui.theme

import androidx.annotation.FontRes
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.visionwidget.R

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
    val weight: FontWeight,
    /**
     * The same face as a font resource.
     *
     * A widget is painted with the platform's own text API rather than with
     * Compose, and that cannot be handed a [FontFamily]. Carried here so the two
     * can never name different files for the same choice.
     */
    @FontRes val resId: Int
)

object UserFonts {
    /** Used until the DB is wired up, and as the fallback for an unknown id. */
    const val DEFAULT_ID = 1

    private val byId = listOf(
        // ── Free (1–8) ──
        UserFontChoice(1, "Editorial", InstrumentSerif, FontWeight.Normal, R.font.instrument_serif_regular),
        UserFontChoice(2, "Modern", DMSans, FontWeight.SemiBold, R.font.dm_sans_variable),
        UserFontChoice(3, "Typewriter", DMMono, FontWeight.Normal, R.font.dm_mono_regular),
        UserFontChoice(4, "Minimal", DMSans, FontWeight.Light, R.font.dm_sans_variable),
        UserFontChoice(5, "Literary", LibreBaskerville, FontWeight.Normal, R.font.libre_baskerville_variable),
        UserFontChoice(6, "Classic", Newsreader, FontWeight.Normal, R.font.newsreader_variable),
        UserFontChoice(7, "Romantic", Lora, FontWeight.Medium, R.font.lora_variable),
        UserFontChoice(8, "Antique", EBGaramond, FontWeight.Medium, R.font.eb_garamond_variable),

        // ── Vision+ (9–21) ──
        UserFontChoice(9, "Quill", CrimsonPro, FontWeight.Normal, R.font.crimson_pro_variable),
        UserFontChoice(10, "Couture", PlayfairDisplay, FontWeight.Medium, R.font.playfair_display_variable),
        UserFontChoice(11, "Soft Serif", Fraunces, FontWeight.SemiBold, R.font.fraunces_variable),
        UserFontChoice(12, "Grotesque", SpaceGrotesk, FontWeight.Medium, R.font.space_grotesk_variable),
        UserFontChoice(13, "Display", BricolageGrotesque, FontWeight.SemiBold, R.font.bricolage_grotesque_variable),
        UserFontChoice(14, "Geometric", Outfit, FontWeight.Medium, R.font.outfit_variable),
        UserFontChoice(15, "Humanist", WorkSans, FontWeight.Medium, R.font.work_sans_variable),
        UserFontChoice(16, "Neutral", PublicSans, FontWeight.SemiBold, R.font.public_sans_variable),
        UserFontChoice(17, "Rounded", Manrope, FontWeight.Bold, R.font.manrope_variable),
        UserFontChoice(18, "Brutal", Syne, FontWeight.ExtraBold, R.font.syne_variable),
        UserFontChoice(19, "Technical", IBMPlexMono, FontWeight.Medium, R.font.ibm_plex_mono_medium),
        UserFontChoice(20, "Console", JetBrainsMono, FontWeight.Normal, R.font.jetbrains_mono_variable),
        UserFontChoice(21, "Stamped", SpaceGrotesk, FontWeight.Bold, R.font.space_grotesk_variable)
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
