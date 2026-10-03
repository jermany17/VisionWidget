package com.example.visionwidget.data

import android.content.Context
import com.example.visionwidget.ui.theme.Alignments
import com.example.visionwidget.ui.theme.BackgroundStyles
import com.example.visionwidget.ui.theme.CardThemes
import com.example.visionwidget.ui.theme.CornerRadii
import com.example.visionwidget.ui.theme.UserFonts
import com.example.visionwidget.ui.theme.WidgetStyle
import com.example.visionwidget.ui.theme.WidgetTarget

/**
 * The few flags that aren't the user's own content.
 *
 * These live in preferences rather than the database so adding one never costs a schema
 * migration of the records themselves.
 */
class AppPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateSharedStyle()
    }

    /**
     * False until the onboarding flow has been through once. Skipping counts as having
     * seen it — the flow is an introduction, not a form that has to be completed.
     */
    var hasSeenOnboarding: Boolean
        get() = prefs.getBoolean(KEY_SEEN_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEN_ONBOARDING, value).apply()

    /**
     * The theme the daily line is drawn from, as picked on the last onboarding step.
     * Null until one is chosen, which draws from every theme.
     */
    var wisdomCategory: String?
        get() = prefs.getString(KEY_WISDOM_CATEGORY, null)
        set(value) = prefs.edit().putString(KEY_WISDOM_CATEGORY, value).apply()

    /**
     * How one widget renders the user's own words — face, colour, layout, fill and
     * corner. Held per widget, so the three can be styled apart or together. The rest of
     * the app stays on the default face, so this is a choice about the widgets alone.
     */
    fun widgetStyle(target: WidgetTarget) = WidgetStyle(
        fontId = prefs.getInt(target.key(KEY_WIDGET_FONT), UserFonts.DEFAULT_ID),
        themeId = prefs.getInt(target.key(KEY_WIDGET_THEME), CardThemes.DEFAULT_ID),
        alignId = prefs.getInt(target.key(KEY_WIDGET_ALIGN), Alignments.DEFAULT_ID),
        backgroundId = prefs.getInt(target.key(KEY_WIDGET_BACKGROUND), BackgroundStyles.DEFAULT_ID),
        cornerRadius = prefs.getInt(target.key(KEY_WIDGET_RADIUS), CornerRadii.DEFAULT)
    )

    fun setWidgetStyle(target: WidgetTarget, style: WidgetStyle) {
        prefs.edit()
            .putInt(target.key(KEY_WIDGET_FONT), style.fontId)
            .putInt(target.key(KEY_WIDGET_THEME), style.themeId)
            .putInt(target.key(KEY_WIDGET_ALIGN), style.alignId)
            .putInt(target.key(KEY_WIDGET_BACKGROUND), style.backgroundId)
            .putInt(target.key(KEY_WIDGET_RADIUS), style.cornerRadius)
            .apply()
    }

    /**
     * The picture behind one widget when its background is Photo, as the content URI the
     * system picker handed back. Null until one is chosen — the style can be selected
     * before a picture exists, and shows a placeholder until it does.
     */
    fun widgetPhotoUri(target: WidgetTarget): String? =
        prefs.getString(target.key(KEY_WIDGET_PHOTO), null)

    fun setWidgetPhotoUri(target: WidgetTarget, uri: String?) {
        prefs.edit().putString(target.key(KEY_WIDGET_PHOTO), uri).apply()
    }

    /**
     * Studio used to apply one look to every widget, so the style was stored once with
     * no target in the key. Those values are handed to all three and the old keys
     * dropped, which both carries the user's look across and keeps the read path free of
     * a fallback that would otherwise have to live there forever.
     */
    private fun migrateSharedStyle() {
        if (SHARED_KEYS.none { prefs.contains(it) }) return

        val shared = WidgetStyle(
            fontId = prefs.getInt(KEY_WIDGET_FONT, UserFonts.DEFAULT_ID),
            themeId = prefs.getInt(KEY_WIDGET_THEME, CardThemes.DEFAULT_ID),
            alignId = prefs.getInt(KEY_WIDGET_ALIGN, Alignments.DEFAULT_ID),
            backgroundId = prefs.getInt(KEY_WIDGET_BACKGROUND, BackgroundStyles.DEFAULT_ID),
            cornerRadius = prefs.getInt(KEY_WIDGET_RADIUS, CornerRadii.DEFAULT)
        )
        val photo = prefs.getString(KEY_WIDGET_PHOTO, null)

        prefs.edit().apply {
            WidgetTarget.entries.forEach { target ->
                putInt(target.key(KEY_WIDGET_FONT), shared.fontId)
                putInt(target.key(KEY_WIDGET_THEME), shared.themeId)
                putInt(target.key(KEY_WIDGET_ALIGN), shared.alignId)
                putInt(target.key(KEY_WIDGET_BACKGROUND), shared.backgroundId)
                putInt(target.key(KEY_WIDGET_RADIUS), shared.cornerRadius)
                putString(target.key(KEY_WIDGET_PHOTO), photo)
            }
            SHARED_KEYS.forEach { remove(it) }
        }.apply()
    }

    private companion object {
        const val PREFS_NAME = "vision_prefs"
        const val KEY_WIDGET_RADIUS = "widget_corner_radius"
        const val KEY_SEEN_ONBOARDING = "has_seen_onboarding"
        const val KEY_WISDOM_CATEGORY = "wisdom_category"
        const val KEY_WIDGET_FONT = "widget_font_id"
        const val KEY_WIDGET_THEME = "widget_theme_id"
        const val KEY_WIDGET_ALIGN = "widget_align_id"
        const val KEY_WIDGET_BACKGROUND = "widget_background_id"
        const val KEY_WIDGET_PHOTO = "widget_photo_uri"

        /** The pre-per-widget names, kept only so an existing install can be moved off them. */
        val SHARED_KEYS = listOf(
            KEY_WIDGET_FONT,
            KEY_WIDGET_THEME,
            KEY_WIDGET_ALIGN,
            KEY_WIDGET_BACKGROUND,
            KEY_WIDGET_RADIUS,
            KEY_WIDGET_PHOTO
        )

        fun WidgetTarget.key(base: String) = "${base}_$key"
    }
}
