package com.example.visionwidget.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionwidget.ui.ContentWidthFraction
import com.example.visionwidget.ui.home.WISDOM
import com.example.visionwidget.ui.home.Wisdom
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.CardTheme
import com.example.visionwidget.ui.theme.CardThemes
import com.example.visionwidget.ui.theme.DMMono
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType
import com.example.visionwidget.ui.vision.Vision
import com.example.visionwidget.ui.vision.formatTargetDate
import com.example.visionwidget.ui.vision.formatWeeksLeft

/** The panel the mock phone sits on — a warm neutral, distinct from the white canvas. */
private val Backdrop = Color(0xFFF3F1EC)

/** The wallpaper behind the widgets: light at the top, falling away toward the bottom. */
private val WallpaperStops = arrayOf(
    0f to Color(0xFFC9C0B1),
    0.46f to Color(0xFF8E8779),
    1f to Color(0xFF4A463F)
)

// The mock phone is drawn at its reference size, so every measurement inside it is the
// one from the design rather than a fraction that would have to be re-derived.
private val PhoneWidth = 234.dp
private val PhoneHeight = 506.dp
private val PhoneShape = RoundedCornerShape(34.dp)
private val WidgetShape = RoundedCornerShape(16.dp)
private val CheckSize = 9.dp

/** Chrome inside the widgets, sized to the mock phone rather than to the screen. */
private val WidgetEyebrow = TextStyle(
    fontFamily = DMMono,
    fontWeight = FontWeight.Normal,
    fontSize = 8.sp,
    lineHeight = 11.sp,
    letterSpacing = 1.44.sp
)

private val WidgetMeta = TextStyle(
    fontFamily = DMMono,
    fontWeight = FontWeight.Normal,
    fontSize = 7.sp,
    lineHeight = 10.sp,
    letterSpacing = 1.26.sp
)

/** The tick itself — held under the circle it sits in so it can't crowd the edges. */
private val WidgetCheckGlyph = TextStyle(
    fontFamily = DMMono,
    fontWeight = FontWeight.Normal,
    fontSize = 6.sp,
    lineHeight = 6.sp
)

private fun widgetTitle(font: UserFontChoice) = TextStyle(
    fontFamily = font.family,
    fontWeight = font.weight,
    fontSize = 19.sp,
    lineHeight = 20.sp
)

private fun widgetLine(font: UserFontChoice) = TextStyle(
    fontFamily = font.family,
    fontWeight = font.weight,
    fontSize = 9.sp,
    lineHeight = 12.sp
)

private fun widgetQuote(font: UserFontChoice) = TextStyle(
    fontFamily = font.family,
    fontWeight = font.weight,
    fontSize = 11.sp,
    lineHeight = 15.sp
)

/** Where a widget can sit. Only the home screen is drawn so far. */
private enum class WidgetSurface(val label: String) {
    Home("Home"),
    Lock("Lock"),
    StandBy("StandBy")
}

/**
 * Previews how the widgets read on a phone, using the user's own data rather than
 * sample copy — the point is to show what they would actually see.
 *
 * The vision shown is the main one, the same one Today follows, since that's the one
 * the widgets are bound to.
 */
@Composable
fun StudioScreen(
    vision: Vision? = null,
    topThreeTasks: List<String?> = List(3) { null },
    topThreeChecked: List<Boolean> = List(3) { false },
    wisdomIndex: Int = 0,
    visionThemeId: Int = CardThemes.DEFAULT_ID,
    topThreeThemeId: Int = CardThemes.DEFAULT_ID,
    wisdomThemeId: Int = CardThemes.DEFAULT_ID,
    userFontId: Int = UserFonts.DEFAULT_ID,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val userFont = UserFonts[userFontId]
    var surface by rememberSaveable { mutableStateOf(WidgetSurface.Home) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxWidth(ContentWidthFraction)) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Studio",
                    style = VisionType.greeting(userFont),
                    color = OnCanvas
                )
                PlanChip(label = "FREE PLAN")
            }
            Spacer(Modifier.height(20.dp))
        }

        // Full-bleed rather than held to the content column: the panel is the backdrop
        // the phone stands on, so it reads better running edge to edge.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Backdrop)
                .padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhonePreview(
                surface = surface,
                vision = vision,
                topThreeTasks = topThreeTasks,
                topThreeChecked = topThreeChecked,
                wisdom = WISDOM[wisdomIndex.coerceIn(WISDOM.indices)],
                visionTheme = CardThemes[visionThemeId],
                topThreeTheme = CardThemes[topThreeThemeId],
                wisdomTheme = CardThemes[wisdomThemeId],
                userFont = userFont
            )
            Spacer(Modifier.height(16.dp))
            SurfaceSwitch(
                selected = surface,
                userFont = userFont,
                onSelect = { surface = it }
            )
        }

        Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
    }
}

@Composable
private fun PlanChip(label: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, Rule, CircleShape)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(text = label, style = VisionType.eyebrow, color = OnCanvas)
    }
}

/**
 * The mock phone. Its wallpaper is drawn rather than themed — it stands in for whatever
 * the user has behind their own widgets, so it stays neutral against every card colour.
 */
@Composable
private fun PhonePreview(
    surface: WidgetSurface,
    vision: Vision?,
    topThreeTasks: List<String?>,
    topThreeChecked: List<Boolean>,
    wisdom: Wisdom,
    visionTheme: CardTheme,
    topThreeTheme: CardTheme,
    wisdomTheme: CardTheme,
    userFont: UserFontChoice
) {
    Box(
        modifier = Modifier
            .width(PhoneWidth)
            .height(PhoneHeight)
            .clip(PhoneShape)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colorStops = WallpaperStops,
                        // Anchored to the top edge so the light falls from above, the
                        // way a wallpaper usually does.
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 1.15f
                    )
                )
            }
    ) {
        // Only the home screen has widgets drawn; the other surfaces show the bare
        // wallpaper until they're built.
        if (surface != WidgetSurface.Home) return@Box

        Column(
            modifier = Modifier.padding(start = 16.dp, top = 38.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VisionWidget(vision = vision, theme = visionTheme, userFont = userFont)
            TopThreeWidget(
                tasks = topThreeTasks,
                checked = topThreeChecked,
                theme = topThreeTheme,
                userFont = userFont
            )
            WisdomWidget(wisdom = wisdom, theme = wisdomTheme, userFont = userFont)
            AppIconRow()
        }
    }
}

/** The frame every widget shares — surface, hairline and inner spacing. */
@Composable
private fun WidgetCard(theme: CardTheme, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = WidgetShape, clip = false)
            .clip(WidgetShape)
            .background(theme.surface)
            .border(1.dp, theme.onSurfaceRule, WidgetShape)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content
    )
}

@Composable
private fun VisionWidget(vision: Vision?, theme: CardTheme, userFont: UserFontChoice) {
    WidgetCard(theme) {
        Text(text = "VISION", style = WidgetEyebrow, color = theme.onSurfaceMuted)
        Text(
            text = vision?.goal ?: "No vision yet",
            style = widgetTitle(userFont),
            color = theme.onSurface
        )
        HorizontalDivider(color = theme.onSurfaceRule, thickness = 1.dp)

        val milestones = vision?.milestones.orEmpty()
        if (milestones.isEmpty()) {
            EmptyWidgetLine(theme = theme, userFont = userFont)
        } else {
            milestones.forEach { milestone ->
                WidgetTaskRow(
                    text = milestone.step,
                    checked = milestone.checked,
                    theme = theme,
                    userFont = userFont
                )
            }
        }

        HorizontalDivider(color = theme.onSurfaceRule, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = vision?.let { formatTargetDate(it.targetDateMillis).uppercase() }.orEmpty(),
                style = WidgetMeta,
                color = theme.onSurfaceMuted
            )
            Text(
                text = vision?.let { formatWeeksLeft(it.targetDateMillis) }.orEmpty(),
                style = WidgetMeta,
                color = theme.onSurfaceMuted
            )
        }
    }
}

@Composable
private fun TopThreeWidget(
    tasks: List<String?>,
    checked: List<Boolean>,
    theme: CardTheme,
    userFont: UserFontChoice
) {
    val set = tasks.indices.filter { tasks[it] != null }
    val done = set.count { checked[it] }

    WidgetCard(theme) {
        Text(
            // Against the tasks that were set rather than a fixed three, matching the
            // count on Today's own card.
            text = "TODAY'S TOP 3 · $done / ${set.size}",
            style = WidgetEyebrow,
            color = theme.onSurfaceMuted
        )
        if (set.isEmpty()) {
            EmptyWidgetLine(theme = theme, userFont = userFont)
        } else {
            set.forEach { index ->
                WidgetTaskRow(
                    text = tasks[index].orEmpty(),
                    checked = checked[index],
                    theme = theme,
                    userFont = userFont
                )
            }
        }
    }
}

/**
 * A ticked-or-not line inside a widget. Milestones and today's tasks are different
 * things, but they read the same way on a widget, so they share the row.
 */
@Composable
private fun WidgetTaskRow(
    text: String,
    checked: Boolean,
    theme: CardTheme,
    userFont: UserFontChoice
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(CheckSize)
                .clip(CircleShape)
                .then(
                    if (checked) Modifier.background(theme.onSurface)
                    else Modifier.border(1.5.dp, theme.onSurfaceMuted, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(text = "✓", style = WidgetCheckGlyph, color = theme.surface)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = widgetLine(userFont).copy(
                textDecoration = if (checked) TextDecoration.LineThrough else null
            ),
            color = if (checked) theme.onSurfaceMuted else theme.onSurface
        )
    }
}

/** Shared wording, so an empty vision and an empty Top 3 read the same on the phone. */
@Composable
private fun EmptyWidgetLine(theme: CardTheme, userFont: UserFontChoice) {
    Text(
        text = "Nothing set yet.",
        style = widgetLine(userFont),
        color = theme.onSurfaceMuted
    )
}

@Composable
private fun WisdomWidget(wisdom: Wisdom, theme: CardTheme, userFont: UserFontChoice) {
    WidgetCard(theme) {
        Text(text = wisdom.text, style = widgetQuote(userFont), color = theme.onSurface)
        Text(
            text = wisdom.category.uppercase(),
            style = WidgetMeta,
            color = theme.onSurfaceMuted
        )
    }
}

/** Stand-ins for the app icons that would sit under the widgets, fading down the row. */
@Composable
private fun AppIconRow() {
    Row(
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        listOf(0.22f, 0.18f, 0.14f, 0.10f).forEach { alpha ->
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

/** Which surface the preview is showing. The other two are placeholders for now. */
@Composable
private fun SurfaceSwitch(
    selected: WidgetSurface,
    userFont: UserFontChoice,
    onSelect: (WidgetSurface) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(OnCanvas.copy(alpha = 0.06f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WidgetSurface.entries.forEach { entry ->
            val isSelected = entry == selected
            Box(
                modifier = Modifier
                    .then(
                        if (isSelected) {
                            Modifier.shadow(elevation = 3.dp, shape = CircleShape, clip = false)
                        } else {
                            Modifier
                        }
                    )
                    .clip(CircleShape)
                    .background(if (isSelected) Canvas else Color.Transparent)
                    .clickable { onSelect(entry) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    // The names read as places, not as chrome labels, so they keep
                    // their capitals and the user's own face.
                    text = entry.label,
                    style = VisionType.bodyText(userFont),
                    color = if (isSelected) OnCanvas else OnCanvasMuted
                )
            }
        }
    }
}
