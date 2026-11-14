package app.visionov.visionwidget.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.visionov.visionwidget.MainActivity
import app.visionov.visionwidget.R
import app.visionov.visionwidget.data.AppDatabase
import app.visionov.visionwidget.data.AppPreferences
import app.visionov.visionwidget.data.TOP_THREE_SLOTS
import app.visionov.visionwidget.ui.home.wisdomFor
import app.visionov.visionwidget.widget.canScheduleExactAlarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val TAG = "VisionReminders"

/**
 * The three things the app says, and when it is allowed to say them.
 *
 * One channel each rather than one between them, because they are three different
 * interruptions and Android's own controls work per channel: a user who wants the
 * morning line but not the evening check can say so in the system settings without
 * having to come back in here, and the switches in here go on meaning what they say.
 */
enum class Reminder(
    val channelId: String,
    val channelName: String,
    val channelNote: String,
    val title: String
) {
    PlanTomorrow(
        channelId = "plan_tomorrow",
        channelName = "Plan tomorrow",
        channelNote = "An evening nudge to set tomorrow's three.",
        title = "Plan tomorrow"
    ),
    DailyWisdom(
        channelId = "daily_wisdom",
        channelName = "Daily Wisdom",
        channelNote = "One line from your chosen theme, each morning.",
        title = "Daily Wisdom"
    ),
    EveningCheck(
        channelId = "evening_check",
        channelName = "Evening check",
        channelNote = "A look in before the day ends, only if something's left.",
        title = "Evening check"
    );

    /** Distinct per reminder, so one landing never replaces another in the shade. */
    val notificationId: Int get() = 2100 + ordinal

    val action: String get() = "app.visionov.visionwidget.REMIND_$name"

    /** Whether this one is switched on, which is a question only the preferences answer. */
    fun enabledIn(preferences: AppPreferences) = when (this) {
        PlanTomorrow -> preferences.planTomorrowEnabled
        DailyWisdom -> preferences.dailyWisdomEnabled
        EveningCheck -> preferences.eveningCheckEnabled
    }

    /** What time of day it is set for, as minutes from midnight. */
    fun minutesIn(preferences: AppPreferences) = when (this) {
        PlanTomorrow -> preferences.planTomorrowMinutes
        DailyWisdom -> preferences.dailyWisdomMinutes
        EveningCheck -> preferences.eveningCheckMinutes
    }
}

/**
 * What the app sends to the shade, and the alarms that send it.
 *
 * Every reminder is one alarm set for its next occurrence and re-set when it lands,
 * rather than a repeating alarm: a repeat set once would hold the time it was set at
 * forever, and these are times the user moves.
 *
 * Nothing is scheduled that could not be shown. The master switch, the system permission
 * and the reminder's own switch all have to be on, and [sync] is the one place that is
 * decided — so a permission withdrawn in the system settings takes the alarms down with
 * it on the next return to the app rather than waking the phone to post nothing.
 */
object Reminders {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * How wide the alarm may land when it cannot be asked for exactly.
     *
     * A reminder is not a clock — ten minutes either side of the half hour is still the
     * evening, and the exact-alarm permission is asked for on behalf of the clock widgets
     * rather than this.
     */
    private const val WINDOW_MS = 10 * 60 * 1000L

    /** Whether Android would let a notification through at all. */
    fun systemAllows(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** The master switch and the permission together — what the screen's top row shows. */
    fun allowed(context: Context): Boolean =
        AppPreferences(context).notificationsEnabled && systemAllows(context)

    /**
     * Brings every alarm into line with what the settings now say.
     *
     * Safe to call as often as anything likes, and meant to be: it is the only way an
     * alarm is ever set or cancelled, so calling it after any change — a switch, a time,
     * a permission, a restart — is the whole of keeping the three in step.
     */
    fun sync(context: Context) {
        val app = context.applicationContext
        val preferences = AppPreferences(app)
        val alarms = app.getSystemService(AlarmManager::class.java) ?: return
        val on = preferences.notificationsEnabled && systemAllows(app)

        Reminder.entries.forEach { reminder ->
            val pending = pendingIntent(app, reminder)
            if (!on || !reminder.enabledIn(preferences)) {
                alarms.cancel(pending)
                return@forEach
            }
            val at = nextOccurrence(reminder.minutesIn(preferences))
            // Exactly where the user has already allowed it, and in a window where they
            // have not. Nothing here asks for that permission on its own account — see
            // [WINDOW_MS].
            runCatching {
                if (canScheduleExactAlarms(app)) {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                } else {
                    alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, pending)
                }
            }.onFailure { Log.w(TAG, "Could not set ${reminder.name}", it) }
        }
    }

    /** The next time today's clock reaches [minutes], or tomorrow's if it already has. */
    private fun nextOccurrence(minutes: Int, now: LocalDateTime = LocalDateTime.now()): Long {
        val today = now.toLocalDate().atTime(LocalTime.of(minutes / 60, minutes % 60))
        val at = if (today.isAfter(now)) today else today.plusDays(1)
        return at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun pendingIntent(context: Context, reminder: Reminder): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            reminder.notificationId,
            Intent(context, ReminderReceiver::class.java).setAction(reminder.action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * Posts [reminder], unless it has nothing to say.
     *
     * The evening check is the one that can decide not to speak: it is a check rather
     * than an alarm, and a day already finished has nothing to be reminded of. The other
     * two always have their line.
     */
    internal fun fire(context: Context, reminder: Reminder, done: () -> Unit) {
        val app = context.applicationContext
        scope.launch {
            try {
                // Asked again here rather than trusted from when the alarm was set. The
                // permission can be taken away in the hours between, and an alarm set
                // yesterday knows nothing about it.
                if (!allowed(app)) return@launch

                val preferences = AppPreferences(app)
                if (!reminder.enabledIn(preferences)) return@launch

                val line = when (reminder) {
                    Reminder.PlanTomorrow ->
                        "Tomorrow starts tonight. Choose tomorrow's three."

                    Reminder.DailyWisdom -> wisdomFor(
                        preferences.wisdomCategory,
                        preferences.wisdomIndex,
                        preferences.wisdomOwnText
                    ).text

                    // Silent on a day that is already done — see [somethingLeftToday].
                    Reminder.EveningCheck ->
                        if (somethingLeftToday(app)) "Something's still left for today." else null
                }

                if (line != null) post(app, reminder, line)
            } catch (error: Throwable) {
                Log.w(TAG, "Reminder ${reminder.name} failed", error)
            } finally {
                // Booked again whatever happened above. A reminder that stayed silent
                // tonight is still due tomorrow, and one that threw must not take the
                // whole series down with it.
                runCatching { sync(app) }
                done()
            }
        }
    }

    /**
     * Whether any of the day's three is written down and not yet ticked.
     *
     * Only the three the day is named for. The set behind them is not always tickable —
     * it unlocks on its own terms — and a reminder about something the user cannot tick
     * off is one they cannot answer.
     *
     * A day the slots have not been rolled over to is treated as empty: those rows still
     * belong to a day that has ended, and nothing in them is left for today.
     */
    private suspend fun somethingLeftToday(context: Context): Boolean {
        val dao = AppDatabase.getInstance(context).ruleOfThreeDao()
        val live = dao.liveDate()
        if (live != null && live != LocalDate.now().toEpochDay()) return false
        return dao.slots().any {
            it.slotIndex in TOP_THREE_SLOTS && !it.task.isNullOrBlank() && !it.checked
        }
    }

    private fun post(context: Context, reminder: Reminder, line: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    reminder.channelId,
                    reminder.channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = reminder.channelNote }
            )
        }

        val open = PendingIntent.getActivity(
            context,
            reminder.notificationId,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, reminder.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(line)
            // The day's line can run past one line in the shade, and a quote cut in half
            // is worse than one that takes two lines to arrive.
            .setStyle(NotificationCompat.BigTextStyle().bigText(line))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Permission is checked in [fire] a moment before this; the catch is for the
        // race where it is withdrawn in between, which throws rather than returning.
        runCatching {
            NotificationManagerCompat.from(context).notify(reminder.notificationId, notification)
        }.onFailure { Log.w(TAG, "Could not post ${reminder.name}", it) }
    }
}

/**
 * Takes a reminder's alarm, and the restart that loses every alarm the app had set.
 *
 * Alarms do not survive a reboot — the system drops them all — so the boot broadcast is
 * where the three are put back. [Reminders.sync] is the same call either way: it reads
 * the settings and leaves the alarms matching them, which is as true after a restart as
 * it is after a switch is moved.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            Reminders.sync(context)
            return
        }

        val reminder = Reminder.entries.firstOrNull { it.action == action }
        if (reminder == null) {
            Log.w(TAG, "Unknown reminder action: $action")
            return
        }

        // Held open until the notification is actually posted. The evening check reads
        // the database to decide whether to speak at all, and a receiver that returned
        // first is one the system may stop the app behind.
        val pending = goAsync()
        Reminders.fire(context, reminder) { pending.finish() }
    }
}
