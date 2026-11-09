package app.visionov.visionwidget.widget

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Whether the clocks may be woken on the second rather than near it.
 *
 * An inexact alarm is allowed to land anywhere in a window the system chooses, and on a
 * phone that has decided the app is idle that window grows: a minute hand that should
 * turn on the minute turns somewhere in the half-minute after it. Asking exactly is the
 * only way to hold a clock to the clock.
 *
 * Granted by the user in Settings rather than by a dialog of ours — there is no runtime
 * prompt for this one — and revocable at any time, so it is asked rather than assumed
 * every time an alarm is set.
 *
 * Before Android 12 there was no such permission and exact alarms were simply allowed.
 */
fun canScheduleExactAlarms(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarms = context.getSystemService(AlarmManager::class.java) ?: return false
    return alarms.canScheduleExactAlarms()
}

/**
 * The Settings page where the permission is given, or null where there is none to open.
 *
 * Points at this app's own entry rather than the list of every app's.
 */
fun exactAlarmSettings(context: Context): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        .setData(Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/** Whether a Gallery design needs the minute to be exact — only the clocks do. */
fun needsExactAlarms(category: String) = category.equals("clock", ignoreCase = true)
