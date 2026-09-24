package com.example.visionwidget.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionwidget.R
import com.example.visionwidget.ui.ContentWidthFraction
import com.example.visionwidget.ui.home.WISDOM
import com.example.visionwidget.ui.home.Wisdom
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.CardTheme
import com.example.visionwidget.ui.theme.CardThemes
import com.example.visionwidget.ui.theme.DMMono
import com.example.visionwidget.ui.theme.NavBar
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.OnNavBar
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

/** A floor rather than a fixed height — a wide face wraps the cards and the frame grows. */
private val PhoneMinHeight = 506.dp

/**
 * Room held at the foot of the frame for the dock and the apply control. The widgets
 * are inset by this much so a tall stack pushes the frame down rather than running
 * underneath what's pinned there.
 */
private val PhoneFooterArea = 140.dp
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

/** The mark beside a paid face — warm, so it reads as an offer rather than a warning. */
private val PlusMark = Color(0xFF9A7B4F)

/** The settings button's own fill and hairline, a shade warmer than the canvas. */
private val IconButtonFill = Color(0xFFEFEBE3)
private val IconButtonBorder = Color(0xFF12110F).copy(alpha = 0.09f)

/** The two halves of Studio. Gallery is a placeholder until its own work lands. */
private enum class StudioTab(val label: String) {
    Design("Design"),
    Gallery("Gallery")
}

private fun tabLabel(font: UserFontChoice) = TextStyle(
    fontFamily = font.family,
    fontWeight = font.weight,
    fontSize = 19.sp,
    lineHeight = 24.sp
)

/** How many faces sit across the picker. */
private const val FontColumns = 3

/** How many colours sit across their picker — smaller cells, so more of them. */
private const val ThemeColumns = 6

private val SwatchShape = RoundedCornerShape(10.dp)

/** A swatch's name. Narrow cells, so tighter than the eyebrow used elsewhere. */
private val SwatchLabel = TextStyle(
    fontFamily = DMMono,
    fontWeight = FontWeight.Normal,
    fontSize = 8.sp,
    lineHeight = 11.sp,
    letterSpacing = 0.4.sp
)

/** The paid mark on a swatch, read against the colour it sits on. */
private val SwatchPlus = TextStyle(
    fontFamily = DMMono,
    fontWeight = FontWeight.Normal,
    fontSize = 9.sp,
    lineHeight = 10.sp
)

/** One wording for both apply controls: they do the same thing, so they read the same. */
private const val ApplyLabelText = "Apply to my widgets"

// The applied state's own colours. Fixed rather than taken from the surface behind
// them, so the control looks the same on the wallpaper and on the white canvas.
private val AppliedFill = Color(0xFF3B3733)
private val AppliedBorder = Color.White.copy(alpha = 0.28f)
private val AppliedInk = Color.White.copy(alpha = 0.65f)

/** Sized to the mock phone it sits in rather than to the screen around it. */
private fun applyLabel(font: UserFontChoice) = TextStyle(
    fontFamily = font.family,
    fontWeight = font.weight,
    fontSize = 14.sp,
    lineHeight = 18.sp
)

/**
 * A chip's own name, set in the face it offers. Sized down from body text so the
 * longest name still fits a third of the row in the widest face on offer.
 */
private fun fontChipLabel(font: UserFontChoice) = TextStyle(
    fontFamily = font.family,
    fontWeight = font.weight,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

/**
 * Previews how the widgets read on a phone, using the user's own data rather than
 * sample copy — the point is to show what they would actually see.
 *
 * The vision shown is the main one, the same one Today follows, since that's the one
 * the widgets are bound to. Picking a face only redraws the preview; it takes applying
 * to change what the rest of the app and the widgets use.
 */
@Composable
fun StudioScreen(
    vision: Vision? = null,
    topThreeTasks: List<String?> = List(3) { null },
    topThreeChecked: List<Boolean> = List(3) { false },
    wisdomIndex: Int = 0,
    /** The look currently applied to the widgets — what the preview starts from. */
    widgetFontId: Int = UserFonts.DEFAULT_ID,
    widgetThemeId: Int = CardThemes.DEFAULT_ID,
    onApplyStyle: (fontId: Int, themeId: Int) -> Unit = { _, _ -> },
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    // Keyed on what's applied, so committing a choice settles the drafts back onto it
    // and the button falls to its applied state without a second signal.
    var draftFontId by rememberSaveable(widgetFontId) { mutableIntStateOf(widgetFontId) }
    var draftThemeId by rememberSaveable(widgetThemeId) { mutableIntStateOf(widgetThemeId) }
    val draftFont = UserFonts[draftFontId]
    val draftTheme = CardThemes[draftThemeId]

    // One control commits both, so it reads as applied only when neither has moved.
    val isApplied = draftFontId == widgetFontId && draftThemeId == widgetThemeId

    // The screen's own text is not a widget, so it keeps the default face however the
    // widgets are set — only the phone preview follows the draft.
    val userFont = UserFonts[UserFonts.DEFAULT_ID]

    var tab by rememberSaveable { mutableStateOf(StudioTab.Design) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlanChip(label = "FREE PLAN")
                    Spacer(Modifier.width(10.dp))
                    SettingsButton(onClick = { showSettings = true })
                }
            }
            Spacer(Modifier.height(14.dp))
            // Held to the content column like everything else on the screen, so the
            // rules stop at the same margins rather than running to the screen edges.
            TabBar(
                selected = tab,
                userFont = userFont,
                onSelect = { tab = it }
            )
        }

        when (tab) {
            StudioTab.Design -> DesignTab(
                vision = vision,
                topThreeTasks = topThreeTasks,
                topThreeChecked = topThreeChecked,
                wisdomIndex = wisdomIndex,
                draftFontId = draftFontId,
                draftThemeId = draftThemeId,
                draftFont = draftFont,
                draftTheme = draftTheme,
                userFont = userFont,
                isApplied = isApplied,
                onSelectFont = { draftFontId = it },
                onSelectTheme = { draftThemeId = it },
                onApplyFont = { onApplyStyle(draftFontId, draftThemeId) },
                contentPadding = contentPadding
            )

            StudioTab.Gallery -> GalleryTab(
                userFont = userFont,
                contentPadding = contentPadding
            )
        }
    }

    if (showSettings) {
        SettingsSheet(userFont = userFont, onDismiss = { showSettings = false })
    }
}

/** The preview and the face picker — everything Studio can change today. */
@Composable
private fun ColumnScope.DesignTab(
    vision: Vision?,
    topThreeTasks: List<String?>,
    topThreeChecked: List<Boolean>,
    wisdomIndex: Int,
    draftFontId: Int,
    draftThemeId: Int,
    draftFont: UserFontChoice,
    draftTheme: CardTheme,
    userFont: UserFontChoice,
    isApplied: Boolean,
    onSelectFont: (Int) -> Unit,
    onSelectTheme: (Int) -> Unit,
    onApplyFont: () -> Unit,
    contentPadding: PaddingValues
) {
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
            vision = vision,
            topThreeTasks = topThreeTasks,
            topThreeChecked = topThreeChecked,
            wisdom = WISDOM[wisdomIndex.coerceIn(WISDOM.indices)],
            // One colour across all three: Studio applies to every widget at once.
            visionTheme = draftTheme,
            topThreeTheme = draftTheme,
            wisdomTheme = draftTheme,
            userFont = draftFont,
            isApplied = isApplied,
            chromeFont = userFont,
            onApply = onApplyFont
        )
    }

    Column(Modifier.fillMaxWidth(ContentWidthFraction)) {
        Spacer(Modifier.height(26.dp))
        Text(text = "TYPOGRAPHY", style = VisionType.eyebrow, color = OnCanvas)
        Spacer(Modifier.height(14.dp))
        FontPicker(selectedId = draftFontId, onSelect = onSelectFont)

        Spacer(Modifier.height(30.dp))
        Text(text = "THEME", style = VisionType.eyebrow, color = OnCanvas)
        Spacer(Modifier.height(14.dp))
        ThemePicker(selectedId = draftThemeId, onSelect = onSelectTheme)

        // The same action as the one on the preview, repeated at the foot of the
        // list: by the time the pickers have been scrolled through, the control up
        // inside the phone is long out of reach.
        Spacer(Modifier.height(28.dp))
        ApplyButton(isApplied = isApplied, userFont = userFont, onApply = onApplyFont)
        Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
    }
}

/** Nothing here yet — the tab exists so the split is in place for the work to land in. */
@Composable
private fun ColumnScope.GalleryTab(userFont: UserFontChoice, contentPadding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxWidth(ContentWidthFraction),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        Text(
            text = "Nothing here yet.",
            style = VisionType.bodyText(userFont),
            color = OnCanvasMuted
        )
        Spacer(Modifier.height(64.dp))
        Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
    }
}

/** Two halves of the screen, the selected one carrying a heavier rule beneath it. */
@Composable
private fun TabBar(
    selected: StudioTab,
    userFont: UserFontChoice,
    onSelect: (StudioTab) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        StudioTab.entries.forEach { entry ->
            val isSelected = entry == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(entry) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = entry.label,
                    style = tabLabel(userFont),
                    color = if (isSelected) OnCanvas else OnCanvasMuted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(if (isSelected) 2.dp else 1.dp)
                        .background(if (isSelected) OnCanvas else Rule)
                )
            }
        }
    }
}

/** The gear beside the plan chip. Opens settings; what's in them comes later. */
@Composable
private fun SettingsButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(IconButtonFill)
            .border(1.dp, IconButtonBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_settings),
            contentDescription = "Settings",
            tint = OnCanvas,
            modifier = Modifier.size(17.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(userFont: UserFontChoice, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Canvas,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(text = "SETTINGS", style = VisionType.eyebrow, color = OnCanvasMuted)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Nothing here yet.",
                style = VisionType.screenPromptTitle(userFont),
                color = OnCanvas
            )
        }
    }
}

/**
 * Commits the previewed face.
 *
 * The same control appears twice — once on the wallpaper under the widgets, once at the
 * foot of the picker — so its colours are fixed rather than read from whatever sits
 * behind it, and the two can't drift apart. It holds an applied state rather than
 * disappearing, so the control stays where the eye last left it.
 */
@Composable
private fun ApplyButton(isApplied: Boolean, userFont: UserFontChoice, onApply: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (isApplied) AppliedFill else NavBar)
            .then(
                if (isApplied) Modifier.border(1.dp, AppliedBorder, CircleShape) else Modifier
            )
            .clickable(enabled = !isApplied, onClick = onApply)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isApplied) "Applied" else ApplyLabelText,
            style = applyLabel(userFont),
            color = if (isApplied) AppliedInk else OnNavBar
        )
    }
}

/**
 * Every face, each chip set in the face it offers — the name alone says nothing about
 * how a typeface reads, so the chip has to be the specimen.
 */
@Composable
private fun FontPicker(selectedId: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // A fixed three across rather than flowing: every chip is set in a different
        // face, so their natural widths vary enough that a flow row packs two here and
        // three there. Equal columns keep the grid reading as a grid.
        UserFonts.all.chunked(FontColumns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { choice ->
                    FontChip(
                        choice = choice,
                        isSelected = choice.id == selectedId,
                        onClick = { onSelect(choice.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // A short last row leaves its columns empty rather than stretching the
                // chips that are in it.
                repeat(FontColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Every colour as its own swatch, free tier first. The name sits under the square
 * rather than inside it — several of the surfaces are too pale to carry text.
 */
@Composable
private fun ThemePicker(selectedId: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CardThemes.all.chunked(ThemeColumns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { theme ->
                    ThemeSwatch(
                        theme = theme,
                        isSelected = theme.id == selectedId,
                        onClick = { onSelect(theme.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(ThemeColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: CardTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // The ring sits outside the colour with a gap, so a dark swatch doesn't
                // swallow it and a pale one doesn't look merely outlined.
                .then(
                    if (isSelected) {
                        Modifier.border(1.dp, OnCanvas, RoundedCornerShape(13.dp))
                    } else {
                        Modifier
                    }
                )
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(SwatchShape)
                    .background(theme.surface)
                    // Every swatch is outlined, not just the pale ones: in a grid the
                    // squares need a common edge to read as one set.
                    .border(1.dp, theme.border ?: theme.onSurfaceRule, SwatchShape)
            ) {
                if (theme.id > CardThemes.LAST_FREE_ID) {
                    Text(
                        text = "+",
                        style = SwatchPlus,
                        // Taken from the colour's own text tone, so it stays legible on
                        // a pale butter and on a near-black cacao alike.
                        color = theme.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = theme.name.uppercase(),
            style = SwatchLabel,
            color = if (isSelected) OnCanvas else OnCanvasMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FontChip(
    choice: UserFontChoice,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = if (isSelected) OnNavBar else OnCanvas
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isSelected) NavBar else Canvas)
            .then(if (isSelected) Modifier else Modifier.border(1.dp, Rule, CircleShape))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildAnnotatedString {
                append(choice.name)
                if (choice.id > UserFonts.LAST_FREE_ID) {
                    // Part of the same run so the mark can't be pushed to its own line
                    // when a wide face fills the column.
                    withStyle(SpanStyle(color = if (isSelected) ink.copy(alpha = 0.8f) else PlusMark)) {
                        append(" +")
                    }
                }
            },
            style = fontChipLabel(choice),
            color = ink,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
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
    vision: Vision?,
    topThreeTasks: List<String?>,
    topThreeChecked: List<Boolean>,
    wisdom: Wisdom,
    visionTheme: CardTheme,
    topThreeTheme: CardTheme,
    wisdomTheme: CardTheme,
    userFont: UserFontChoice,
    isApplied: Boolean,
    chromeFont: UserFontChoice,
    onApply: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(PhoneWidth)
            .heightIn(min = PhoneMinHeight)
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
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                top = 38.dp,
                end = 16.dp,
                bottom = PhoneFooterArea
            ),
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
        }

        // Pinned to the bottom edge rather than following the widgets, the way a dock
        // sits on a real home screen however many widgets are stacked above it. The
        // apply control sits under the dock, inside the frame it acts on.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIconRow()
            Spacer(Modifier.height(16.dp))
            ApplyButton(isApplied = isApplied, userFont = chromeFont, onApply = onApply)
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
        // Each side takes half the row and wraps within it. Left to SpaceBetween the
        // two run together once the date and the countdown are both long enough to
        // fill the width between them.
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = vision?.let { formatTargetDate(it.targetDateMillis).uppercase() }.orEmpty(),
                style = WidgetMeta,
                color = theme.onSurfaceMuted,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = vision?.let { formatWeeksLeft(it.targetDateMillis) }.orEmpty(),
                style = WidgetMeta,
                color = theme.onSurfaceMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
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
private fun AppIconRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 2.dp),
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

