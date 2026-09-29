package com.example.visionwidget.ui.theme

/**
 * A named set of every choice Studio offers — a starting point rather than a mode.
 *
 * Picking one fills in all five controls below it; changing any single control after
 * that simply leaves the set, and the arrangement carries on as the user's own.
 */
data class ThemePreset(
    val id: Int,
    val name: String,
    val fontId: Int,
    val themeId: Int,
    val alignId: Int,
    val backgroundId: Int,
    val cornerRadius: Int
) {
    /** Part of the paid tier. Subscription state isn't wired up; the picker only marks it. */
    val isPlus: Boolean get() = id > ThemePresets.LAST_FREE_ID
}

object ThemePresets {
    const val LAST_FREE_ID = 3

    /** Every set, in the order the row scrolls through them. */
    val all = listOf(
        // ── Free (1–3) ──
        ThemePreset(1, "Scandinavian", 4, 4, 1, 1, 30),
        ThemePreset(2, "Editorial", 1, 3, 1, 1, 14),
        ThemePreset(3, "Dark Focus", 2, 2, 1, 2, 26),

        // ── Vision+ (4–9) ──
        ThemePreset(4, "Film Diary", 3, 19, 2, 4, 20),
        ThemePreset(5, "Zen", 6, 6, 2, 1, 34),
        ThemePreset(6, "Forest", 1, 28, 1, 2, 26),
        ThemePreset(7, "Dusk", 7, 29, 2, 2, 30),
        ThemePreset(8, "Bloom", 5, 15, 1, 3, 22),
        ThemePreset(9, "Atelier", 13, 9, 1, 1, 8)
    )

    /**
     * The set a combination happens to be, if any.
     *
     * Derived rather than remembered: a set is only ever a description of the five
     * values, so building one by hand marks it just as selecting it would, and nudging
     * one value away from a set drops the mark without anything having to clear it.
     */
    fun matching(
        fontId: Int,
        themeId: Int,
        alignId: Int,
        backgroundId: Int,
        cornerRadius: Int
    ): ThemePreset? = all.firstOrNull {
        it.fontId == fontId &&
            it.themeId == themeId &&
            it.alignId == alignId &&
            it.backgroundId == backgroundId &&
            it.cornerRadius == cornerRadius
    }
}

/** How round the widget cards are. The presets span this range; the slider offers it all. */
object CornerRadii {
    const val DEFAULT = 16
    const val MIN = 0
    const val MAX = 40
}
