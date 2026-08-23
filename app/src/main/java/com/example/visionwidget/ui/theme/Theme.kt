package com.example.visionwidget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// The warm-paper look is the brand, so the palette is fixed: no dynamic color,
// no dark variant. Both would repaint the surfaces this design is built around.
private val VisionColorScheme = lightColorScheme(
    primary = Amber,
    onPrimary = Canvas,
    secondary = Indigo,
    onSecondary = Canvas,
    background = Canvas,
    onBackground = Ink,
    surface = Plum,
    onSurface = Cream,
    surfaceVariant = AvatarFill,
    onSurfaceVariant = InkMuted,
    outline = Rule
)

@Composable
fun VisionWidgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VisionColorScheme,
        typography = Typography,
        content = content
    )
}
