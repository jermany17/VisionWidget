package com.example.visionwidget.data

import android.content.Context
import com.example.visionwidget.ui.theme.Alignments
import com.example.visionwidget.ui.theme.BackgroundStyles
import com.example.visionwidget.ui.theme.CardThemes
import com.example.visionwidget.ui.theme.UserFonts

/**
 * The few flags that aren't the user's own content.
 *
 * These live in preferences rather than the database so adding one never costs a schema
 * migration of the records themselves.
 */
class AppPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * False until the onboarding flow has been through once. Skipping counts as having
     * seen it — the flow is an introduction, not a form that has to be completed.
     */
    var hasSeenOnboarding: Boolean
        get() = prefs.getBoolean(KEY_SEEN_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEN_ONBOARDING, value).apply()

    /**
     * The face the widgets render the user's own words in — the cards on Today, the
     * Studio preview, and the home screen widgets themselves. The rest of the app stays
     * on the default face, so this is a choice about the widgets rather than the theme.
     */
    var widgetFontId: Int
        get() = prefs.getInt(KEY_WIDGET_FONT, UserFonts.DEFAULT_ID)
        set(value) = prefs.edit().putInt(KEY_WIDGET_FONT, value).apply()

    /** The colour the widget cards are painted in. Scoped the same way as the face. */
    var widgetThemeId: Int
        get() = prefs.getInt(KEY_WIDGET_THEME, CardThemes.DEFAULT_ID)
        set(value) = prefs.edit().putInt(KEY_WIDGET_THEME, value).apply()

    /** How the widget cards set out their words. Scoped the same way as face and colour. */
    var widgetAlignId: Int
        get() = prefs.getInt(KEY_WIDGET_ALIGN, Alignments.DEFAULT_ID)
        set(value) = prefs.edit().putInt(KEY_WIDGET_ALIGN, value).apply()

    /** How the widget cards are filled behind their words. */
    var widgetBackgroundId: Int
        get() = prefs.getInt(KEY_WIDGET_BACKGROUND, BackgroundStyles.DEFAULT_ID)
        set(value) = prefs.edit().putInt(KEY_WIDGET_BACKGROUND, value).apply()

    /**
     * The picture behind the cards when the background is Photo, as the content URI the
     * system picker handed back. Null until one is chosen — the style can be selected
     * before a picture exists, and shows a placeholder until it does.
     */
    var widgetPhotoUri: String?
        get() = prefs.getString(KEY_WIDGET_PHOTO, null)
        set(value) = prefs.edit().putString(KEY_WIDGET_PHOTO, value).apply()

    private companion object {
        const val PREFS_NAME = "vision_prefs"
        const val KEY_SEEN_ONBOARDING = "has_seen_onboarding"
        const val KEY_WIDGET_FONT = "widget_font_id"
        const val KEY_WIDGET_THEME = "widget_theme_id"
        const val KEY_WIDGET_ALIGN = "widget_align_id"
        const val KEY_WIDGET_BACKGROUND = "widget_background_id"
        const val KEY_WIDGET_PHOTO = "widget_photo_uri"
    }
}
