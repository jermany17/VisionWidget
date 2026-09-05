package com.example.visionwidget.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.visionwidget.R

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

    /** Empty-state headline that owns a whole screen, so it carries more weight. */
    fun screenPromptTitle(font: UserFontChoice) = TextStyle(
        fontFamily = font.family,
        fontWeight = font.weight,
        fontSize = 32.sp,
        lineHeight = 38.sp
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

