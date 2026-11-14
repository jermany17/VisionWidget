package app.visionov.visionwidget.ui.home

import kotlin.random.Random

data class Wisdom(
    val category: String,
    val text: String
)

/** Seed list, shown until the DB is wired up. */
val WISDOM = listOf(
    Wisdom("motivation", "Motivation is a guest. Habit is a resident."),
    Wisdom("motivation", "The work you avoid is usually the work that counts."),
    Wisdom("motivation", "Start before you feel ready. Ready arrives later."),
    Wisdom("success", "Clarity comes from action, not from thinking about action."),
    Wisdom("success", "Consistency beats perfection, every single week."),
    Wisdom("success", "Three things. Nothing else today."),
    Wisdom("life", "You are allowed to move slowly and still arrive."),
    Wisdom("life", "Nobody is coming. That is the good news."),
    Wisdom("life", "Put the day down. It has been carried enough."),
    Wisdom("happiness", "Stay with what matters."),
    Wisdom("happiness", "One step is enough today."),
    Wisdom("happiness", "The good life is small, repeated, and yours."),
    Wisdom("wisdom", "You have power over your mind, not outside events."),
    Wisdom("wisdom", "Waste no more time arguing what a good person should be. Be one."),
    Wisdom("wisdom", "Tomorrow starts tonight."),

    // Reviewed pass, 2026-11-14 — see Vision_Widget_Quotes_Pass_2026-11-14.xlsx.
    Wisdom("motivation", "Begin with the energy you have, not the energy you wish you had."),
    Wisdom("motivation", "Make the next choice one your future self will appreciate."),
    Wisdom("motivation", "Small beginnings are still beginnings."),
    Wisdom("motivation", "Choose the next step, not the entire staircase."),
    Wisdom("motivation", "Keep a little promise to yourself today."),
    Wisdom("motivation", "Give yourself a beginning small enough to choose today."),
    Wisdom("motivation", "You can make progress before confidence catches up."),
    Wisdom("motivation", "Let one useful action interrupt the habit of waiting."),
    Wisdom("motivation", "Begin imperfectly; let the work teach you what comes next."),
    Wisdom("motivation", "Let the next ten minutes belong to something you care about."),
    Wisdom("motivation", "Make space for progress that nobody else can see."),
    Wisdom("motivation", "Keep a visible note of why you started."),
    Wisdom("motivation", "One step counts."),
    Wisdom("motivation", "Begin before doubt wins."),
    Wisdom("motivation", "Small steps matter."),
    Wisdom("motivation", "Move with intention."),
    Wisdom("motivation", "Keep your promise."),
    Wisdom("motivation", "Action creates momentum."),
    Wisdom("motivation", "Begin again gently."),
    Wisdom("motivation", "Let yourself begin."),
    Wisdom("motivation", "Forward is forward."),
    Wisdom("motivation", "Make today count."),
    Wisdom("motivation", "Remember your why."),
    Wisdom("success", "Let your standards guide you without making them a measure of your worth."),
    Wisdom("success", "Let your progress reflect your values, not someone else's timeline."),
    Wisdom("success", "Your next chapter is shaped by what you practice now."),
    Wisdom("success", "Make decisions for the life you want to maintain."),
    Wisdom("success", "Let achievement support your life rather than consume it."),
    Wisdom("success", "Define success before the world defines it for you."),
    Wisdom("success", "Build success around the life you want to keep living."),
    Wisdom("success", "Success becomes clearer when you decide what is enough."),
    Wisdom("success", "Build systems that support your values when motivation is quiet."),
    Wisdom("success", "Invest in skills and habits that remain useful after the milestone."),
    Wisdom("success", "Success can be steady without being spectacular."),
    Wisdom("success", "Not every opportunity deserves a place in your plan."),
    Wisdom("success", "Choose progress that aligns with your actual priorities."),
    Wisdom("success", "Make decisions for the ordinary days, not only the highlight reel."),
    Wisdom("success", "Build a future you can be present enough to enjoy."),
    Wisdom("success", "Your future is shaped in moments too small to announce themselves."),
    Wisdom("success", "Your priorities become real through the time you give them."),
    Wisdom("success", "Make your priorities visible in the way you arrange your day."),
    Wisdom("success", "Choose what matters."),
    Wisdom("success", "Build with intention."),
    Wisdom("success", "Consistency compounds."),
    Wisdom("success", "Depth over noise."),
    Wisdom("success", "Let values lead."),
    Wisdom("success", "Choose your measure."),
    Wisdom("success", "Create your own standard."),
    Wisdom("life", "Not every meaningful day needs a memorable story."),
    Wisdom("life", "A slower season can still be a season of becoming."),
    Wisdom("life", "Make choices that feel honest when nobody is watching."),
    Wisdom("life", "Some answers become clearer when you stop chasing them."),
    Wisdom("life", "Let curiosity keep your world open."),
    Wisdom("life", "Change can be a form of coming closer to yourself."),
    Wisdom("life", "Leave room for your life to surprise your plans."),
    Wisdom("life", "Not every detour takes you away from yourself."),
    Wisdom("life", "Let your values show up in the way you spend a Tuesday."),
    Wisdom("life", "Your life is not a race against an invisible clock."),
    Wisdom("life", "Give yourself permission to be a beginner again."),
    Wisdom("life", "Keep some hours free from the need to improve yourself."),
    Wisdom("life", "Not every season needs a lesson to be worthwhile."),
    Wisdom("life", "Let your plans be sturdy enough to guide you and flexible enough to change."),
    Wisdom("life", "Make a little room for what matters most to you."),
    Wisdom("life", "Build a routine that supports the life you actually want."),
    Wisdom("life", "Design your days around what you want to make room for."),
    Wisdom("life", "Choose a personal motto that feels true on an ordinary morning."),
    Wisdom("life", "Stay open to change."),
    Wisdom("life", "Make space for wonder."),
    Wisdom("life", "Live beyond the checklist."),
    Wisdom("life", "Begin a new chapter."),
    Wisdom("life", "Make your values visible."),
    Wisdom("life", "Make space for meaning."),
    Wisdom("life", "Build a life that fits."),
    Wisdom("happiness", "Happiness does not need an audience to be real."),
    Wisdom("happiness", "Let a small good thing be enough for this moment."),
    Wisdom("happiness", "Joy can be quiet and still change the tone of a day."),
    Wisdom("happiness", "Let happiness be something you experience, not another task to complete."),
    Wisdom("happiness", "You can appreciate today and still look forward to tomorrow."),
    Wisdom("happiness", "Enjoy the progress you once thought was far away."),
    Wisdom("happiness", "Let a good conversation be the achievement of the hour."),
    Wisdom("happiness", "Find happiness in the details that make a day feel personal."),
    Wisdom("happiness", "Let gratitude be specific enough to feel real."),
    Wisdom("happiness", "Choose reminders that bring you back to yourself."),
    Wisdom("happiness", "Choose reminders that encourage rather than criticize."),
    Wisdom("happiness", "Build a daily rhythm that leaves space for being human."),
    Wisdom("happiness", "Joy belongs here."),
    Wisdom("happiness", "Make room for delight."),
    Wisdom("happiness", "Enjoy what is here."),
    Wisdom("happiness", "Enough can feel good."),
    Wisdom("happiness", "Find your small joy."),
    Wisdom("happiness", "Receive the good."),
    Wisdom("happiness", "Come back to yourself."),
    Wisdom("wisdom", "Not every thought deserves a decision."),
    Wisdom("wisdom", "Give difficult ideas time to become clear."),
    Wisdom("wisdom", "Sometimes the most useful answer is a more precise question."),
    Wisdom("wisdom", "Pause long enough to separate a fact from a fear."),
    Wisdom("wisdom", "Let new evidence refine an old belief."),
    Wisdom("wisdom", "Clarity grows when you name the real question."),
    Wisdom("wisdom", "Learn to recognize when more information will actually help."),
    Wisdom("wisdom", "Your values deserve a place in your everyday decisions."),
    Wisdom("wisdom", "Make the important thing easy to remember."),
    Wisdom("wisdom", "Ask a better question."),
    Wisdom("wisdom", "Clarity takes patience."),
    Wisdom("wisdom", "Facts before fears."),
    Wisdom("wisdom", "Be willing to revise."),
    Wisdom("wisdom", "Seek to understand."),
    Wisdom("wisdom", "Perspective changes things."),
    Wisdom("wisdom", "Reflection is useful."),
    Wisdom("manifestation", "Picture the possibility, then give it a place in your day."),
    Wisdom("manifestation", "A vision becomes more real when your habits make room for it."),
    Wisdom("manifestation", "Dream with openness; move with intention."),
    Wisdom("manifestation", "Let the life you imagine influence the choices you make today."),
    Wisdom("manifestation", "Give your aspirations attention, patience, and practical care."),
    Wisdom("manifestation", "Let your intention guide your attention, and your attention guide your actions."),
    Wisdom("manifestation", "Act in alignment with the future you want to grow into."),
    Wisdom("manifestation", "Let your environment remind you of what you are inviting into your life."),
    Wisdom("manifestation", "Prepare for the opportunities you hope to recognize."),
    Wisdom("manifestation", "Notice the small openings that align with what you are seeking."),
    Wisdom("manifestation", "Let your surroundings support the person you are becoming."),
    Wisdom("manifestation", "Build a ritual that helps you remember your own direction."),
    Wisdom("manifestation", "Let your everyday surroundings reflect the life you are creating."),
    Wisdom("manifestation", "Make room for possibility."),
    Wisdom("manifestation", "Move with intention."),
    Wisdom("manifestation", "Vision into action."),
    Wisdom("manifestation", "Trust the next step."),
    Wisdom("manifestation", "Align with your vision."),
    Wisdom("manifestation", "Notice new possibilities."),
    Wisdom("manifestation", "See it, shape it.")
)

/**
 * The themes offered as a choice, in the order they're listed. Named here rather than at
 * each picker so onboarding and Studio can't drift into offering different sets.
 */
val WISDOM_THEMES = listOf(
    "Motivation",
    "Success",
    "Life",
    "Happiness",
    "Wisdom",
    "Manifestation"
)

/**
 * Where the daily line is drawn from until a theme is chosen — skipping the question is
 * answered with the first theme it offered rather than with all of them at once.
 */
val DEFAULT_WISDOM_THEME = WISDOM_THEMES.first().lowercase()

/**
 * The line the user wrote themselves, which belongs to no theme in the list above.
 *
 * Held as a category rather than as a flag beside one: everywhere a theme is read, this
 * is the same kind of answer, and the card asks the same question of both.
 */
const val OWN_WISDOM_CATEGORY = "your own"

/** What that choice is called where the themes are offered. */
const val OWN_WISDOM_LABEL = "Your own"

/** Stands in until a line is written, so the card is never left blank. */
const val OWN_WISDOM_PLACEHOLDER = "Write a line of your own."

/**
 * How long the user's own line can run. The widest the quote is ever set is the 4x4
 * card, which holds about this much before it has to start cutting.
 */
const val MAX_OWN_WISDOM_LENGTH = 70

fun isOwnWisdom(category: String) = category.equals(OWN_WISDOM_CATEGORY, ignoreCase = true)

/**
 * The line a category is showing — the user's own, or the one at [index].
 *
 * Every card goes through here rather than reaching into [WISDOM] by index, since under
 * the user's own theme there is no index to reach by.
 */
fun wisdomFor(category: String, index: Int, ownText: String): Wisdom =
    if (isOwnWisdom(category)) {
        Wisdom(OWN_WISDOM_CATEGORY, ownText.ifBlank { OWN_WISDOM_PLACEHOLDER })
    } else {
        WISDOM[index.coerceIn(WISDOM.indices)]
    }

/**
 * The lines belonging to [category].
 *
 * A name with nothing under it falls back to the whole list rather than to nothing: an
 * empty theme would leave the card with no quote to draw at all.
 */
fun wisdomIndices(category: String): List<Int> {
    if (isOwnWisdom(category)) return emptyList()
    val matching = WISDOM.indices.filter { WISDOM[it].category.equals(category, ignoreCase = true) }
    return matching.ifEmpty { WISDOM.indices.toList() }
}

/** A line to open on, drawn from within [category]. */
fun randomWisdomIndex(category: String): Int = wisdomIndices(category).random()

/**
 * Picks an entry other than [current] from within [category], so a shuffle tap always
 * visibly changes the quote instead of sometimes landing on the one already showing.
 */
fun nextWisdomIndex(current: Int, category: String): Int {
    val pool = wisdomIndices(category)
    if (pool.size < 2) return pool.firstOrNull() ?: current
    val position = pool.indexOf(current)
    // Showing a line from outside the theme — it was just changed under the card — so
    // anything in the new theme is a move rather than a repeat.
    if (position < 0) return pool.random()
    return pool[(position + 1 + Random.nextInt(pool.size - 1)) % pool.size]
}
