package com.example.visionwidget.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** How a widget card is filled behind its words. */
data class BackgroundStyle(
    val id: Int,
    val name: String,
    /** Part of the paid tier. Subscription state isn't wired up; the picker only marks it. */
    val isPlus: Boolean
)

object BackgroundStyles {
    /** Used until the DB is wired up, and as the fallback for an unknown id. */
    const val DEFAULT_ID = 1

    const val SOLID = 1
    const val GRADIENT = 2
    const val GLASS = 3
    const val PHOTO = 4

    /** Every style, in the order the picker lists them. */
    val all = listOf(
        BackgroundStyle(SOLID, "Solid", isPlus = false),
        BackgroundStyle(GRADIENT, "Gradient", isPlus = false),
        BackgroundStyle(GLASS, "Glass", isPlus = true),
        BackgroundStyle(PHOTO, "Photo", isPlus = true)
    )

    private val byId = all.associateBy { it.id }

    private val default: BackgroundStyle get() = byId.getValue(DEFAULT_ID)

    operator fun get(id: Int): BackgroundStyle = byId[id] ?: default
}

/**
 * A card's resolved surface: what to paint behind it, and the ink that stays legible on
 * top. Keeping the pairing here means a background can't be handed text it would hide.
 */
data class WidgetSkin(
    val background: Brush,
    /** Painted over [background] — the photo scrim, and nothing otherwise. */
    val overlay: Brush?,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val onSurfaceRule: Color,
    val border: Color?,
    /**
     * Drawn on top of [onSurface] — the tick inside a filled checkbox. Read against the
     * fill rather than the card, since under a photo there's no flat surface to borrow.
     */
    val onInk: Color,
    /**
     * Whether the card lifts off the wallpaper. A translucent panel can't: the shadow
     * meant to sit behind it shows through instead, reading as a smudge in its middle.
     */
    val castsShadow: Boolean
)

/** The design's fixed angle for the gradient fill. */
private const val GRADIENT_ANGLE = 158f

// Photo hands the theme's own ink back: the picture underneath is unknown, so the scrim
// is what guarantees contrast and the text colour has to be fixed to match it.
private val PhotoInk = Color(0xFFF7F4EE)
private const val PHOTO_MUTED_ALPHA = 0.76f

/** The scrim's own range — not adjustable, because it's what makes any photo safe. */
private const val PHOTO_SCRIM_TOP = 0.30f
private const val PHOTO_SCRIM_BOTTOM = 0.62f

/** How much of the wallpaper a glass panel lets through. */
private const val GLASS_ALPHA = 0.74f

fun widgetSkin(theme: CardTheme, style: BackgroundStyle): WidgetSkin = when (style.id) {
    BackgroundStyles.GRADIENT -> WidgetSkin(
        background = angledGradient(GRADIENT_ANGLE, listOf(theme.surface, theme.surfaceStep)),
        overlay = null,
        onSurface = theme.onSurface,
        onSurfaceMuted = theme.onSurfaceMuted,
        onSurfaceRule = theme.onSurfaceRule,
        border = theme.border,
        onInk = theme.surface,
        castsShadow = true
    )

    BackgroundStyles.GLASS -> WidgetSkin(
        // One flat pane, not a gradient: glass is a blur of what's behind it, and any
        // shading of its own reads as a second surface rather than as depth.
        background = SolidColorBrush(theme.surface.copy(alpha = GLASS_ALPHA)),
        overlay = null,
        onSurface = theme.onSurface,
        onSurfaceMuted = theme.onSurfaceMuted,
        onSurfaceRule = theme.onSurfaceRule,
        border = theme.onSurface.copy(alpha = 0.22f),
        onInk = theme.surface,
        castsShadow = false
    )

    BackgroundStyles.PHOTO -> WidgetSkin(
        background = placeholderPhoto(),
        overlay = Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = PHOTO_SCRIM_TOP),
                Color.Black.copy(alpha = PHOTO_SCRIM_BOTTOM)
            )
        ),
        onSurface = PhotoInk,
        onSurfaceMuted = PhotoInk.copy(alpha = PHOTO_MUTED_ALPHA),
        onSurfaceRule = PhotoInk.copy(alpha = 0.22f),
        border = null,
        onInk = Color(0xFF1C1A17),
        castsShadow = true
    )

    else -> WidgetSkin(
        background = SolidColorBrush(theme.surface),
        overlay = null,
        onSurface = theme.onSurface,
        onSurfaceMuted = theme.onSurfaceMuted,
        onSurfaceRule = theme.onSurfaceRule,
        border = theme.border,
        onInk = theme.surface,
        castsShadow = true
    )
}

/** Stands in for the user's picture until one is chosen. */
private fun placeholderPhoto(): Brush = ShaderBrush(
    LinearGradientShader(
        from = Offset.Zero,
        to = Offset(14f, 14f),
        colors = listOf(
            Color(0xFF6A6459),
            Color(0xFF6A6459),
            Color(0xFF7A7469),
            Color(0xFF7A7469)
        ),
        colorStops = listOf(0f, 0.5f, 0.5f, 1f),
        tileMode = TileMode.Repeated
    )
)

private fun SolidColorBrush(color: Color): Brush = Brush.verticalGradient(listOf(color, color))

/**
 * A gradient at a fixed angle, measured the way the design states it — zero pointing up,
 * increasing clockwise. Resolved per size rather than per call, so one brush paints a
 * widget card and a preview swatch alike.
 */
private fun angledGradient(angleDegrees: Float, colors: List<Color>): Brush =
    object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            val radians = Math.toRadians(angleDegrees.toDouble())
            val dx = sin(radians).toFloat()
            val dy = -cos(radians).toFloat()
            // Half the span the line covers across the box, so the gradient reaches both
            // corners it points at instead of banding early.
            val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
            val centre = Offset(size.width / 2f, size.height / 2f)
            return LinearGradientShader(
                from = Offset(centre.x - dx * half, centre.y - dy * half),
                to = Offset(centre.x + dx * half, centre.y + dy * half),
                colors = colors
            )
        }
    }
