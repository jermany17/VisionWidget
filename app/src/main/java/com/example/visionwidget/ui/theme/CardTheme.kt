package com.example.visionwidget.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** How far a hairline is pulled toward black from the surface it outlines. */
private const val BORDER_DARKEN = 0.12f

/** Above this, a surface is close enough to the white canvas to need outlining. */
private const val LIGHT_SURFACE_LUMINANCE = 0.4f

/** Same colour, pulled toward black. White becomes a faint grey, beige a deeper beige. */
private fun Color.darkenBy(fraction: Float) = Color(
    red = red * (1f - fraction),
    green = green * (1f - fraction),
    blue = blue * (1f - fraction),
    alpha = alpha
)

/** Hairline for a light surface that would otherwise blend into the white canvas. */
fun hairlineFor(surface: Color): Color = surface.darkenBy(BORDER_DARKEN)

/**
 * A card surface and the text colour that always pairs with it.
 *
 * The database stores only the numeric id, so the pairing has to live here —
 * that way text can never be handed a colour that is unreadable on its own card.
 */
data class CardTheme(
    val id: Int,
    /**
     * What the colour is called where the user picks it. Stored in title case and
     * uppercased at the point of display, like every other label in the app.
     */
    val name: String,
    val surface: Color,
    val onSurface: Color
) {
    /**
     * Light cards sit on a white canvas with nothing to separate them, so they get a
     * hairline in their own colour, slightly darkened. Dark cards already contrast
     * with the canvas and get none.
     *
     * Decided from the surface's own brightness rather than by comparing the text to
     * black: the palette pairs each colour with a tuned near-black or near-white, so an
     * equality test would miss every one of them.
     */
    val border: Color? = if (surface.luminance() > LIGHT_SURFACE_LUMINANCE) {
        hairlineFor(surface)
    } else {
        null
    }

    /**
     * Secondary text on the card — meta rows, eyebrows, counters. Derived from
     * [onSurface] so it stays readable whichever surface the DB picks.
     */
    val onSurfaceMuted: Color = onSurface.copy(alpha = 0.55f)

    /** Hairline drawn on the card itself — dividers between rows of its content. */
    val onSurfaceRule: Color = onSurface.copy(alpha = 0.20f)
}

object CardThemes {
    /** Used until the DB is wired up, and as the fallback for an unknown id. */
    const val DEFAULT_ID = 1

    /**
     * Colours above this id are part of the paid tier. Subscription state isn't wired
     * up yet, so they're selectable — the picker only marks them.
     */
    const val LAST_FREE_ID = 13

    private val byId = listOf(
        // ── Free (1–13) ──
        CardTheme(1, "Plum", Color(0xFF503A53), Color(0xFFEEECEE)),
        CardTheme(2, "Black", Color(0xFF100F0E), Color(0xFFF2EFE9)),
        CardTheme(3, "Paper", Color(0xFFF4F1E8), Color(0xFF252219)),
        CardTheme(4, "White", Color(0xFFFFFFFF), Color(0xFF1F1F1F)),
        CardTheme(5, "Linen", Color(0xFFEFE7D6), Color(0xFF262117)),
        CardTheme(6, "Fog", Color(0xFFEAECEB), Color(0xFF1E1F1F)),
        CardTheme(7, "Sage", Color(0xFFDCE4D8), Color(0xFF1E221C)),
        CardTheme(8, "Almond", Color(0xFFE3D3C0), Color(0xFF241E17)),
        CardTheme(9, "Clay", Color(0xFFE2CBBB), Color(0xFF251D18)),
        CardTheme(10, "Petal", Color(0xFFEBD2CE), Color(0xFF261B19)),
        CardTheme(11, "Iris", Color(0xFFD3CADF), Color(0xFF1F1C26)),
        CardTheme(12, "Deep Sky", Color(0xFFA7B9CB), Color(0xFF1A1F23)),
        CardTheme(13, "Cocoa", Color(0xFF33261E), Color(0xFFEFEAE4)),

        // ── Vision+ (14–30) ──
        CardTheme(14, "Butter", Color(0xFFF8EDC6), Color(0xFF2C2611)),
        CardTheme(15, "Rose", Color(0xFFF4E5E5), Color(0xFF251818)),
        CardTheme(16, "Sky", Color(0xFFE3EAF1), Color(0xFF191F24)),
        CardTheme(17, "Lilac", Color(0xFFEAE7F4), Color(0xFF1B1825)),
        CardTheme(18, "Olive", Color(0xFFD3D6BE), Color(0xFF21221B)),
        CardTheme(19, "Sand", Color(0xFFE7DED1), Color(0xFF242019)),
        CardTheme(20, "Chestnut", Color(0xFFA98D77), Color(0xFF231C16)),
        CardTheme(21, "Terra", Color(0xFFD9B7A3), Color(0xFF261D18)),
        CardTheme(22, "Ochre", Color(0xFFC0A06B), Color(0xFF252018)),
        CardTheme(23, "Eucalyptus", Color(0xFF82998E), Color(0xFF1D201F)),
        CardTheme(24, "Forest", Color(0xFF2F4539), Color(0xFFECEEED)),
        // Broken across two lines rather than left to wrap: at a sixth of the row these
        // two sit right on the edge of fitting, so the grid would break them unevenly.
        CardTheme(25, "Deep\nSage", Color(0xFFABB9A4), Color(0xFF1E211C)),
        CardTheme(26, "Deep\nLilac", Color(0xFFB5AECA), Color(0xFF1D1B22)),
        CardTheme(27, "Dusty Blush", Color(0xFFD2AEB3), Color(0xFF231A1B)),
        CardTheme(28, "Espresso", Color(0xFF4D3E33), Color(0xFFEEEDEC)),
        CardTheme(29, "Ink", Color(0xFF2C4069), Color(0xFFEBECEF)),
        CardTheme(30, "Cacao", Color(0xFF211714), Color(0xFFF0EBE5))
    ).associateBy { it.id }

    /** Every colour, in the order the picker lists them. */
    val all: List<CardTheme> = byId.values.toList()

    val default: CardTheme get() = byId.getValue(DEFAULT_ID)

    /** Falls back to the default rather than crashing on an id the DB adds later. */
    operator fun get(id: Int): CardTheme = byId[id] ?: default
}
