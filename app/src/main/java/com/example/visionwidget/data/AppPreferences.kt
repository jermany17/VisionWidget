package com.example.visionwidget.data

import android.content.Context
import com.example.visionwidget.ui.home.DEFAULT_WISDOM_THEME
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
     * Skipping that step leaves it on [DEFAULT_WISDOM_THEME] rather than on no theme at
     * all, so the card is always drawn from somewhere nameable.
     */
    var wisdomCategory: String
        get() = prefs.getString(KEY_WISDOM_CATEGORY, null) ?: DEFAULT_WISDOM_THEME
        set(value) = prefs.edit().putString(KEY_WISDOM_CATEGORY, value).apply()

    /**
     * Which line the daily card is showing.
     *
     * Kept rather than worked out from the date, because the card can be shuffled: the
     * widget has to see the line the app settled on, not one it picked for itself.
     */
    var wisdomIndex: Int
        get() = prefs.getInt(KEY_WISDOM_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_WISDOM_INDEX, value).apply()

    /**
     * The one line the user wrote themselves, shown when the theme is their own. Blank
     * until they write one, which the card stands a placeholder in for.
     */
    var wisdomOwnText: String
        get() = prefs.getString(KEY_WISDOM_OWN_TEXT, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_WISDOM_OWN_TEXT, value).apply()

    /**
     * The day the two sets of three last changed places, or zero if they never have.
     *
     * Kept as a date rather than a flag so it clears itself: a new day is a day this
     * does not match, and the handover is there to be earned once more. It is the one
     * thing about the sets that cannot be read off the slots — once they have swapped,
     * the arrangement is indistinguishable from one the user built that way.
     */
    var topThreeSwappedOn: Long
        get() = prefs.getLong(KEY_TOP_THREE_SWAPPED_ON, 0L)
        set(value) = prefs.edit().putLong(KEY_TOP_THREE_SWAPPED_ON, value).apply()

    /**
     * The day the plan waiting in the wings is for, or zero while there is none.
     *
     * A plan is written the evening before and taken up when that day arrives, so it is
     * dated: the same lines are a day's work if the app is opened on the day and a day
     * that was never started if it is opened after.
     */
    var plannedDay: Long
        get() = prefs.getLong(KEY_PLANNED_DAY, 0L)
        set(value) = prefs.edit().putLong(KEY_PLANNED_DAY, value).apply()

    /**
     * What was planned, in slot order across both sets, blanks for the slots left alone.
     *
     * Kept here rather than in a table of its own. It is one day's six lines, read once
     * and spent, and the database has no way through it yet — it was laid down at
     * version one with no migrations, so a table added now would meet an older install
     * with nowhere to go.
     *
     * Joined on newlines, which the fields it comes from cannot hold: every one of them
     * is a single line that commits on Done.
     */
    var plannedTasks: List<String>
        get() = prefs.getString(KEY_PLANNED_TASKS, "")
            .orEmpty()
            .split('\n')
            .let { if (it.size == 1 && it.first().isEmpty()) emptyList() else it }
        set(value) = prefs.edit().putString(KEY_PLANNED_TASKS, value.joinToString("\n")).apply()

    /** Forgets the plan, whether it was taken up or missed. */
    fun clearPlan() {
        prefs.edit()
            .remove(KEY_PLANNED_DAY)
            .remove(KEY_PLANNED_TASKS)
            .apply()
    }


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
        cornerRadius = prefs.getInt(target.key(KEY_WIDGET_RADIUS), CornerRadii.DEFAULT),
        customColour = prefs.getInt(target.key(KEY_WIDGET_CUSTOM_COLOUR), 0)
    )

    fun setWidgetStyle(target: WidgetTarget, style: WidgetStyle) {
        prefs.edit()
            .putInt(target.key(KEY_WIDGET_FONT), style.fontId)
            .putInt(target.key(KEY_WIDGET_THEME), style.themeId)
            .putInt(target.key(KEY_WIDGET_ALIGN), style.alignId)
            .putInt(target.key(KEY_WIDGET_BACKGROUND), style.backgroundId)
            .putInt(target.key(KEY_WIDGET_RADIUS), style.cornerRadius)
            .putInt(target.key(KEY_WIDGET_CUSTOM_COLOUR), style.customColour)
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
        const val KEY_WISDOM_INDEX = "wisdom_index"
        const val KEY_WISDOM_OWN_TEXT = "wisdom_own_text"
        const val KEY_TOP_THREE_SWAPPED_ON = "top_three_swapped_on"
        const val KEY_PLANNED_DAY = "planned_day"
        const val KEY_PLANNED_TASKS = "planned_tasks"
        const val KEY_WIDGET_FONT = "widget_font_id"
        const val KEY_WIDGET_THEME = "widget_theme_id"
        const val KEY_WIDGET_CUSTOM_COLOUR = "widget_custom_colour"
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
