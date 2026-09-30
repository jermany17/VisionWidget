package com.example.visionwidget.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Which of the three widgets a look belongs to.
 *
 * Studio can style one of these or all of them at once, but the store only ever holds
 * the three — "all" is a way of writing rather than a fourth thing to keep.
 */
enum class WidgetTarget(val key: String) {
    Vision("vision"),
    TopThree("top3"),
    Wisdom("wisdom")
}

/**
 * The five choices Studio commits together.
 *
 * The picture is deliberately not one of them: a photo is saved the moment it's picked,
 * while these wait for the apply, so folding it in here would make the two drift.
 */
data class WidgetStyle(
    val fontId: Int = UserFonts.DEFAULT_ID,
    val themeId: Int = CardThemes.DEFAULT_ID,
    val alignId: Int = Alignments.DEFAULT_ID,
    val backgroundId: Int = BackgroundStyles.DEFAULT_ID,
    val cornerRadius: Int = CornerRadii.DEFAULT
)

/**
 * One widget's whole appearance — the committed style and the picture behind it. Photos
 * are held per widget rather than shared: a picture is chosen for the card it sits in.
 */
data class WidgetLook(
    val style: WidgetStyle = WidgetStyle(),
    val photoUri: String? = null
)

/**
 * A look turned into the values a card actually draws with, so the same five lines of
 * lookup don't have to be repeated once per card on every screen that paints one.
 */
data class ResolvedLook(
    val skin: WidgetSkin,
    val photoUri: String?,
    val font: UserFontChoice,
    val align: AlignChoice,
    val shape: RoundedCornerShape
)

fun WidgetLook.resolve(): ResolvedLook = ResolvedLook(
    skin = widgetSkin(CardThemes[style.themeId], BackgroundStyles[style.backgroundId]),
    photoUri = photoUri,
    font = UserFonts[style.fontId],
    align = Alignments[style.alignId],
    shape = RoundedCornerShape(style.cornerRadius.dp)
)
