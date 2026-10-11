package com.example.visionwidget.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * The width every measurement in the app was drawn against.
 *
 * Phones differ in how many dp they are across — not only by hardware, but because the
 * display-size setting changes it on the same device. A layout drawn for one width and
 * handed a narrower one does not shrink; it keeps its type at the stated size and breaks
 * the line instead, which is how COMPLETED came to read COMPLET / ED.
 */
private const val DESIGN_WIDTH_DP = 393f

/**
 * Scales the whole screen to the width it was designed for.
 *
 * One number for everything — type, padding, cards, corners — so a narrower phone gets
 * the same drawing slightly smaller rather than a rearranged one. The alternative,
 * shrinking each label that happens not to fit, saves the line break and loses the
 * proportions with it: STREAK at full size beside a COMPLETED that gave way.
 *
 * The system font-size setting is held at one as well. It is the other thing that
 * moves type without moving anything around it, and a card measured to the pixel for
 * one size of word has nowhere to put a larger one: the date and the countdown meet in
 * the middle of a row and the line breaks. Everything here already scales with the
 * screen, so the setting has nothing left to do but break the drawing.
 */
@Composable
fun ScaledToDesign(content: @Composable () -> Unit) {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val density = LocalDensity.current

    val scaled = remember(widthDp, density) {
        Density(
            density = density.density * (widthDp / DESIGN_WIDTH_DP),
            fontScale = 1f
        )
    }

    CompositionLocalProvider(LocalDensity provides scaled, content = content)
}
