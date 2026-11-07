package app.visionov.visionwidget.billing

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.VisionType

/** The sheet is its own dark panel, not the app's canvas — it is the one paid moment. */
private val PaywallSurface = Color(0xFF1C1A17)
private val PaywallInk = Color(0xFFF7F5F1)
private val PaywallInkMuted = PaywallInk.copy(alpha = 0.55f)
private val PlanBorder = PaywallInk.copy(alpha = 0.16f)
private val PlanBorderChosen = PaywallInk.copy(alpha = 0.85f)

private val PlanShape = RoundedCornerShape(16.dp)

/**
 * What a locked feature opens instead of unlocking.
 *
 * Every Plus-marked control in the app comes here rather than explaining itself where
 * it stands: the offer is one thing and should read as one thing, wherever it is
 * reached from. Nothing is listed here that the Console doesn't price — the two plans
 * are drawn from [BillingManager.offers], so a price change in the Console is a price
 * change in the app without a release.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(userFont: UserFontChoice, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val billing = remember(context) { BillingManager.get(context) }
    val offers by billing.offers.collectAsStateWithLifecycle()
    val status by billing.status.collectAsStateWithLifecycle()

    // Yearly first, which is the one worth taking and the one the sheet quotes.
    var chosen by rememberSaveable { mutableStateOf(Plus.YEARLY) }

    // Bought: the thing this sheet asks for has happened, so it has nothing left to say.
    if (status.isActive) onDismiss()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PaywallSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = PaywallInkMuted) }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "VISION+ · EVERYTHING INCLUDED",
                style = VisionType.sheetLabel,
                color = PaywallInkMuted
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "The vision is free.\nMaking it yours is Vision+.",
                style = VisionType.sheetTitle(userFont),
                color = PaywallInk
            )

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth()) {
                BenefitColumn(
                    heading = "DESIGN",
                    lines = listOf(
                        "All Premium Collections",
                        "Premium Typography",
                        "Your Own Colour",
                        "Glass Widgets"
                    ),
                    userFont = userFont,
                    modifier = Modifier.weight(1f)
                )
                BenefitColumn(
                    heading = "EXPERIENCE",
                    lines = listOf("Gallery Layouts", "Your Own Quotes", "Multiple Visions"),
                    userFont = userFont,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlanCard(
                    eyebrow = "MONTHLY",
                    price = offers?.monthly?.price,
                    note = "per month",
                    chosen = chosen == Plus.MONTHLY,
                    userFont = userFont,
                    onClick = { chosen = Plus.MONTHLY },
                    modifier = Modifier.weight(1f)
                )
                PlanCard(
                    eyebrow = "YEARLY",
                    price = offers?.yearly?.price,
                    note = "per year",
                    chosen = chosen == Plus.YEARLY,
                    userFont = userFont,
                    onClick = { chosen = Plus.YEARLY },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))
            val price = when (chosen) {
                Plus.MONTHLY -> offers?.monthly?.price
                else -> offers?.yearly?.price
            }
            // Dead until Play has answered. A button that opens an empty sheet is worse
            // than one that waits, and the wait is a second at worst.
            val ready = price != null && activity != null
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (ready) PaywallInk else PaywallInk.copy(alpha = 0.25f))
                    .clickable(enabled = ready) {
                        activity?.let { billing.purchase(it, chosen) }
                    }
                    .padding(vertical = 17.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (price == null) "Loading…" else "Subscribe · $price",
                    style = VisionType.sheetButton(userFont),
                    color = if (ready) PaywallSurface else PaywallSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Renews automatically until cancelled. Charged to your Google " +
                    "Account at confirmation. Cancel anytime in Play Store settings.",
                style = VisionType.sheetLead(userFont).copy(fontSize = 11.sp, lineHeight = 15.sp),
                color = PaywallInkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Not now",
                style = VisionType.sheetCancel(userFont),
                color = PaywallInkMuted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun BenefitColumn(
    heading: String,
    lines: List<String>,
    userFont: UserFontChoice,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(text = heading, style = VisionType.fieldLabel, color = PaywallInkMuted)
        Spacer(Modifier.height(10.dp))
        lines.forEach {
            Text(text = it, style = VisionType.sheetLead(userFont), color = PaywallInk)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * One of the two plans. The chosen one is outlined rather than filled: a filled card
 * beside the button under it would read as a second button.
 */
@Composable
private fun PlanCard(
    eyebrow: String,
    price: String?,
    note: String,
    chosen: Boolean,
    userFont: UserFontChoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(PlanShape)
            .border(1.dp, if (chosen) PlanBorderChosen else PlanBorder, PlanShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = eyebrow, style = VisionType.fieldLabel, color = PaywallInkMuted)
        Spacer(Modifier.height(8.dp))
        Text(
            text = price ?: "—",
            style = VisionType.sheetGoalField(userFont),
            color = PaywallInk
        )
        Spacer(Modifier.height(4.dp))
        Text(text = note, style = VisionType.sheetLead(userFont), color = PaywallInkMuted)
    }
}
