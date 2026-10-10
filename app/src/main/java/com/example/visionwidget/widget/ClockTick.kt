package com.example.visionwidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Keeps the clock's hands moving.
 *
 * A widget is only redrawn on the schedule its provider declares, and the shortest the
 * platform honours there is half an hour — which would leave a clock wrong almost all
 * of the time. An alarm is the only way to ask for a minute, so the clock sets one for
 * as long as one of its widgets is up and cancels it when the last goes.
 *
 * Inexact, deliberately. An exact alarm every minute needs a permission meant for
 * alarm clocks and costs battery to match; an inexact one is batched with whatever else
 * the system is already waking for, which is close enough for a hand that moves six
 * degrees a minute. While the screen is off it may not fire at all — which is fine,
 * since nobody is looking.
 */
private const val TICK_INTERVAL_MILLIS = 60_000L

private fun tickIntent(context: Context) = PendingIntent.getBroadcast(
    context,
    0,
    Intent(context, ClockTickReceiver::class.java),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

/** Starts the minute, aligned to the next one so the hand moves when the clock does. */
fun scheduleClockTick(context: Context) {
    val alarms = context.getSystemService(AlarmManager::class.java) ?: return
    val now = System.currentTimeMillis()
    val nextMinute = now - now % TICK_INTERVAL_MILLIS + TICK_INTERVAL_MILLIS
    alarms.setRepeating(
        AlarmManager.RTC,
        nextMinute,
        TICK_INTERVAL_MILLIS,
        tickIntent(context)
    )
}

fun cancelClockTick(context: Context) {
    context.getSystemService(AlarmManager::class.java)?.cancel(tickIntent(context))
}

class ClockTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // The redraw outlives onReceive, so the broadcast is held open until it lands.
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ClockAppWidget().updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
