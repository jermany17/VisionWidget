package com.example.visionwidget.ui.studio

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.visionwidget.R
import com.example.visionwidget.ui.ContentWidthFraction
import com.example.visionwidget.ui.home.DEFAULT_WISDOM_THEME
import com.example.visionwidget.ui.components.croppedPhotoBackground
import com.example.visionwidget.ui.components.rememberWidgetPhoto
import com.example.visionwidget.ui.home.WISDOM
import com.example.visionwidget.ui.home.WISDOM_THEMES
import com.example.visionwidget.ui.home.Wisdom
import com.example.visionwidget.ui.home.wisdomIndices
import com.example.visionwidget.ui.theme.AlignChoice
import com.example.visionwidget.ui.theme.Alignments
import com.example.visionwidget.ui.theme.BackgroundStyles
import com.example.visionwidget.ui.theme.Canvas
import com.example.visionwidget.ui.theme.CardTheme
import com.example.visionwidget.ui.theme.CardThemes
import com.example.visionwidget.ui.theme.CornerRadii
import com.example.visionwidget.ui.theme.DMMono
import com.example.visionwidget.ui.theme.InstrumentSerif
import com.example.visionwidget.ui.theme.ThemePreset
import com.example.visionwidget.ui.theme.ThemePresets
import com.example.visionwidget.ui.theme.NavBar
import com.example.visionwidget.ui.theme.OnCanvas
import com.example.visionwidget.ui.theme.OnCanvasMuted
import com.example.visionwidget.ui.theme.OnNavBar
import com.example.visionwidget.ui.theme.ResolvedLook
import com.example.visionwidget.ui.theme.Rule
import com.example.visionwidget.ui.theme.resolve
import com.example.visionwidget.ui.theme.UserFontChoice
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.VisionType
import com.example.visionwidget.ui.theme.WidgetLook
import com.example.visionwidget.ui.theme.WidgetSkin
import com.example.visionwidget.ui.theme.WidgetStyle
import com.example.visionwidget.ui.theme.WidgetTarget
import com.example.visionwidget.ui.vision.Vision
import com.example.visionwidget.ui.vision.formatTargetDate
import com.example.visionwidget.ui.vision.formatWeeksLeft
import com.example.visionwidget.widget.BLUSH_DESIGN_HEIGHT
import com.example.visionwidget.widget.BLUSH_DESIGN_WIDTH
import com.example.visionwidget.widget.BlushWidgetReceiver
import com.example.visionwidget.widget.ClockWidgetReceiver
import com.example.visionwidget.widget.TopThreeWidgetReceiver
import com.example.visionwidget.widget.VisionCardWidgetReceiver
import com.example.visionwidget.widget.WisdomWidgetReceiver
import com.example.visionwidget.widget.renderBlushCalendar
import com.example.visionwidget.widget.QuoteWidgetReceiver
import com.example.visionwidget.widget.requestPinWidget

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

/**
 * Which widgets the choices below are being made for.
 *
 * All is a way of writing rather than a look of its own — it commits one arrangement to
 * every widget at once, which is why it has no picture: a photo belongs to the single
 * card it fills, so it's offered only once a widget has been singled out.
 */
private enum class StyleScope(val label: String, val targets: Set<WidgetTarget>) {
    All("All", WidgetTarget.entries.toSet()),
    Vision("Vision", setOf(WidgetTarget.Vision)),
    TopThree("Top 3", setOf(WidgetTarget.TopThree)),
    Wisdom("Wisdom", setOf(WidgetTarget.Wisdom));

    /** The one widget being styled, or null while every widget is. */
    val single: WidgetTarget? get() = targets.singleOrNull()
}

private fun tabLabel(font: UserFontChoice) = TextStyle(
    fontFamily = font.family,
    fontWeight = font.weight,
    fontSize = 19.sp,
    lineHeight = 24.sp
)

/**
 * The scope bar's own label. Smaller than [tabLabel]: four across where the tabs are
 * two, so "Wisdom" has to sit in a quarter of the width rather than a half.
 */
private fun scopeLabel(font: UserFontChoice) = TextStyle(
    fontFamily = font.family,
    fontWeight = font.weight,
    fontSize = 14.sp,
    lineHeight = 18.sp
)

/** How many faces sit across the picker. */
private const val FontColumns = 3

// Wider than tall, so a card reads as a widget's proportions rather than as a tile.
private val PresetCardWidth = 104.dp
private val PresetCardHeight = 82.dp
private val PresetCardShape = RoundedCornerShape(18.dp)

// The shelf's own scrollbar — a light groove with a darker grip, and arrows at the ends.
private val ScrollTrack = Color(0xFFE8E6E1)
private val ScrollThumb = Color(0xFF9B9892)

private val ScrollArrow = TextStyle(
    fontFamily = DMMono,
    fontWeight = FontWeight.Normal,
    fontSize = 9.sp,
    lineHeight = 12.sp
)

/** The "Aa" on a set's card, shown in that set's own face. */
private fun presetSpecimen(font: UserFontChoice) = TextStyle(
    fontFamily = font.family,
    fontWeight = font.weight,
    fontSize = 26.sp,
    lineHeight = 30.sp
)

/** A set's name. Fixed to the app's own face — it labels the card, it isn't part of it. */
private fun presetName() = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

/** Two faces across, with room between them for the shelf to read as a grid. */
private val GalleryGutter = 14.dp

/** A face's name. The app's own serif, as the labels on the Design shelf are. */
private fun galleryName() = TextStyle(
    fontFamily = InstrumentSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 19.sp
)

/** What the face is, under its name — size and grid, set like every other eyebrow. */
private val GalleryMeta = TextStyle(
    fontFamily = DMMono,
    fontWeight = FontWeight.Normal,
    fontSize = 8.sp,
    lineHeight = 12.sp,
    letterSpacing = 1.2.sp
)

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
    /** The look applied to each widget — what the preview starts from. */
    looks: Map<WidgetTarget, WidgetLook> = WidgetTarget.entries.associateWith { WidgetLook() },
    onApplyStyle: (targets: Set<WidgetTarget>, style: WidgetStyle) -> Unit = { _, _ -> },
    onPickPhoto: (target: WidgetTarget, uri: String?) -> Unit = { _, _ -> },
    /** The theme the daily line is drawn from. */
    wisdomCategory: String = DEFAULT_WISDOM_THEME,
    onSelectWisdomCategory: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPinUnsupported by rememberSaveable { mutableStateOf(false) }

    // The launcher does the asking and the placing; all this can do is start it, and
    // say so on the rare launcher that won't be asked.
    val pin = { receiver: Class<*> ->
        if (!requestPinWidget(context, receiver)) showPinUnsupported = true
    }

    var scope by rememberSaveable { mutableStateOf(StyleScope.All) }
    val lookOf = { target: WidgetTarget -> looks[target] ?: WidgetLook() }

    // Only a single widget can carry a picture: under All the same photo behind all
    // three isn't a look anyone asked for, so the style is offered without it.
    val allowsPhoto = scope.single != null
    val scopePhotoUri = scope.single?.let { lookOf(it).photoUri }

    // What the controls settle onto. Keyed on the applied look so committing a choice
    // drops the drafts back onto it and the button falls to its applied state without a
    // second signal — and on the scope, so moving between widgets picks up that widget's
    // own arrangement rather than carrying the last one across.
    val seed = remember(scope, looks) {
        val applied = lookOf(scope.targets.first()).style
        if (!allowsPhoto && applied.backgroundId == BackgroundStyles.PHOTO) {
            applied.copy(backgroundId = BackgroundStyles.DEFAULT_ID)
        } else {
            applied
        }
    }

    // Reset on the scope as well as on the seed, not on the seed alone: All borrows
    // Vision's style, so the two scopes can seed identically and an uncommitted change
    // made under Vision would otherwise ride across into All and dress all three.
    var draftFontId by rememberSaveable(scope, seed) { mutableIntStateOf(seed.fontId) }
    var draftThemeId by rememberSaveable(scope, seed) { mutableIntStateOf(seed.themeId) }
    var draftAlignId by rememberSaveable(scope, seed) { mutableIntStateOf(seed.alignId) }
    var draftBackgroundId by rememberSaveable(scope, seed) { mutableIntStateOf(seed.backgroundId) }
    var draftCornerRadius by rememberSaveable(scope, seed) { mutableIntStateOf(seed.cornerRadius) }

    /**
     * Whether a control has been used since the scope was picked.
     *
     * The drafts have to hold one arrangement, but under All the three widgets may
     * already be wearing three different ones — so until something is actually chosen
     * the drafts stand for nothing, and the preview shows each widget as it really is
     * rather than flattening all three onto an arrangement nobody asked for.
     */
    var touched by rememberSaveable(scope, seed) { mutableStateOf(false) }

    // Keyed on the applied theme alone, not on the scope: the daily line belongs to the
    // app rather than to a widget, so moving between widgets leaves a pending choice
    // standing instead of quietly dropping it.
    var draftWisdomCategory by rememberSaveable(wisdomCategory) { mutableStateOf(wisdomCategory) }

    val draft = WidgetStyle(
        fontId = draftFontId,
        themeId = draftThemeId,
        alignId = draftAlignId,
        backgroundId = draftBackgroundId,
        cornerRadius = draftCornerRadius
    )

    // Derived, never stored: a set is only a description of the five values below it, so
    // building one by hand marks it exactly as picking it would.
    val matchedPreset = ThemePresets.matching(
        draftFontId,
        draftThemeId,
        draftAlignId,
        draftBackgroundId,
        draftCornerRadius
    )

    // What the phone draws. Once a choice is made every widget in scope follows it —
    // that's what applying to a scope means — but before then each keeps its own.
    val previewLooks = scope.targets.associateWith { target ->
        if (touched) {
            WidgetLook(draft, scopePhotoUri).resolve()
        } else {
            lookOf(target).resolve()
        }
    }

    // The quote the phone shows follows the drafted theme, so picking one shows the line
    // it would actually put on the card rather than holding the old one until applying.
    val previewWisdom = remember(draftWisdomCategory, wisdomIndex) {
        val pool = wisdomIndices(draftWisdomCategory)
        WISDOM[if (wisdomIndex in pool) wisdomIndex else pool.first()]
    }

    // One control commits all five to every widget in scope, and the daily theme with
    // them. With nothing chosen there's nothing pending, however far apart the three
    // happen to be.
    val isApplied = (!touched || scope.targets.all { lookOf(it).style == draft }) &&
        draftWisdomCategory == wisdomCategory

    // The system picker hands back a URI that stays readable across restarts only if
    // the grant is taken persistently.
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = scope.single
        if (uri != null && target != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onPickPhoto(target, uri.toString())
        }
    }

    // The screen's own text is not a widget, so it keeps the default face however the
    // widgets are set — only the phone preview follows the draft.
    val userFont = UserFonts[UserFonts.DEFAULT_ID]

    var tab by rememberSaveable { mutableStateOf(StudioTab.Design) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showMissingPhoto by rememberSaveable { mutableStateOf(false) }

    // Photo has nothing to show without a picture, so applying it would blank the
    // widget. The choice is kept as it is and the alert says what's missing.
    val applyStyle = {
        if (draftBackgroundId == BackgroundStyles.PHOTO && scopePhotoUri == null) {
            showMissingPhoto = true
        } else {
            // Only when a control was used: the control can now be live for a theme
            // change alone, and under All an untouched draft stands for nothing — writing
            // it would flatten three different looks onto one nobody picked.
            if (touched) onApplyStyle(scope.targets, draft)
            if (draftWisdomCategory != wisdomCategory) {
                onSelectWisdomCategory(draftWisdomCategory)
            }
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
                wisdom = previewWisdom,
                scope = scope,
                onSelectScope = { scope = it },
                allowsPhoto = allowsPhoto,
                draftFontId = draftFontId,
                draftThemeId = draftThemeId,
                draftAlignId = draftAlignId,
                draftBackgroundId = draftBackgroundId,
                draftCornerRadius = draftCornerRadius,
                previewLooks = previewLooks,
                matchedPresetId = matchedPreset?.id,
                photoUri = scopePhotoUri,
                userFont = userFont,
                isApplied = isApplied,
                onSelectPreset = { preset ->
                    draftFontId = preset.fontId
                    draftThemeId = preset.themeId
                    draftAlignId = preset.alignId
                    draftBackgroundId = preset.backgroundId
                    draftCornerRadius = preset.cornerRadius
                    touched = true
                },
                onSelectFont = { draftFontId = it; touched = true },
                onSelectTheme = { draftThemeId = it; touched = true },
                onSelectAlign = { draftAlignId = it; touched = true },
                onSelectBackground = { draftBackgroundId = it; touched = true },
                onCornerRadiusChange = { draftCornerRadius = it; touched = true },
                onPickPhoto = {
                    photoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onClearPhoto = { scope.single?.let { onPickPhoto(it, null) } },
                onApplyFont = applyStyle,
                wisdomCategory = draftWisdomCategory,
                // Normalised here so a drafted theme and an applied one are the same
                // string, and the two can be compared for whether anything is pending.
                onSelectWisdomCategory = { draftWisdomCategory = it.lowercase() },
                onAddWidget = pin,
                contentPadding = contentPadding
            )

            StudioTab.Gallery -> GalleryTab(pin = pin, contentPadding = contentPadding)
        }
    }

    if (showSettings) {
        SettingsSheet(userFont = userFont, onDismiss = { showSettings = false })
    }

    if (showMissingPhoto) {
        MissingPhotoAlert(userFont = userFont, onDismiss = { showMissingPhoto = false })
    }

    if (showPinUnsupported) {
        PinUnsupportedAlert(onDismiss = { showPinUnsupported = false })
    }
}

/** The preview and the face picker — everything Studio can change today. */
@Composable
private fun ColumnScope.DesignTab(
    vision: Vision?,
    topThreeTasks: List<String?>,
    topThreeChecked: List<Boolean>,
    wisdom: Wisdom,
    scope: StyleScope,
    onSelectScope: (StyleScope) -> Unit,
    allowsPhoto: Boolean,
    draftFontId: Int,
    draftThemeId: Int,
    draftAlignId: Int,
    draftBackgroundId: Int,
    draftCornerRadius: Int,
    /** What the phone draws, per widget — the keys are the widgets in scope. */
    previewLooks: Map<WidgetTarget, ResolvedLook>,
    matchedPresetId: Int?,
    photoUri: String?,
    userFont: UserFontChoice,
    isApplied: Boolean,
    onSelectPreset: (ThemePreset) -> Unit,
    onSelectFont: (Int) -> Unit,
    onSelectTheme: (Int) -> Unit,
    onSelectAlign: (Int) -> Unit,
    onSelectBackground: (Int) -> Unit,
    onCornerRadiusChange: (Int) -> Unit,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    onApplyFont: () -> Unit,
    wisdomCategory: String,
    onSelectWisdomCategory: (String) -> Unit,
    onAddWidget: (Class<*>) -> Unit,
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
            wisdom = wisdom,
            // Only what's being styled: singling out a widget is a request to look at
            // that widget, and the other two would only be there to be ignored.
            looks = previewLooks,
            isApplied = isApplied,
            chromeFont = userFont,
            onApply = onApplyFont
        )
    }

    Column(Modifier.fillMaxWidth(ContentWidthFraction)) {
        Spacer(Modifier.height(10.dp))
        ScopeBar(selected = scope, userFont = userFont, onSelect = onSelectScope)
    }

    Spacer(Modifier.height(26.dp))
    // Full width so the row can run past the content margins as it scrolls, the way a
    // shelf of cards should.
    Text(
        text = "COLLECTIONS",
        style = VisionType.eyebrow,
        color = OnCanvas,
        modifier = Modifier.fillMaxWidth(ContentWidthFraction)
    )
    Spacer(Modifier.height(14.dp))
    CollectionsRow(
        selectedId = matchedPresetId,
        allowsPhoto = allowsPhoto,
        onSelect = onSelectPreset
    )

    Column(Modifier.fillMaxWidth(ContentWidthFraction)) {
        Spacer(Modifier.height(30.dp))
        Text(text = "LAYOUT", style = VisionType.eyebrow, color = OnCanvas)
        Spacer(Modifier.height(14.dp))
        LayoutPicker(selectedId = draftAlignId, onSelect = onSelectAlign)

        Spacer(Modifier.height(30.dp))
        Text(text = "BACKGROUND", style = VisionType.eyebrow, color = OnCanvas)
        Spacer(Modifier.height(14.dp))
        BackgroundPicker(
            selectedId = draftBackgroundId,
            allowsPhoto = allowsPhoto,
            onSelect = onSelectBackground
        )
        if (draftBackgroundId == BackgroundStyles.PHOTO) {
            Spacer(Modifier.height(12.dp))
            PhotoRow(
                hasPhoto = photoUri != null,
                userFont = userFont,
                onPick = onPickPhoto,
                onClear = onClearPhoto
            )
        }

        Spacer(Modifier.height(26.dp))
        CornerRadiusRow(radius = draftCornerRadius, onChange = onCornerRadiusChange)

        Spacer(Modifier.height(24.dp))
        Text(text = "TYPOGRAPHY", style = VisionType.eyebrow, color = OnCanvas)
        Spacer(Modifier.height(14.dp))
        FontPicker(selectedId = draftFontId, onSelect = onSelectFont)

        Spacer(Modifier.height(30.dp))
        Text(text = "THEME", style = VisionType.eyebrow, color = OnCanvas)
        Spacer(Modifier.height(14.dp))
        ThemePicker(selectedId = draftThemeId, onSelect = onSelectTheme)

        Spacer(Modifier.height(30.dp))
        Text(text = "DAILY WISDOM", style = VisionType.eyebrow, color = OnCanvas)
        Spacer(Modifier.height(14.dp))
        WisdomThemePicker(selected = wisdomCategory, onSelect = onSelectWisdomCategory)

        // The same action as the one on the preview, repeated at the foot of the
        // list: by the time the pickers have been scrolled through, the control up
        // inside the phone is long out of reach. It stays last, under everything it
        // commits.
        Spacer(Modifier.height(28.dp))
        ApplyButton(isApplied = isApplied, userFont = userFont, onApply = onApplyFont)

        // Applying dresses a card; this puts one up. Offered per widget rather than
        // per scope, since a widget is what the launcher takes.
        Spacer(Modifier.height(30.dp))
        Text(text = "ADD TO YOUR SCREEN", style = VisionType.eyebrow, color = OnCanvas)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AddWidgetChip("Vision", Modifier.weight(1f)) { onAddWidget(VisionCardWidgetReceiver::class.java) }
            AddWidgetChip("Top 3", Modifier.weight(1f)) { onAddWidget(TopThreeWidgetReceiver::class.java) }
            AddWidgetChip("Wisdom", Modifier.weight(1f)) { onAddWidget(WisdomWidgetReceiver::class.java) }
        }
        Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
    }
}

/**
 * The theme the daily line is drawn from. One choice for the app, not one per widget,
 * and committed with the rest by the apply control below it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WisdomThemePicker(selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WISDOM_THEMES.forEach { theme ->
            // Stored lowercase, offered in title case, so the two are compared loosely.
            val isSelected = theme.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) NavBar else Canvas)
                    .then(
                        if (isSelected) Modifier else Modifier.border(1.dp, Rule, CircleShape)
                    )
                    .clickable { onSelect(theme) }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = theme,
                    style = fontChipLabel(UserFonts[UserFonts.DEFAULT_ID]),
                    color = if (isSelected) OnNavBar else OnCanvas
                )
            }
        }
    }
}

/**
 * Faces that stand on their own.
 *
 * Nothing here is bound to the user's vision or tasks — these only ever show the date,
 * so they're shown as finished things rather than as something to dress. Two for now,
 * while the shelf is being built out.
 */
@Composable
private fun ColumnScope.GalleryTab(
    pin: (Class<*>) -> Unit,
    contentPadding: PaddingValues
) {
    Column(Modifier.fillMaxWidth(ContentWidthFraction)) {
        Spacer(Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(GalleryGutter)) {
            GalleryItem(
                name = "Clock",
                meta = "LARGE · 4 × 4",
                onAdd = { pin(ClockWidgetReceiver::class.java) },
                modifier = Modifier.weight(1f)
            ) { size -> ClockWidget(size) }

            GalleryItem(
                name = "Quote",
                meta = "LARGE · 4 × 4",
                onAdd = { pin(QuoteWidgetReceiver::class.java) },
                modifier = Modifier.weight(1f)
            ) { size -> QuoteWidget(size) }
        }

        // Wide, so it takes a row of its own rather than half of one.
        Spacer(Modifier.height(24.dp))
        GalleryItem(
            name = "Calendar",
            meta = "MEDIUM · 4 × 2",
            onAdd = { pin(BlushWidgetReceiver::class.java) },
            aspect = BLUSH_DESIGN_WIDTH / BLUSH_DESIGN_HEIGHT,
            modifier = Modifier.fillMaxWidth()
        ) { size -> BlushCalendarFace(size) }
        Spacer(Modifier.height(30.dp))
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

/**
 * One face on the shelf: the widget itself at whatever width the column gives it, with
 * its name and what it is underneath.
 *
 * The face is handed its own measured width rather than a fixed size, since every
 * measurement inside it is a fraction of that — a thumbnail and a full-size preview are
 * then the same drawing at two scales.
 */
@Composable
private fun GalleryItem(
    name: String,
    meta: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    /** Width over height, as the face was drawn. */
    aspect: Float = 1f,
    face: @Composable (size: Dp) -> Unit
) {
    Column(modifier) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(aspect)) {
            face(maxWidth)
        }
        Spacer(Modifier.height(10.dp))
        Text(text = name, style = galleryName(), color = OnCanvas)
        Spacer(Modifier.height(3.dp))
        Text(text = meta, style = GalleryMeta, color = OnCanvasMuted)

        // Puts the face up without sending anyone off to hunt through the launcher's
        // own picker for it. Nothing is said about whether one is already up: the same
        // face can sit in as many places as the user likes, so that would answer a
        // question nobody is asking. Nor is a destination named — the launcher decides
        // where it lands, and it needn't be the home screen.
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .border(1.dp, Rule, CircleShape)
                .clickable(onClick = onAdd)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add widget",
                style = fontChipLabel(UserFonts[UserFonts.DEFAULT_ID]),
                color = OnCanvas,
                maxLines = 1
            )
        }
    }
}


/** One widget's offer to be put up, narrow enough that three sit across a row. */
@Composable
private fun AddWidgetChip(label: String, modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, Rule, CircleShape)
            .clickable(onClick = onAdd)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = fontChipLabel(UserFonts[UserFonts.DEFAULT_ID]),
            color = OnCanvas,
            maxLines = 1
        )
    }
}

/** Says why nothing happened, on a launcher that won't take the request. */
@Composable
private fun PinUnsupportedAlert(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Canvas,
        title = {
            Text(text = "CAN'T ADD IT FROM HERE", style = VisionType.eyebrow, color = OnCanvas)
        },
        text = {
            Text(
                text = "This launcher won't take the request. Add the widget from its " +
                    "own picker instead — hold an empty spot on the home screen, " +
                    "choose Widgets, and look for Vision Widget.",
                style = VisionType.bodyText(UserFonts[UserFonts.DEFAULT_ID]),
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

/**
 * Which widgets the pickers below are dressing. Built like [TabBar] rather than as chips
 * so the two read as the same kind of control — one splits the screen, one splits what
 * the screen is acting on.
 */
@Composable
private fun ScopeBar(
    selected: StyleScope,
    userFont: UserFontChoice,
    onSelect: (StyleScope) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        StyleScope.entries.forEach { entry ->
            val isSelected = entry == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(entry) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = entry.label,
                    style = scopeLabel(userFont),
                    color = if (isSelected) OnCanvas else OnCanvasMuted,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 10.dp)
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

/**
 * The ready-made sets, as a shelf that scrolls sideways. Each card is its own specimen:
 * the set's colour behind the set's face, so the name is the least of what it says.
 */
@Composable
private fun CollectionsRow(
    selectedId: Int?,
    allowsPhoto: Boolean,
    onSelect: (ThemePreset) -> Unit
) {
    // A set built on Photo has nothing to stand on until a picture has been chosen for
    // one particular card, so it's held back until a widget has been singled out.
    val presets = remember(allowsPhoto) {
        ThemePresets.all.filter { allowsPhoto || it.backgroundId != BackgroundStyles.PHOTO }
    }
    val scroll = rememberScrollState()
    // The row is the viewport, so its own width is what the bar measures against.
    var viewportWidth by remember { mutableIntStateOf(0) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // Taken from the real width rather than as a fraction of the row: inside a
        // horizontal scroll the row has no bounded width to take a fraction of.
        val sideMargin = maxWidth * (1f - ContentWidthFraction) / 2f

        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { viewportWidth = it.width }
                    .horizontalScroll(scroll)
                    // Padding inside the scroll, so the first and last cards start and
                    // end on the content margins but can still scroll past them.
                    .padding(horizontal = sideMargin),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                presets.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        isSelected = preset.id == selectedId,
                        onClick = { onSelect(preset) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            ScrollIndicator(
                scroll = scroll,
                viewportWidth = viewportWidth,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(ContentWidthFraction)
            )
        }
    }
}

/**
 * Says how far along the shelf is and how much of it there is. Drawn rather than left
 * to the platform: a scrollbar that only appears while a finger is down says nothing
 * about a row that has more to show when it's sitting still.
 */
@Composable
private fun ScrollIndicator(
    scroll: ScrollState,
    viewportWidth: Int,
    modifier: Modifier = Modifier
) {
    val contentWidth = viewportWidth + scroll.maxValue
    // Nothing to indicate before the row has been measured, or when it all fits.
    if (viewportWidth == 0 || scroll.maxValue == 0) return

    val visibleShare = (viewportWidth.toFloat() / contentWidth).coerceIn(0.1f, 1f)
    val progress = (scroll.value.toFloat() / scroll.maxValue).coerceIn(0f, 1f)

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "◀", style = ScrollArrow, color = OnCanvasMuted)
        Spacer(Modifier.width(8.dp))
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                // A groove six dp tall is far too thin to hit, so the bar carries a
                // taller transparent box and draws the groove inside it.
                .height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val trackWidth = maxWidth
            val thumbWidth = trackWidth * visibleShare
            val travel = trackWidth - thumbWidth

            // Where a touch at [x] should put the row, reading the point as the middle
            // of the grip rather than its left edge.
            fun scrollFor(x: Float): Int {
                val travelPx = with(density) { travel.toPx() }
                if (travelPx <= 0f) return 0
                val thumbCentre = with(density) { (thumbWidth / 2).toPx() }
                val fraction = ((x - thumbCentre) / travelPx).coerceIn(0f, 1f)
                return (fraction * scroll.maxValue).roundToInt()
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(ScrollTrack)
                    .pointerInput(scroll.maxValue, trackWidth, thumbWidth) {
                        detectTapGestures { offset ->
                            scope.launch { scroll.scrollTo(scrollFor(offset.x)) }
                        }
                    }
                    .pointerInput(scroll.maxValue, trackWidth, thumbWidth) {
                        detectHorizontalDragGestures { change, _ ->
                            scope.launch { scroll.scrollTo(scrollFor(change.position.x)) }
                        }
                    }
            ) {
                Box(
                    Modifier
                        .offset(x = travel * progress)
                        .width(thumbWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(ScrollThumb)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(text = "▶", style = ScrollArrow, color = OnCanvasMuted)
    }
}

@Composable
private fun PresetCard(preset: ThemePreset, isSelected: Boolean, onClick: () -> Unit) {
    val theme = CardThemes[preset.themeId]
    val font = UserFonts[preset.fontId]
    // One shape for every card, not each set's own radius: a tight corner clips the
    // paid mark, and the shelf reads as a set of samples rather than a ragged row.
    val shape = PresetCardShape

    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .width(PresetCardWidth)
                .height(PresetCardHeight)
                .clip(shape)
                .background(theme.surface)
                .border(1.dp, theme.border ?: theme.onSurfaceRule, shape)
        ) {
            Text(
                text = "Aa",
                style = presetSpecimen(font),
                color = theme.onSurface,
                modifier = Modifier.align(Alignment.Center)
            )
            if (preset.isPlus) {
                Text(
                    text = "+",
                    style = SwatchPlus,
                    color = theme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(OnCanvas),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✓", style = SwatchPlus, color = Canvas)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            // Always the app's own face, never the set's: these are labels for the
            // shelf, not part of the specimen above them.
            text = preset.name,
            style = presetName(),
            color = OnCanvas
        )
    }
}

/** The one value Studio offers as a range rather than a set of choices. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CornerRadiusRow(radius: Int, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "CORNER RADIUS", style = VisionType.eyebrow, color = OnCanvas)
            Text(text = "${radius}px", style = VisionType.eyebrow, color = PlusMark)
        }
        Slider(
            value = radius.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = CornerRadii.MIN.toFloat()..CornerRadii.MAX.toFloat(),
            // One unbroken hairline rather than a filled portion: the figure to the
            // right already says where the value sits, so the track only has to show
            // the range it moves along.
            track = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(Rule)
                )
            },
            thumb = {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(OnCanvas)
                )
            }
        )
    }
}

/** The ways a card can be filled behind its words. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundPicker(selectedId: Int, allowsPhoto: Boolean, onSelect: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Photo fills one card with one picture, so it's offered only once the choices
        // are being made for a single widget rather than for all three at once.
        BackgroundStyles.all.filter { allowsPhoto || it.id != BackgroundStyles.PHOTO }.forEach { style ->
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
    /**
     * How to draw each widget. A widget missing from the map is out of scope and stays
     * off the phone; the three may well be dressed differently, so each brings its own.
     */
    looks: Map<WidgetTarget, ResolvedLook>,
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
            looks[WidgetTarget.Vision]?.let { look ->
                VisionWidget(
                    vision = vision,
                    skin = look.skin,
                    photoUri = look.photoUri,
                    userFont = look.font,
                    align = look.align,
                    shape = look.shape
                )
            }
            looks[WidgetTarget.TopThree]?.let { look ->
                TopThreeWidget(
                    tasks = topThreeTasks,
                    checked = topThreeChecked,
                    skin = look.skin,
                    photoUri = look.photoUri,
                    userFont = look.font,
                    align = look.align,
                    shape = look.shape
                )
            }
            looks[WidgetTarget.Wisdom]?.let { look ->
                WisdomWidget(
                    wisdom = wisdom,
                    skin = look.skin,
                    photoUri = look.photoUri,
                    userFont = look.font,
                    align = look.align,
                    shape = look.shape
                )
            }
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
    shape: RoundedCornerShape,
    content: @Composable ColumnScope.() -> Unit
) {
    // A chosen picture replaces the fill entirely; the scrim over it is what keeps the
    // words readable, so it's painted whether the picture loaded or not.
    val photo = rememberWidgetPhoto(photoUri.takeIf { skin.usesPhoto })
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (skin.castsShadow) {
                    Modifier.shadow(elevation = 10.dp, shape = shape, clip = false)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .then(
                if (photo != null) {
                    Modifier.croppedPhotoBackground(photo)
                } else {
                    Modifier.background(skin.background)
                }
            )
            .then(skin.overlay?.let { Modifier.background(it) } ?: Modifier)
            .then(skin.border?.let { Modifier.border(1.dp, it, shape) } ?: Modifier)
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
    align: AlignChoice,
    shape: RoundedCornerShape
) {
    WidgetCard(skin, photoUri, align, shape) {
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
    align: AlignChoice,
    shape: RoundedCornerShape
) {
    val set = tasks.indices.filter { tasks[it] != null }
    val done = set.count { checked[it] }

    WidgetCard(skin, photoUri, align, shape) {
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
    align: AlignChoice,
    shape: RoundedCornerShape
) {
    WidgetCard(skin, photoUri, align, shape) {
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

