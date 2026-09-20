package com.example.visionwidget.data

import android.content.Context
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

    private companion object {
        const val PREFS_NAME = "vision_prefs"
        const val KEY_SEEN_ONBOARDING = "has_seen_onboarding"
        const val KEY_WIDGET_FONT = "widget_font_id"
    }
}
