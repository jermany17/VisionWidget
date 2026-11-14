package app.visionov.visionwidget.data

import android.content.Context
import app.visionov.visionwidget.ui.home.DEFAULT_WISDOM_THEME
import app.visionov.visionwidget.ui.home.isOwnWisdom
import app.visionov.visionwidget.ui.home.nextWisdomIndex
import app.visionov.visionwidget.ui.theme.Alignments
import app.visionov.visionwidget.ui.theme.BackgroundStyles
import app.visionov.visionwidget.ui.theme.CardThemes
import app.visionov.visionwidget.ui.theme.CornerRadii
import app.visionov.visionwidget.ui.theme.UserFonts
import app.visionov.visionwidget.ui.theme.WidgetStyle
import app.visionov.visionwidget.ui.theme.WidgetTarget

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
     * False until the tour that follows onboarding has been through once.
     *
     * Kept apart from [hasSeenOnboarding] because the two are answered at different
     * moments and one is skippable without the other: onboarding asks for a goal, and
     * this only shows what the app does with it.
     */
    var hasSeenTutorial: Boolean
        get() = prefs.getBoolean(KEY_SEEN_TUTORIAL, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEN_TUTORIAL, value).apply()

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
     * The day the line now showing was turned to, as an epoch day. Zero until the first
     * day is recorded.
     *
     * Kept beside the line itself because nothing is running at midnight to be told the
     * date changed. Whichever of the app and the widgets wakes first works out that a
     * turn is owed, and this is what stops the other from turning it a second time.
     */
    var wisdomDay: Long
        get() = prefs.getLong(KEY_WISDOM_DAY, 0L)
        set(value) = prefs.edit().putLong(KEY_WISDOM_DAY, value).apply()

    /**
     * Moves the daily line on if [today] is not the day it was last turned to.
     *
     * Beside the shuffle control rather than instead of it: the user may still press
     * that as often as they like, and this only sees to it that a day never opens on
     * the line the one before it closed with. The move is [nextWisdomIndex]'s, so it is
     * random within the theme and never lands on the line already showing.
     *
     * Two days are left alone. The user's own theme holds a single line, so turning it
     * would land on the same words; and the very first day only records itself, because
     * a line just chosen in onboarding is not one to replace before it has been read.
     *
     * Held under a lock because the three wisdom widgets are rendered in parallel and
     * each one asks. Unguarded, two could both read yesterday's stamp and the day would
     * turn the line twice.
     *
     * @return whether the line actually moved, so the caller can redraw what shows it.
     */
    fun rollWisdomForDay(today: Long): Boolean = synchronized(RollLock) {
        if (wisdomDay == today) return false

        val firstEver = wisdomDay == 0L
        wisdomDay = today
        if (firstEver || isOwnWisdom(wisdomCategory)) return false

        val moved = nextWisdomIndex(wisdomIndex, wisdomCategory)
        if (moved == wisdomIndex) return false

        wisdomIndex = moved
        return true
    }

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
     * The Gallery designs the user has put in their library, in the order they put them
     * there.
     *
     * The library is what the launcher's picker offers: a size is chosen there, and the
     * setting screen that follows shows the three dressed cards and nothing else of the
     * Gallery but these. Forty-four designs would be a list nobody reads, so a design
     * reaches the home screen only once it has been asked for here.
     *
     * Held in preferences for the reason [plannedTasks] is, and joined the same way — a
     * design id is a slug with no newline in it.
     */
    var libraryDesignIds: List<String>
        get() = prefs.getString(KEY_LIBRARY, "")
            .orEmpty()
            .split('\n')
            .let { if (it.size == 1 && it.first().isEmpty()) emptyList() else it }
        set(value) = prefs.edit().putString(KEY_LIBRARY, value.joinToString("\n")).apply()


    /**
     * Which base plan Vision+ was last bought on, noted when the purchase sheet opens.
     *
     * Play tells the client that a subscription is held but not which plan it is held
     * on, and only the settings row's wording turns on that — so this is a note to
     * ourselves, not a record of anything. Whether the subscription exists is Play's
     * answer and is never kept here: a copy of that is a copy that can be edited.
     */
    var purchasedBasePlanId: String?
        get() = prefs.getString(KEY_PURCHASED_PLAN, null)
        set(value) = prefs.edit().putString(KEY_PURCHASED_PLAN, value).apply()

    /**
     * What Play last answered about Vision+, for the widgets to read.
     *
     * Not a second opinion on whether the subscription is held — Play's answer is the
     * only one, and [BillingManager] is where it is asked for. This is a note of that
     * answer left where a widget can reach it: a widget is drawn with no activity up
     * and often with the app never opened, and opening a billing connection on every
     * redraw to re-ask a question already answered is not something a home screen
     * should be made to pay for.
     *
     * Starts held. A cell that has been drawing a design for months must not go behind
     * a notice on the strength of nothing having been asked yet — only on Play actually
     * saying the subscription has gone.
     */
    var plusHeld: Boolean
        get() = prefs.getBoolean(KEY_PLUS_HELD, true)
        set(value) = prefs.edit().putBoolean(KEY_PLUS_HELD, value).apply()

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
        // Held to the range the slider offers: a corner saved when it reached further
        // would otherwise stay on the widget with no way left to move it back.
        cornerRadius = prefs.getInt(target.key(KEY_WIDGET_RADIUS), CornerRadii.DEFAULT)
            .coerceIn(CornerRadii.MIN, CornerRadii.MAX),
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
        /** Shared, not per-instance: every caller builds its own [AppPreferences]. */
        val RollLock = Any()

        const val PREFS_NAME = "vision_prefs"
        const val KEY_WIDGET_RADIUS = "widget_corner_radius"
        const val KEY_SEEN_ONBOARDING = "has_seen_onboarding"
        const val KEY_SEEN_TUTORIAL = "has_seen_tutorial"
        const val KEY_WISDOM_CATEGORY = "wisdom_category"
        const val KEY_WISDOM_INDEX = "wisdom_index"
        const val KEY_WISDOM_OWN_TEXT = "wisdom_own_text"
        const val KEY_WISDOM_DAY = "wisdom_day"
        const val KEY_PLANNED_DAY = "planned_day"
        const val KEY_PLANNED_TASKS = "planned_tasks"
        const val KEY_LIBRARY = "library_design_ids"
        const val KEY_PURCHASED_PLAN = "purchased_base_plan"
        const val KEY_PLUS_HELD = "plus_held"
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
