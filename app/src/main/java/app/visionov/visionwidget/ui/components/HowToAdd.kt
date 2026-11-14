package app.visionov.visionwidget.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.visionov.visionwidget.R
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.VisionType

/**
 * One stop on the way to a widget — the picture, and what to do in it.
 *
 * The picture is a photograph of the launcher rather than a drawing of one, because the
 * thing being described belongs to the phone and not to this app: what the user is
 * looking for is a real row in a real sheet, and a stylised version of it would be a
 * second thing to recognise before finding the first.
 */
private data class HowToStep(
    @DrawableRes val picture: Int,
    val title: String,
    val note: String
)

/**
 * The four, in order.
 *
 * Worded for Samsung's launcher, which is what the pictures are taken on, and hedged
 * where the wording is the launcher's to choose — "Widgets" is called that on most but
 * not all of them, which is what [HowToStep.note] on the second step is for.
 */
private val HOW_TO_STEPS = listOf(
    HowToStep(
        picture = R.drawable.howto_1,
        title = "Touch and hold an empty spot on your Home screen",
        note = "Edit options appear at the bottom."
    ),
    HowToStep(
        picture = R.drawable.howto_2,
        title = "Tap Widgets",
        note = "The name can vary by phone."
    ),
    HowToStep(
        picture = R.drawable.howto_3,
        title = "Find Vision Widget",
        note = "Search or scroll to Vision Widget and tap it."
    ),
    HowToStep(
        picture = R.drawable.howto_4,
        title = "Pick a size, tap Add",
        note = "It goes straight onto your Home screen."
    )
)

/** The mount the picture sits on, a shade off the canvas as the plan row is. */
private val StageFill = Color(0xFFF2EFE9)
private val StageShape = RoundedCornerShape(20.dp)

private val DotSize = 6.dp
private val CurrentDotWidth = 20.dp

/** Back's own width, so Next keeps the rest of the row whichever step is showing. */
private val BackWidth = 96.dp

/**
 * What "Add widgets" opens — the four steps between here and a widget on the home screen.
 *
 * Walked rather than scrolled: each step is one thing to do and the next one only makes
 * sense once it is done, so the sheet shows one at a time and the way through is the
 * same pair of buttons throughout.
 *
 * The app cannot place a widget for the user here — a launcher only takes that from its
 * own picker — so this is a set of directions rather than a control, and it says so by
 * ending on "Got it" rather than on anything that sounds like it will do the work.
 */
@Composable
fun HowToAddPage(userFont: UserFontChoice, onBack: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val step = HOW_TO_STEPS[page]
    val last = page == HOW_TO_STEPS.lastIndex

    // Walking back out of the directions is the system gesture's job here, as it is on
    // every other page — see the back handler the host puts round this.
    Box(
        Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
    ) {
        // The picture takes what is left after the words and the buttons rather than
        // pushing them past the bottom edge — on a short screen the walk-through was
        // being cut off mid-button.
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "HOW TO ADD",
                    style = VisionType.sheetLabel,
                    color = OnCanvasMuted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "CLOSE",
                    style = VisionType.sheetLabel,
                    color = OnCanvasMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onBack)
                        .padding(start = 10.dp, top = 2.dp, bottom = 2.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "How to add a widget",
                style = VisionType.sheetTitle(userFont),
                color = OnCanvas
            )

            Spacer(Modifier.height(16.dp))
            Stage(
                picture = step.picture,
                page = page,
                count = HOW_TO_STEPS.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(Modifier.height(18.dp))
            Text(
                text = "STEP ${page + 1} OF ${HOW_TO_STEPS.size}",
                style = VisionType.fieldLabel,
                color = OnCanvasMuted
            )
            Spacer(Modifier.height(8.dp))
            // Two lines at most on a narrow phone, and the room for the second is held
            // either way, so the buttons under it don't move between steps.
            Text(
                text = step.title,
                style = VisionType.userFace(userFont, 19.sp, 25.sp, (-0.28).sp),
                color = OnCanvas,
                modifier = Modifier.heightIn(min = 50.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = step.note,
                style = VisionType.sheetLead(userFont),
                color = OnCanvasMuted
            )

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Nothing behind the first step, so Back isn't drawn there at all —
                // Next takes the whole row rather than sitting beside an empty slot.
                if (page > 0) {
                    WalkButton(
                        label = "Back",
                        userFont = userFont,
                        filled = false,
                        onClick = { page-- },
                        modifier = Modifier.width(BackWidth)
                    )
                }
                WalkButton(
                    label = if (last) "Got it" else "Next",
                    userFont = userFont,
                    filled = true,
                    onClick = { if (last) onBack() else page++ },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** The picture on its mount, with how far along drawn under it. */
@Composable
private fun Stage(
    @DrawableRes picture: Int,
    page: Int,
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(StageShape)
            .background(StageFill),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(picture),
            contentDescription = null,
            // Fitted, never cropped: a step that cut the thing being pointed at off the
            // side of the picture would be describing something the user cannot see.
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 14.dp, bottom = 28.dp)
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(count) { index ->
                val here = index == page
                Box(
                    modifier = Modifier
                        .size(width = if (here) CurrentDotWidth else DotSize, height = DotSize)
                        .clip(CircleShape)
                        .background(if (here) OnCanvas else OnCanvas.copy(alpha = 0.18f))
                )
            }
        }
    }
}

/**
 * One of the two ways through. Next is filled and Back is outlined, so the step forward
 * is the one the eye lands on — the way back is there to be found, not to be offered.
 */
@Composable
private fun WalkButton(
    label: String,
    userFont: UserFontChoice,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (filled) OnCanvas else Canvas)
            .then(
                if (filled) Modifier else Modifier.border(1.dp, OnCanvas.copy(alpha = 0.18f), shape)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = VisionType.sheetButton(userFont),
            color = if (filled) Canvas else OnCanvas
        )
    }
}
