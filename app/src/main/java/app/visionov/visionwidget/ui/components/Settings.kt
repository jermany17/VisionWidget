package app.visionov.visionwidget.ui.components

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
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
 * What the gear opens.
 *
 * Shared rather than one per tab. Settings are the app's own, not a screen's, so the
 * gear has to mean the same thing wherever it is pressed — and there is only one of
 * these to fill in when there is finally something to put in it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(userFont: UserFontChoice, onDismiss: () -> Unit) {
    var showPaywall by rememberSaveable { mutableStateOf(false) }

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
        }
    }

    if (showPaywall) {
        PaywallSheet(userFont = userFont, onDismiss = { showPaywall = false })
    }
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

    when (val held = status) {
        is PlusStatus.Active -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PlanShape)
                .background(OnCanvas)
                .clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Plus.manageUrl(context.packageName).toUri())
                        )
                    }
                }
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column {
                Text(text = held.label, style = VisionType.sheetTitle(userFont), color = Canvas)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (held.autoRenewing) {
                        "MANAGE IN GOOGLE PLAY"
                    } else {
                        "CANCELLED · MANAGE IN GOOGLE PLAY"
                    },
                    style = VisionType.fieldLabel,
                    color = Canvas.copy(alpha = 0.55f)
                )
            }
            Text(
                text = "→",
                style = VisionType.glyph,
                color = Canvas,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
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
                    style = VisionType.sheetTitle(userFont),
                    color = OnCanvas
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = offers?.let { "${it.monthly.price}/MO OR ${it.yearly.price}/YR" }
                        ?: "EVERYTHING INCLUDED",
                    style = VisionType.fieldLabel,
                    color = OnCanvasMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(OnCanvas)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Text(text = "SEE PLANS", style = VisionType.fieldLabel, color = Canvas)
            }
        }
    }
}
