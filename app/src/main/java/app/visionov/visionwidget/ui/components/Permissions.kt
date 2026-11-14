package app.visionov.visionwidget.ui.components

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.visionov.visionwidget.data.AppPreferences
import app.visionov.visionwidget.notify.Reminder
import app.visionov.visionwidget.notify.Reminders
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.DMMono
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.Rule
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.VisionType
import app.visionov.visionwidget.widget.canScheduleExactAlarms
import app.visionov.visionwidget.widget.exactAlarmSettings

/**
 * This screen is the one page in the app that is read as a list of settings rather than
 * as a card on a canvas, so it inverts the usual pair: the page is the warm paper and
 * the groups sit on it in white. Grouped rows need the page behind them to be the darker
 * of the two, or the groups stop reading as separate things.
 */
private val PageFill = Color(0xFFF1EEE8)
private val BlockFill = Color(0xFFFCFBF9)
private val BlockShape = RoundedCornerShape(14.dp)

/**
 * The list's own scale, held well under the sheet titles above it.
 *
 * Every line here is one of a stack of a dozen, and at the size a sheet sets its single
 * headline the stack reads as a dozen headlines. The notes under them are set in the
 * user's own face rather than in the chrome mono: they are sentences to be read, and
 * mono at this size turns a sentence into a label.
 */
private fun rowTitle(font: UserFontChoice) = VisionType.userFace(font, 15.sp, 19.sp, (-0.2).sp)
private fun rowNote(font: UserFontChoice) = VisionType.userFace(font, 11.5.sp, 15.sp)
private val rowValue = VisionType.fieldCounter

/** The wheel's figures — mono, so the columns do not shuffle as the digits change. */
private val wheelDigit = TextStyle(
    fontFamily = DMMono,
    fontSize = 15.sp,
    lineHeight = 19.sp
)

/** Material's switch is built for a denser list than this one; brought down to match. */
private const val SwitchScale = 0.76f

/**
 * What "Permissions & access" opens — everything the app is allowed to interrupt with,
 * and the one thing the clock widgets need from the system.
 *
 * The two halves are not the same kind of thing and are not asked for the same way. The
 * reminders are ours to switch, so they are switches; the exact-alarm permission is
 * Android's to grant and cannot be given or taken from in here, so that row shows where
 * it stands and opens the page where it is actually decided.
 */
@Composable
fun PermissionsPage(userFont: UserFontChoice, onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { AppPreferences(context) }

    // Read once into state and written straight through on every change, so the switches
    // answer the finger rather than waiting for a read back from disk.
    var master by remember { mutableStateOf(preferences.notificationsEnabled) }
    var systemAllows by remember { mutableStateOf(Reminders.systemAllows(context)) }
    var exactAlarms by remember { mutableStateOf(canScheduleExactAlarms(context)) }

    var planOn by remember { mutableStateOf(preferences.planTomorrowEnabled) }
    var planAt by remember { mutableIntStateOf(preferences.planTomorrowMinutes) }
    var wisdomOn by remember { mutableStateOf(preferences.dailyWisdomEnabled) }
    var wisdomAt by remember { mutableIntStateOf(preferences.dailyWisdomMinutes) }
    var eveningOn by remember { mutableStateOf(preferences.eveningCheckEnabled) }
    var eveningAt by remember { mutableIntStateOf(preferences.eveningCheckMinutes) }

    // Which row has its wheel out, if any. One at a time: three wheels open at once is a
    // page of spinning digits with no way to tell which one the next flick would move.
    var openPicker by remember { mutableStateOf<Reminder?>(null) }

    // Both of these are decided outside the app and can be changed while it is open, so
    // they are asked again on every return rather than trusted from when the sheet was
    // built. The alarms are brought back into line with whatever the answer turns out
    // to be — a permission withdrawn in the system settings takes them down here.
    LifecycleResumeEffect(Unit) {
        systemAllows = Reminders.systemAllows(context)
        exactAlarms = canScheduleExactAlarms(context)
        Reminders.sync(context)
        onPauseOrDispose { }
    }

    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        systemAllows = granted && Reminders.systemAllows(context)
        // Refused. Android only shows its own dialog so many times, so the second
        // refusal is silent and there would be nothing left for a tap to do — the one
        // place the answer can still be changed is the system's own page.
        if (!granted) openNotificationSettings(context)
        Reminders.sync(context)
    }

    /** The master row's true state: ours and Android's both have to say yes. */
    val notificationsOn = master && systemAllows

    Box(
        Modifier
            .fillMaxSize()
            .background(PageFill)
            .statusBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "‹  Settings",
                style = VisionType.userFace(userFont, 13.sp, 17.sp),
                color = OnCanvasMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onBack)
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Permissions & access",
                style = VisionType.sheetTitle(userFont),
                color = OnCanvas
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Reminders across your day, and what your widgets need.",
                style = rowNote(userFont),
                color = OnCanvasMuted
            )

            SectionHeading("NOTIFICATIONS")
            Block {
                SwitchRow(
                    title = "Allow notifications",
                    note = null,
                    checked = notificationsOn,
                    userFont = userFont,
                    onChange = { wanted ->
                        if (wanted && !systemAllows) {
                            // Ours is set either way: once Android says yes there is
                            // nothing else standing in the way, and the switch should
                            // not need a second press to mean what it already said.
                            master = true
                            preferences.notificationsEnabled = true
                            requestNotifications(context, askNotifications::launch)
                        } else {
                            master = wanted
                            preferences.notificationsEnabled = wanted
                            Reminders.sync(context)
                        }
                    }
                )
            }

            if (!notificationsOn) {
                Note("Notifications are off. Your clock widgets still update on their own.", userFont)
            }

            // Hidden rather than greyed out while nothing can be delivered. Three
            // switches that would change nothing are three questions the user has no
            // reason to be asked — the one that matters is the row above.
            if (notificationsOn) {
                SectionHeading("DAILY RHYTHM")

                ReminderBlock(
                    title = "Plan tomorrow",
                    note = "Set tomorrow's three before you sleep",
                    on = planOn,
                    onToggle = {
                        planOn = it
                        preferences.planTomorrowEnabled = it
                        Reminders.sync(context)
                    },
                    minutes = planAt,
                    onMinutes = {
                        planAt = it
                        preferences.planTomorrowMinutes = it
                        Reminders.sync(context)
                    },
                    expanded = openPicker == Reminder.PlanTomorrow,
                    onExpand = {
                        openPicker = if (openPicker == Reminder.PlanTomorrow) {
                            null
                        } else {
                            Reminder.PlanTomorrow
                        }
                    },
                    userFont = userFont
                )

                Spacer(Modifier.height(10.dp))
                ReminderBlock(
                    title = "Daily Wisdom",
                    note = "One line from your chosen theme",
                    on = wisdomOn,
                    onToggle = {
                        wisdomOn = it
                        preferences.dailyWisdomEnabled = it
                        Reminders.sync(context)
                    },
                    minutes = wisdomAt,
                    onMinutes = {
                        wisdomAt = it
                        preferences.dailyWisdomMinutes = it
                        Reminders.sync(context)
                    },
                    expanded = openPicker == Reminder.DailyWisdom,
                    onExpand = {
                        openPicker = if (openPicker == Reminder.DailyWisdom) {
                            null
                        } else {
                            Reminder.DailyWisdom
                        }
                    },
                    userFont = userFont
                )

                Spacer(Modifier.height(10.dp))
                ReminderBlock(
                    title = "Evening check",
                    note = "Only if something's left today",
                    on = eveningOn,
                    onToggle = {
                        eveningOn = it
                        preferences.eveningCheckEnabled = it
                        Reminders.sync(context)
                    },
                    minutes = eveningAt,
                    onMinutes = {
                        // Written through the preference, which is what holds it to the
                        // evening — a time picked earlier comes back as the earliest it
                        // is allowed to be.
                        preferences.eveningCheckMinutes = it
                        eveningAt = preferences.eveningCheckMinutes
                        Reminders.sync(context)
                    },
                    // The wheel is not offered the hours it would only refuse: see
                    // [AppPreferences.eveningCheckMinutes].
                    earliest = AppPreferences.EVENING_CHECK_EARLIEST,
                    expanded = openPicker == Reminder.EveningCheck,
                    onExpand = {
                        openPicker = if (openPicker == Reminder.EveningCheck) {
                            null
                        } else {
                            Reminder.EveningCheck
                        }
                    },
                    userFont = userFont
                )

                Note("Evening check stays silent once everything's done.", userFont)
            }

            SectionHeading("WIDGETS")
            Block {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openExactAlarmSettings(context) }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Alarms & reminders",
                            style = rowTitle(userFont),
                            color = OnCanvas
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = "Keeps your clock widgets updated on time.",
                            style = rowNote(userFont),
                            color = OnCanvasMuted
                        )
                    }
                    Text(
                        text = if (exactAlarms) "ALLOWED" else "OFF",
                        style = rowValue,
                        color = OnCanvasMuted
                    )
                    Spacer(Modifier.size(7.dp))
                    Text(text = "›", style = rowValue, color = OnCanvasMuted)
                }
            }

            Note(
                if (exactAlarms) {
                    "Your clock widgets update right on time."
                } else {
                    "Without it, your clock widgets may update a little late."
                },
                userFont
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Spacer(Modifier.height(18.dp))
    Text(text = text, style = VisionType.fieldCounter, color = OnCanvasMuted)
    Spacer(Modifier.height(7.dp))
}

/** One grouped card. The rows inside it are its own, divided rather than spaced. */
@Composable
private fun Block(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(BlockShape)
            .background(BlockFill)
    ) {
        content()
    }
}

@Composable
private fun Note(text: String, userFont: UserFontChoice) {
    Spacer(Modifier.height(7.dp))
    Text(
        text = text,
        style = rowNote(userFont),
        color = OnCanvasMuted,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    note: String?,
    checked: Boolean,
    userFont: UserFontChoice,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = rowTitle(userFont), color = OnCanvas)
            if (note != null) {
                Spacer(Modifier.height(1.dp))
                Text(text = note, style = rowNote(userFont), color = OnCanvasMuted)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Canvas,
                checkedTrackColor = OnCanvas,
                checkedBorderColor = OnCanvas,
                uncheckedThumbColor = Canvas,
                uncheckedTrackColor = OnCanvas.copy(alpha = 0.16f),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.scale(SwitchScale)
        )
    }
}

/**
 * One reminder: the switch, the time it is set for, and the wheel that sets it.
 *
 * The wheel opens inside the block rather than over it. These are three settings that
 * are read against each other — the evening check only makes sense beside the hour the
 * plan is written at — and a dialog over the page hides the other two at the moment the
 * user is choosing between them.
 */
@Composable
private fun ReminderBlock(
    title: String,
    note: String,
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    minutes: Int,
    onMinutes: (Int) -> Unit,
    expanded: Boolean,
    onExpand: () -> Unit,
    userFont: UserFontChoice,
    earliest: Int = 0
) {
    Block {
        SwitchRow(
            title = title,
            note = note,
            checked = on,
            userFont = userFont,
            onChange = onToggle
        )
        if (on) {
            HorizontalDivider(color = Rule, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpand)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time",
                    style = rowTitle(userFont),
                    color = OnCanvas,
                    modifier = Modifier.weight(1f)
                )
                Text(text = clockOf(minutes), style = rowValue, color = OnCanvas)
                Spacer(Modifier.size(5.dp))
                // The arrow is the only thing saying whether the wheel is out, so it
                // turns over rather than staying put.
                Text(
                    text = if (expanded) "▴" else "▾",
                    style = rowValue,
                    color = OnCanvasMuted
                )
            }
            if (expanded) {
                TimeWheel(
                    minutes = minutes,
                    earliest = earliest,
                    onPicked = onMinutes
                )
            }
        }
    }
}

private fun clockOf(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

/** How tall one row of the wheel is, and how many of them are on show at once. */
private val WheelRow = 34.dp
private const val WheelRows = 5
private val WheelHeight = WheelRow * WheelRows

/** The band the chosen figure sits in — the only part of the wheel that is not moving. */
private val BandFill = Color(0xFFF1EFEA)

/** The minute is offered in fives. A reminder is not an appointment. */
private const val MinuteStep = 5

/**
 * Two columns of figures that settle on whichever one is in the middle.
 *
 * Both halves are chosen the same way and read back together, so the hour is not
 * committed on its own: whichever column was moved, the time reported is what the two of
 * them now say between them.
 */
@Composable
private fun TimeWheel(minutes: Int, earliest: Int, onPicked: (Int) -> Unit) {
    val firstHour = earliest / 60
    val hours = remember(firstHour) { (firstHour..23).toList() }
    val minuteMarks = remember { (0 until 60 step MinuteStep).toList() }

    // The time showing may not be a figure the wheel offers — a default of 18:30 under a
    // five-minute step does, but a value carried over from an older build need not. The
    // nearest one it does offer is what the wheel opens on.
    val hour = minutes / 60
    val hourIndex = hours.indexOf(hour).coerceAtLeast(0)
    val minuteIndex = minuteMarks
        .indexOfFirst { it >= minutes % 60 }
        .let { if (it < 0) minuteMarks.lastIndex else it }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WheelHeight)
            .padding(horizontal = 14.dp)
            .padding(bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(WheelRow)
                .clip(RoundedCornerShape(9.dp))
                .background(BandFill)
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelColumn(
                values = hours,
                startIndex = hourIndex,
                modifier = Modifier.width(56.dp),
                onSettled = { onPicked(hours[it] * 60 + minuteMarks[minuteIndex]) }
            )
            Text(
                text = ":",
                style = wheelDigit,
                color = OnCanvas,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            WheelColumn(
                values = minuteMarks,
                startIndex = minuteIndex,
                modifier = Modifier.width(56.dp),
                onSettled = { onPicked(hours[hourIndex] * 60 + minuteMarks[it]) }
            )
        }
    }
}

/**
 * One column of the wheel.
 *
 * The padding above and below is half the column less half a row, which is what puts the
 * first value in the middle at rest — and it is also what makes [LazyListState] line an
 * item up with the band when it is scrolled to, so the snap below needs no arithmetic of
 * its own.
 *
 * The settle is done here rather than left to a fling behaviour: a flick that stops
 * between two figures has to land on one of them, and that has to happen whether the
 * movement ended in a fling or in a finger lifted mid-drag.
 */
@Composable
private fun WheelColumn(
    values: List<Int>,
    startIndex: Int,
    onSettled: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val edge = (WheelHeight - WheelRow) / 2

    val centred by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - middle) }
                ?.index
                ?: startIndex
        }
    }

    // Reported only once the column has come to rest. Writing on every frame of a flick
    // would set the reminder to every figure the wheel passed on the way.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { moving ->
                if (moving) return@collect
                val landed = centred
                listState.animateScrollToItem(landed)
                onSettled(landed)
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxHeight(),
        contentPadding = PaddingValues(vertical = edge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(values.size) { index ->
            val here = index == centred
            Box(
                modifier = Modifier.height(WheelRow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%02d".format(values[index]),
                    style = wheelDigit,
                    // The ones either side are still legible — that is how the user
                    // knows which way to move — but they are plainly not the answer.
                    color = if (here) OnCanvas else OnCanvas.copy(alpha = 0.28f)
                )
            }
        }
    }
}

/**
 * Asks Android for the notification permission, where there is one to ask for.
 *
 * Below Android 13 there is no runtime permission — notifications are on unless the user
 * has turned them off in the system settings, which is not something that can be asked
 * back by a dialog — so that page is opened instead.
 */
private fun requestNotifications(context: Context, launch: (String) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launch(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        openNotificationSettings(context)
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }.onFailure {
        openAppSettings(context)
    }
}

private fun openExactAlarmSettings(context: Context) {
    val intent = exactAlarmSettings(context)
    if (intent == null) {
        // Before Android 12 exact alarms were simply allowed, so there is no page for
        // this and nothing to grant — the app's own settings are the nearest thing.
        openAppSettings(context)
        return
    }
    runCatching { context.startActivity(intent) }.onFailure { openAppSettings(context) }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(android.net.Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
