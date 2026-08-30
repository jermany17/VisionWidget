package com.example.visionwidget.ui.theme

import androidx.compose.ui.graphics.Color

// Canvas — the app background. Fixed white, never themed.
val Canvas = Color(0xFFFFFFFF)

/** Text on the white canvas is always pure black. Card interiors use [CardTheme] instead. */
val OnCanvas = Color(0xFF000000)

/** Secondary text on the canvas — same black ink, held back so it reads as support. */
val OnCanvasMuted = OnCanvas.copy(alpha = 0.55f)

// Hairline divider on the canvas — derived the same way as card borders, so every
// hairline sitting on white is the same colour.
val Rule = hairlineFor(Canvas)

// Avatar chip fill. Neutral grey to match the hairlines, a touch lighter so the
// chip reads as a fill rather than competing with the borders around it.
val AvatarFill = Color(0xFFE6E6E6)

// Bottom navigation is fixed chrome, never themed from the DB: a black bar with
// white labels, and a white pill with a black label for the selected tab.
val NavBar = Color(0xFF000000)
val OnNavBar = Color(0xFFFFFFFF)
