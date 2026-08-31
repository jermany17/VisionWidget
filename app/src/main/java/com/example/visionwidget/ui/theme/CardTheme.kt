package com.example.visionwidget.ui.theme

import androidx.compose.ui.graphics.Color

/** How far a hairline is pulled toward black from the surface it outlines. */
private const val BORDER_DARKEN = 0.12f

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
    val surface: Color,
    val onSurface: Color
) {
    /**
     * Light cards sit on a white canvas with nothing to separate them, so they get a
     * hairline in their own colour, slightly darkened. Dark cards already contrast
     * with the canvas and get none.
     */
    val border: Color? = if (onSurface == Color.Black) hairlineFor(surface) else null

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

    private val byId = listOf(
        CardTheme(1, Color(0xFF3A2A3C), Color.White),
        CardTheme(2, Color(0xFF100F0E), Color.White),
        CardTheme(3, Color(0xFFF4F1E8), Color.Black),
        CardTheme(4, Color(0xFFFFFFFF), Color.Black),
        CardTheme(5, Color(0xFFEFE9DC), Color.Black),
        CardTheme(6, Color(0xFFE7DED1), Color.Black),
        CardTheme(7, Color(0xFFDCE4D8), Color.Black),
        CardTheme(8, Color(0xFFE2E4E3), Color.Black)
    ).associateBy { it.id }

    val default: CardTheme get() = byId.getValue(DEFAULT_ID)

    /** Falls back to the default rather than crashing on an id the DB adds later. */
    operator fun get(id: Int): CardTheme = byId[id] ?: default
}
