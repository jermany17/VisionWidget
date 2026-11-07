package app.visionov.visionwidget.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.visionov.visionwidget.R

/** Display — headlines, vision titles, quotes. */
val InstrumentSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal)
)

/** Interface — body, labels, buttons, inputs. Variable file, one axis per weight. */
val DMSans = FontFamily(
    Font(
        R.font.dm_sans_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(300))
    ),
    Font(
        R.font.dm_sans_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.dm_sans_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    )
)

/** Data — eyebrows, counters, dates, meta. */
val DMMono = FontFamily(
    Font(R.font.dm_mono_regular, FontWeight.Normal)
)

// --- User-selectable faces ---
//
// All variable files, so one resource covers every weight: the family below declares
// only the weight its choice in [UserFonts] actually renders at, and the axis is set to
// match. Declaring a weight the file isn't asked for would ship the same bytes twice.

private fun variable(resId: Int, weight: FontWeight, axis: Int) = FontFamily(
    Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(axis)))
)

val LibreBaskerville = variable(R.font.libre_baskerville_variable, FontWeight.Normal, 400)
val Newsreader = variable(R.font.newsreader_variable, FontWeight.Normal, 400)
val Lora = variable(R.font.lora_variable, FontWeight.Medium, 500)
val EBGaramond = variable(R.font.eb_garamond_variable, FontWeight.Medium, 500)
val CrimsonPro = variable(R.font.crimson_pro_variable, FontWeight.Normal, 400)
val PlayfairDisplay = variable(R.font.playfair_display_variable, FontWeight.Medium, 500)
val Fraunces = variable(R.font.fraunces_variable, FontWeight.SemiBold, 600)
val BricolageGrotesque = variable(R.font.bricolage_grotesque_variable, FontWeight.SemiBold, 600)
val Outfit = variable(R.font.outfit_variable, FontWeight.Medium, 500)
val WorkSans = variable(R.font.work_sans_variable, FontWeight.Medium, 500)
val PublicSans = variable(R.font.public_sans_variable, FontWeight.SemiBold, 600)
val Manrope = variable(R.font.manrope_variable, FontWeight.Bold, 700)
val Syne = variable(R.font.syne_variable, FontWeight.ExtraBold, 800)
val JetBrainsMono = variable(R.font.jetbrains_mono_variable, FontWeight.Normal, 400)

/** The one static file among the picks — IBM Plex Mono ships per weight, not variable. */
val IBMPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium)
)

/** Two choices ride on this face, so it carries both weights rather than two copies. */
val SpaceGrotesk = FontFamily(
    Font(
        R.font.space_grotesk_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.space_grotesk_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    )
)

/**
 * Semantic styles for the app.
 *
 * Styles that render the user's own content take a [UserFontChoice] and are functions;
 * chrome styles are fixed vals. [DMMono] is the chrome default, with the bottom
 * navigation the exception on [DMSans].
 */
object VisionType {
    // --- User-selectable face, resolved from the id the DB stores ---

    fun greeting(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 34.sp,
        lineHeight = 40.sp
    )

    /** Streak and weekly counts — "17 days", "18 / 21". */
    fun metricValue(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 18.sp,
        lineHeight = 22.sp
    )

    /**
     * Headline inside a card, where it shares the page with other sections. The same
     * slot holds the vision's name once set and the prompt to create one before that,
     * so both use this size.
     */
    fun cardTitle(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 24.sp,
        lineHeight = 30.sp
    )

    /**
     * The label inside a full-width pill button set in the user's face — a size down
     * from [cardTitle], since a button's own fill already gives it enough weight.
     */
    fun buttonLabel(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    /** Empty-state headline that owns a whole screen, so it carries more weight. */
    fun screenPromptTitle(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 32.sp,
        lineHeight = 38.sp
    )

    // --- Sheets and dialogs ---
    //
    // One scale for every panel that comes up over a screen, so a sheet and the question
    // it raises are set the same way wherever they appear. Held deliberately under the
    // page scale above: these are read once, close, with the answer right under them,
    // and the figures that give a screen its weight only crowd a panel a few lines tall.
    //
    // Set against the design table, with one standing rule — nothing here grows. Where
    // the table asked for more than the app already had, the app's own figure comes down
    // a step instead. The complaint these answer is that the sheets read large, and a
    // table is a description of proportions before it is a list of numbers.

    /** NEW VISION · ADD A MILESTONE · DELETE VISION — the panel's own name. */
    val sheetLabel = TextStyle(
        fontFamily = DMMono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.sp
    )

    /**
     * THE GOAL · TARGET DATE · BY — the name of a field rather than of the panel. The
     * same size, tracked a shade tighter, so the two read as different ranks without
     * either having to be the louder.
     */
    val fieldLabel = sheetLabel.copy(letterSpacing = 1.8.sp)

    /** How much room is left in a field — "0 / 25". The quietest mark on the panel. */
    val fieldCounter = TextStyle(
        fontFamily = DMMono,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.9.sp
    )

    /** The headline of a sheet. */
    fun sheetTitle(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 25.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.375).sp
    )

    /** The one line under a sheet's headline — "One step, one date." */
    fun sheetLead(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    /** A sheet's prose — what deleting costs, what a hundred per cent means. */
    fun sheetBody(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.14).sp
    )

    /**
     * What a vision is called. The largest thing written into a sheet, because it is the
     * one the whole sheet is about.
     */
    fun sheetGoalField(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.27).sp
    )

    /** The reason under it — an aside to the goal, so a step down from it. */
    fun sheetWhyField(font: UserFontChoice) = sheetGoalField(font).copy(
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.26).sp
    )

    /** A date, once chosen. Read as a figure to agree with rather than as prose. */
    fun sheetDateField(font: UserFontChoice) = sheetGoalField(font)

    /**
     * A line in a list — a milestone, a task on tomorrow's plan.
     *
     * Two under the field it is cousin to. Three of these stack one above another with
     * a rule between each, and at the goal's size the stack reads as three headlines
     * rather than as a list of three things.
     */
    fun sheetRowField(font: UserFontChoice) = sheetGoalField(font).copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.14).sp
    )

    /** The label in a sheet's buttons, whichever of the two answers it carries. */
    fun sheetButton(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.15).sp
    )

    /** The way out under them, which is not one of the two answers. */
    fun sheetCancel(font: UserFontChoice) = sheetButton(font).copy(
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.21).sp
    )

    /**
     * A dialog is the same panel at closer range — it covers the sheet it was raised
     * from rather than a screen, so every figure sits a step under the sheet's own.
     */
    val modalLabel = TextStyle(
        fontFamily = DMMono,
        fontWeight = FontWeight.Normal,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.9.sp
    )

    fun modalTitle(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 24.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.36).sp
    )

    fun modalBody(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 12.5.sp,
        lineHeight = 18.sp
    )

    fun modalButton(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    /** The daily wisdom quote inside its card. */
    fun quote(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 22.sp,
        lineHeight = 30.sp
    )

    /**
     * The percentage inside a progress ring. Sized to the ring rather than to the text
     * around it, so the figure fills the hole the ring leaves without crowding it.
     */
    fun ringValue(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 22.sp,
        lineHeight = 26.sp
    )

    /** The initial in the avatar chip. */
    fun avatar(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 13.sp,
        lineHeight = 16.sp
    )

    /**
     * The note under the list of slots — the rule the reader is being asked to keep.
     * Set in the user's own face, like the prompts it sits beneath, a size down from
     * them so it reads as an aside.
     */
    fun helperText(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    // --- Chrome, always DMMono ---

    val eyebrow = TextStyle(
        fontFamily = DMMono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp
    )

    /**
     * A symbol standing beside or inside a row — an arrow, a dismiss mark. Set above
     * the label it accompanies, because at eyebrow size these strokes all but vanish.
     */
    val glyph = TextStyle(
        fontFamily = DMMono,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 16.sp
    )

    /**
     * The sentence under a headline — the vision's own line, and the prompt shown in
     * its place before one is set.
     */
    fun bodyText(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 15.sp,
        lineHeight = 21.sp
    )

    val navLabel = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}

