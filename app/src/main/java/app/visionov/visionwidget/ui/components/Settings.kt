package app.visionov.visionwidget.ui.components

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.visionov.visionwidget.R
import app.visionov.visionwidget.billing.BillingManager
import app.visionov.visionwidget.billing.PaywallSheet
import app.visionov.visionwidget.billing.Plus
import app.visionov.visionwidget.billing.PlusStatus
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.Rule
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.VisionType

/** The disc the gear sits on — fixed, so it reads the same over every screen. */
private val IconButtonFill = Color(0xFFEFEBE3)
private val IconButtonBorder = Color(0xFF12110F).copy(alpha = 0.09f)

/**
 * The band the gear is centred in — one line of the title face, whatever the header
 * around it is made of.
 *
 * Vision and Studio head their tabs with a single line and Today and Insights with an
 * eyebrow above one, so a gear centred on its own header sits at two different heights
 * and steps down the screen as the tabs are switched. Centring it on the first line's
 * height instead puts it in the same place on all four.
 */
private val TitleLine = 40.dp

/** The plan row's own corner and fill, a shade off the canvas so the row reads as one. */
private val PlanShape = RoundedCornerShape(18.dp)
private val PlanRowFill = Color(0xFFF2EFE9)

/** Where a question goes when "Contact us" is tapped. */
private const val SUPPORT_EMAIL = "support@visionov.app"
private const val PRIVACY_URL = "https://visionov.app/privacy/"
private const val TERMS_URL = "https://visionov.app/terms/"

/**
 * The list's own, smaller scale.
 *
 * [VisionType.sheetTitle] is sized for a sheet's one headline — "Get Vision+" and four
 * row names at that size read as four headlines stacked rather than as a list, which is
 * what every other label on the sheet was already sized down from [VisionType.sheetLabel]
 * to avoid. Kept local rather than folded into [VisionType]: nothing else in the app
 * reads a row of names this way.
 */
private fun settingsRowTitle(font: UserFontChoice) = VisionType.userFace(font, 18.sp, 22.sp, (-0.3).sp)
private val settingsHint = VisionType.fieldCounter
private val settingsButtonLabel = VisionType.fieldLabel.copy(fontSize = 9.sp, letterSpacing = 1.4.sp)
private val settingsGlyph = VisionType.glyph.copy(fontSize = 13.sp, lineHeight = 14.sp)

/** The gear, at the head of every tab. */
@Composable
fun SettingsButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.height(TitleLine),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(IconButtonFill)
                .border(1.dp, IconButtonBorder, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = OnCanvas,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/**
 * Where each row of the settings sheet goes.
 *
 * Every one of these opens a page rather than another panel, and a page belongs to the
 * app rather than to the sheet — it has to cover the tab bar, which the sheet is drawn
 * under. So the sheet asks and the host answers; see [VisionApp].
 *
 * Carried as one object rather than as four parameters because it is one idea, and
 * because the four screens that put the gear on their header would otherwise each be
 * threading four lambdas they never look at.
 */
data class SettingsRoutes(
    /** How many designs are kept, for the My Library row's own count. */
    val libraryCount: Int = 0,
    val openLibrary: () -> Unit = {},
    val openHowToAdd: () -> Unit = {},
    val openPermissions: () -> Unit = {},
    val openLicences: () -> Unit = {}
)

/**
 * What the gear opens.
 *
 * Shared rather than one per tab. Settings are the app's own, not a screen's, so the
 * gear has to mean the same thing wherever it is pressed — and there is only one of
 * these to fill in when there is finally something to put in it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    userFont: UserFontChoice,
    routes: SettingsRoutes = SettingsRoutes(),
    onDismiss: () -> Unit
) {
    var showPaywall by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    /** Every row that opens a page takes the sheet down behind it on the way. */
    fun leaveFor(open: () -> Unit) {
        open()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Canvas,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(text = "SETTINGS", style = VisionType.sheetLabel, color = OnCanvasMuted)
            Spacer(Modifier.height(14.dp))
            PlanRow(userFont = userFont, onSeePlans = { showPaywall = true })

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Rule, thickness = 1.dp)
            SettingsRow(
                userFont = userFont,
                title = "Add widgets",
                trailingLabel = "How to add",
                onClick = { leaveFor(routes.openHowToAdd) }
            )
            HorizontalDivider(color = Rule, thickness = 1.dp)
            SettingsRow(
                userFont = userFont,
                title = "My Library",
                trailingLabel = if (routes.libraryCount == 1) {
                    "1 saved"
                } else {
                    "${routes.libraryCount} saved"
                },
                onClick = { leaveFor(routes.openLibrary) }
            )
            HorizontalDivider(color = Rule, thickness = 1.dp)
            SettingsRow(
                userFont = userFont,
                title = "Permissions & access",
                onClick = { leaveFor(routes.openPermissions) }
            )
            HorizontalDivider(color = Rule, thickness = 1.dp)
            SettingsRow(
                userFont = userFont,
                title = "Contact us",
                trailingLabel = "Email",
                onClick = { sendSupportEmail(context) }
            )
            HorizontalDivider(color = Rule, thickness = 1.dp)

            Spacer(Modifier.height(14.dp))
            SettingsFooter(onLicences = { leaveFor(routes.openLicences) })
        }
    }

    // The one thing here that stays a panel. Buying is a decision taken against the
    // row that offered it, and a page would take that row off the screen.
    if (showPaywall) {
        PaywallSheet(userFont = userFont, onDismiss = { showPaywall = false })
    }
}

/**
 * One row of the settings list — a name, and what tapping it does or will do.
 *
 * Add widgets and Permissions & access have nothing behind them yet, so they take no
 * [onClick] and sit inert with their arrow still drawn: the row is there to be found
 * later rather than promising something today's build can't open.
 */
@Composable
private fun SettingsRow(
    userFont: UserFontChoice,
    title: String,
    trailingLabel: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = settingsRowTitle(userFont),
            color = OnCanvas,
            modifier = Modifier.weight(1f)
        )
        if (trailingLabel != null) {
            Text(text = trailingLabel, style = settingsHint, color = OnCanvasMuted)
            Spacer(Modifier.width(6.dp))
        }
        Text(text = "→", style = settingsGlyph, color = OnCanvasMuted)
    }
}

/**
 * Opens whatever the phone reads mail with, addressed and subjected already.
 *
 * `ACTION_SENDTO` with a bare `mailto:` scheme rather than `ACTION_SEND`: `SEND` offers
 * the message to every app that can share text at all — notes apps, messengers, Bluetooth
 * — where `SENDTO` on `mailto:` is a filter Android resolves to mail apps alone, which is
 * the one the OS defines for exactly this. The app's own version rides along in the body
 * so a report doesn't start with "what version are you on."
 */
private fun sendSupportEmail(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
    }
    runCatching { context.startActivity(intent) }
}

/** Version on the left, what governs it on the right — the app's own small print. */
@Composable
private fun SettingsFooter(onLicences: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = PackageInfoCompat.getLongVersionCode(info)
            "${info.versionName} (build $code) ·\nAndroid ${Build.VERSION.RELEASE}"
        }.getOrDefault("")
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            text = version,
            style = settingsHint,
            color = OnCanvasMuted,
            modifier = Modifier.weight(1f)
        )
        val context = LocalContext.current
        // The two that are published go out to the browser; the third is ours and is
        // kept inside the app, where it can still be read with no network.
        LegalLink(label = "PRIVACY") { openLegalUrl(context, PRIVACY_URL) }
        LegalLink(label = "TERMS") { openLegalUrl(context, TERMS_URL) }
        LegalLink(label = "LICENCES", onClick = onLicences)
    }
}

private fun openLegalUrl(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

/** PRIVACY · TERMS · LICENCES — underlined, because a colour here would only be a style. */
@Composable
private fun LegalLink(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = settingsHint.copy(textDecoration = TextDecoration.Underline),
        color = OnCanvasMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, bottom = 1.dp)
    )
}

/**
 * Which plan the user is on, and the way out of it either direction.
 *
 * This is where the plan is read now — the Studio header used to carry a chip saying
 * "FREE PLAN", which named the state on one tab only and had nowhere to go when it
 * changed. Settings is the app's, as the gear beside it is.
 *
 * Subscribed, the row hands off to Play rather than offering to cancel: a subscription
 * is managed where it is billed, and an in-app cancel button that only deep-links is a
 * button pretending to do something.
 */
@Composable
private fun PlanRow(userFont: UserFontChoice, onSeePlans: () -> Unit) {
    val context = LocalContext.current
    val billing = remember(context) { BillingManager.get(context) }
    val status by billing.status.collectAsStateWithLifecycle()
    val offers by billing.offers.collectAsStateWithLifecycle()

    // Asked every time the sheet opens: a subscription can have ended somewhere the app
    // was never running to hear about it.
    LaunchedEffect(Unit) { billing.refresh() }

    val openManage = {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Plus.manageUrl(context.packageName).toUri())
            )
        }
        Unit
    }

    when (val held = status) {
        // Renewing: the row that's paying for itself, filled solid to read as the
        // settled state of the three.
        is PlusStatus.Active -> if (held.autoRenewing) Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PlanShape)
                .background(OnCanvas)
                .clickable(onClick = openManage)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column {
                Text(text = held.label, style = settingsRowTitle(userFont), color = Canvas)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "MANAGE IN GOOGLE PLAY",
                    style = settingsButtonLabel,
                    color = Canvas.copy(alpha = 0.55f)
                )
            }
            Text(
                text = "→",
                style = settingsGlyph,
                color = Canvas,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        } else {
            // Cancelled but not yet expired: outlined rather than filled, so a plan
            // winding down doesn't read as identical to one that's settled and renewing.
            // Play doesn't hand the client an expiry date to name here — that's the
            // Developer API's to know, not the Billing Library's — so this says only
            // what the client actually knows: it won't renew.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PlanShape)
                    .background(Canvas)
                    .border(1.2.dp, OnCanvas.copy(alpha = 0.18f), PlanShape)
                    .clickable(onClick = openManage)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(text = held.label, style = settingsRowTitle(userFont), color = OnCanvas)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "WON'T RENEW · MANAGE IN GOOGLE PLAY",
                        style = settingsButtonLabel,
                        color = OnCanvasMuted
                    )
                }
                Text(
                    text = "→",
                    style = settingsGlyph,
                    color = OnCanvasMuted,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        // Not held, or not known yet. Both offer the same way in: asking before Play has
        // answered is harmless, since the sheet waits for the prices itself.
        else -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PlanShape)
                .background(PlanRowFill)
                .clickable(onClick = onSeePlans)
                .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Get Vision+",
                    style = settingsRowTitle(userFont),
                    color = OnCanvas
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = offers?.let { "${it.monthly.price}/MO OR ${it.yearly.price}/YR" }
                        ?: "EVERYTHING INCLUDED",
                    style = settingsHint,
                    color = OnCanvasMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(OnCanvas)
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                Text(text = "SEE PLANS", style = settingsButtonLabel, color = Canvas)
            }
        }
    }
}
