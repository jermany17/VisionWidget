package com.example.visionwidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Asks the launcher to put [receiver]'s widget on the home screen.
 *
 * Declaring a widget only ever offers it in the launcher's own picker; it takes the user
 * going there and dragging it out for one to actually land. This asks on their behalf
 * instead, so a face can be put up from the shelf it was chosen on.
 *
 * The launcher answers with its own confirmation, and may decline to be asked at all —
 * a few don't support being asked, and there's nothing to be done about that but say so.
 * False means the ask never happened.
 */
fun requestPinWidget(context: Context, receiver: Class<*>): Boolean {
    val manager = AppWidgetManager.getInstance(context) ?: return false
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
}

/**
 * How many of [receiver]'s widgets are on the home screen.
 *
 * A count rather than a yes or no: the same face can be put up more than once, on
 * different pages or at different sizes, so having one already is no reason to stop
 * offering another.
 */
fun placedWidgetCount(context: Context, receiver: Class<*>): Int {
    val manager = AppWidgetManager.getInstance(context) ?: return 0
    return manager.getAppWidgetIds(ComponentName(context, receiver)).size
}
