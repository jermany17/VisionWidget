package com.example.visionwidget.ui.studio

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.visionwidget.ui.components.rememberWidgetPhoto
import com.example.visionwidget.ui.home.WISDOM
import com.example.visionwidget.ui.home.Wisdom
import com.example.visionwidget.ui.theme.AlignChoice
import com.example.visionwidget.ui.theme.Alignments
import com.example.visionwidget.ui.theme.BackgroundStyles
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
import com.example.visionwidget.ui.theme.WidgetSkin
import com.example.visionwidget.ui.theme.widgetSkin
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
    widgetAlignId: Int = Alignments.DEFAULT_ID,
    widgetBackgroundId: Int = BackgroundStyles.DEFAULT_ID,
    /** The picture behind the cards under Photo, or null before one is chosen. */
    widgetPhotoUri: String? = null,
    onApplyStyle: (fontId: Int, themeId: Int, alignId: Int, backgroundId: Int) -> Unit =
        { _, _, _, _ -> },
    onPickPhoto: (String?) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    // Keyed on what's applied, so committing a choice settles the drafts back onto it
    // and the button falls to its applied state without a second signal.
    var draftFontId by rememberSaveable(widgetFontId) { mutableIntStateOf(widgetFontId) }
    var draftThemeId by rememberSaveable(widgetThemeId) { mutableIntStateOf(widgetThemeId) }
    var draftAlignId by rememberSaveable(widgetAlignId) { mutableIntStateOf(widgetAlignId) }
    var draftBackgroundId by rememberSaveable(widgetBackgroundId) {
        mutableIntStateOf(widgetBackgroundId)
    }
    val draftFont = UserFonts[draftFontId]
    val draftTheme = CardThemes[draftThemeId]
    val draftAlign = Alignments[draftAlignId]

    val draftSkin = widgetSkin(draftTheme, BackgroundStyles[draftBackgroundId])

    // One control commits all four, so it reads as applied only when none has moved.
    val isApplied = draftFontId == widgetFontId &&
        draftThemeId == widgetThemeId &&
        draftAlignId == widgetAlignId &&
        draftBackgroundId == widgetBackgroundId

    // The system picker hands back a URI that stays readable across restarts only if
    // the grant is taken persistently.
    val context = LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onPickPhoto(uri.toString())
        }
    }

    // The screen's own text is not a widget, so it keeps the default face however the
    // widgets are set — only the phone preview follows the draft.
    val userFont = UserFonts[UserFonts.DEFAULT_ID]

    var tab by rememberSaveable { mutableStateOf(StudioTab.Design) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showMissingPhoto by rememberSaveable { mutableStateOf(false) }

    // Photo has nothing to show without a picture, so applying it would blank the
    // widgets. The choice is kept as it is and the alert says what's missing.
    val applyStyle = {
        if (draftBackgroundId == BackgroundStyles.PHOTO && widgetPhotoUri == null) {
            showMissingPhoto = true
        } else {
            onApplyStyle(draftFontId, draftThemeId, draftAlignId, draftBackgroundId)
        }
    }

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
                draftAlignId = draftAlignId,
                draftBackgroundId = draftBackgroundId,
                draftFont = draftFont,
                draftAlign = draftAlign,
                draftSkin = draftSkin,
                photoUri = widgetPhotoUri,
                userFont = userFont,
                isApplied = isApplied,
                onSelectFont = { draftFontId = it },
                onSelectTheme = { draftThemeId = it },
                onSelectAlign = { draftAlignId = it },
                onSelectBackground = { draftBackgroundId = it },
                onPickPhoto = {
                    photoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClearPhoto = { onPickPhoto(null) },
                onApplyFont = applyStyle,
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

    if (showMissingPhoto) {
        MissingPhotoAlert(userFont = userFont, onDismiss = { showMissingPhoto = false })
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
    draftAlignId: Int,
    draftBackgroundId: Int,
    draftFont: UserFontChoice,
    draftAlign: AlignChoice,
    draftSkin: WidgetSkin,
    photoUri: String?,
    userFont: UserFontChoice,
    isApplied: Boolean,
    onSelectFont: (Int) -> Unit,
    onSelectTheme: (Int) -> Unit,
    onSelectAlign: (Int) -> Unit,
    onSelectBackground: (Int) -> Unit,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
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
            // One surface across all three: Studio applies to every widget at once.
            skin = draftSkin,
            photoUri = photoUri,
            userFont = draftFont,
            align = draftAlign,
            isApplied = isApplied,
            chromeFont = userFont,
            onApply = onApplyFont
        )
    }

    Column(Modifier.fillMaxWidth(ContentWidthFraction)) {
        Spacer(Modifier.height(26.dp))
        Text(text = "LAYOUT", style = VisionType.eyebrow, color = OnCanvas)
        Spacer(Modifier.height(14.dp))
        LayoutPicker(selectedId = draftAlignId, onSelect = onSelectAlign)

        Spacer(Modifier.height(30.dp))
        Text(text = "BACKGROUND", style = VisionType.eyebrow, color = OnCanvas)
        Spacer(Modifier.height(14.dp))
        BackgroundPicker(selectedId = draftBackgroundId, onSelect = onSelectBackground)
        if (draftBackgroundId == BackgroundStyles.PHOTO) {
            Spacer(Modifier.height(12.dp))
            PhotoRow(
                hasPhoto = photoUri != null,
                userFont = userFont,
                onPick = onPickPhoto,
                onClear = onClearPhoto
            )
        }

        Spacer(Modifier.height(30.dp))
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

/** Says why applying did nothing, rather than letting Photo blank the widgets. */
@Composable
private fun MissingPhotoAlert(userFont: UserFontChoice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Canvas,
        title = { Text(text = "NO PHOTO ADDED", style = VisionType.eyebrow, color = OnCanvas) },
        text = {
            Text(
                text = "Add a photo before applying this background.",
                style = VisionType.bodyText(userFont),
                color = OnCanvasMuted
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "OK", style = VisionType.eyebrow, color = OnCanvas)
            }
        }
    )
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

/** The ways a card can be filled behind its words. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundPicker(selectedId: Int, onSelect: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BackgroundStyles.all.forEach { style ->
            val isSelected = style.id == selectedId
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) NavBar else Canvas)
                    .then(
                        if (isSelected) Modifier else Modifier.border(1.dp, Rule, CircleShape)
                    )
                    .clickable { onSelect(style.id) }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(style.name)
                        if (style.isPlus) {
                            withStyle(
                                SpanStyle(
                                    color = if (isSelected) {
                                        OnNavBar.copy(alpha = 0.8f)
                                    } else {
                                        PlusMark
                                    }
                                )
                            ) {
                                append(" +")
                            }
                        }
                    },
                    style = fontChipLabel(UserFonts[UserFonts.DEFAULT_ID]),
                    color = if (isSelected) OnNavBar else OnCanvas
                )
            }
        }
    }
}

/** Shown under the picker while Photo is chosen — the way a picture gets in or out. */
@Composable
private fun PhotoRow(
    hasPhoto: Boolean,
    userFont: UserFontChoice,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .border(1.dp, Rule, CircleShape)
                .clickable(onClick = onPick)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasPhoto) "Change photo" else "Add a photo",
                style = applyLabel(userFont),
                color = OnCanvas
            )
        }
        if (hasPhoto) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, Rule, CircleShape)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Remove", style = applyLabel(userFont), color = OnCanvasMuted)
            }
        }
    }
}

/**
 * The three ways a widget can set out its words. Chips take their natural width rather
 * than equal thirds — there are only three, and the names are short enough to read.
 */
@Composable
private fun LayoutPicker(selectedId: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Alignments.all.forEach { choice ->
            val isSelected = choice.id == selectedId
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) NavBar else Canvas)
                    .then(
                        if (isSelected) Modifier else Modifier.border(1.dp, Rule, CircleShape)
                    )
                    .clickable { onSelect(choice.id) }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = choice.name,
                    style = fontChipLabel(UserFonts[UserFonts.DEFAULT_ID]),
                    color = if (isSelected) OnNavBar else OnCanvas
                )
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
    skin: WidgetSkin,
    photoUri: String?,
    userFont: UserFontChoice,
    align: AlignChoice,
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
            VisionWidget(
                vision = vision,
                skin = skin,
                photoUri = photoUri,
                userFont = userFont,
                align = align
            )
            TopThreeWidget(
                tasks = topThreeTasks,
                checked = topThreeChecked,
                skin = skin,
                photoUri = photoUri,
                userFont = userFont,
                align = align
            )
            WisdomWidget(
                wisdom = wisdom,
                skin = skin,
                photoUri = photoUri,
                userFont = userFont,
                align = align
            )
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
private fun WidgetCard(
    skin: WidgetSkin,
    photoUri: String?,
    align: AlignChoice,
    content: @Composable ColumnScope.() -> Unit
) {
    // A chosen picture replaces the fill entirely; the scrim over it is what keeps the
    // words readable, so it's painted whether the picture loaded or not.
    val photo = rememberWidgetPhoto(photoUri)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (skin.castsShadow) {
                    Modifier.shadow(elevation = 10.dp, shape = WidgetShape, clip = false)
                } else {
                    Modifier
                }
            )
            .clip(WidgetShape)
            .then(
                if (photo != null) {
                    Modifier.paint(BitmapPainter(photo), contentScale = ContentScale.Crop)
                } else {
                    Modifier.background(skin.background)
                }
            )
            .then(skin.overlay?.let { Modifier.background(it) } ?: Modifier)
            .then(skin.border?.let { Modifier.border(1.dp, it, WidgetShape) } ?: Modifier)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        // Set on the column as well as on the text: a checked row is a row, and only
        // the column can move it off the left edge.
        horizontalAlignment = align.horizontal,
        content = content
    )
}

@Composable
private fun VisionWidget(
    vision: Vision?,
    skin: WidgetSkin,
    photoUri: String?,
    userFont: UserFontChoice,
    align: AlignChoice
) {
    WidgetCard(skin, photoUri, align) {
        Text(
            text = "VISION",
            style = WidgetEyebrow,
            color = skin.onSurfaceMuted,
            textAlign = align.textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = align.format(vision?.goal ?: "No vision yet"),
            style = widgetTitle(userFont),
            color = skin.onSurface,
            textAlign = align.textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        HorizontalDivider(color = skin.onSurfaceRule, thickness = 1.dp)

        val milestones = vision?.milestones.orEmpty()
        if (milestones.isEmpty()) {
            EmptyWidgetLine(skin = skin, userFont = userFont, align = align)
        } else {
            milestones.forEach { milestone ->
                WidgetTaskRow(
                    text = milestone.step,
                    checked = milestone.checked,
                    skin = skin,
                    userFont = userFont,
                    align = align
                )
            }
        }

        HorizontalDivider(color = skin.onSurfaceRule, thickness = 1.dp)
        // Each side takes half the row and wraps within it. Left to SpaceBetween the
        // two run together once the date and the countdown are both long enough to
        // fill the width between them.
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = vision?.let { formatTargetDate(it.targetDateMillis).uppercase() }.orEmpty(),
                style = WidgetMeta,
                color = skin.onSurfaceMuted,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = vision?.let { formatWeeksLeft(it.targetDateMillis) }.orEmpty(),
                style = WidgetMeta,
                color = skin.onSurfaceMuted,
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
    skin: WidgetSkin,
    photoUri: String?,
    userFont: UserFontChoice,
    align: AlignChoice
) {
    val set = tasks.indices.filter { tasks[it] != null }
    val done = set.count { checked[it] }

    WidgetCard(skin, photoUri, align) {
        Text(
            // Against the tasks that were set rather than a fixed three, matching the
            // count on Today's own card.
            text = "TODAY'S TOP 3 · $done / ${set.size}",
            style = WidgetEyebrow,
            color = skin.onSurfaceMuted,
            textAlign = align.textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        if (set.isEmpty()) {
            EmptyWidgetLine(skin = skin, userFont = userFont, align = align)
        } else {
            set.forEach { index ->
                WidgetTaskRow(
                    text = tasks[index].orEmpty(),
                    checked = checked[index],
                    skin = skin,
                    userFont = userFont,
                    align = align
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
    skin: WidgetSkin,
    userFont: UserFontChoice,
    align: AlignChoice
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(CheckSize)
                .clip(CircleShape)
                .then(
                    if (checked) Modifier.background(skin.onSurface)
                    else Modifier.border(1.5.dp, skin.onSurfaceMuted, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(text = "✓", style = WidgetCheckGlyph, color = skin.onInk)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = align.format(text),
            style = widgetLine(userFont).copy(
                textDecoration = if (checked) TextDecoration.LineThrough else null
            ),
            color = if (checked) skin.onSurfaceMuted else skin.onSurface
        )
    }
}

/** Shared wording, so an empty vision and an empty Top 3 read the same on the phone. */
@Composable
private fun EmptyWidgetLine(skin: WidgetSkin, userFont: UserFontChoice, align: AlignChoice) {
    Text(
        text = align.format("Nothing set yet."),
        style = widgetLine(userFont),
        color = skin.onSurfaceMuted,
        textAlign = align.textAlign,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun WisdomWidget(
    wisdom: Wisdom,
    skin: WidgetSkin,
    photoUri: String?,
    userFont: UserFontChoice,
    align: AlignChoice
) {
    WidgetCard(skin, photoUri, align) {
        Text(
            text = align.format(wisdom.text),
            style = widgetQuote(userFont),
            color = skin.onSurface,
            textAlign = align.textAlign,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = wisdom.category.uppercase(),
            style = WidgetMeta,
            color = skin.onSurfaceMuted,
            textAlign = align.textAlign,
            modifier = Modifier.fillMaxWidth()
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

