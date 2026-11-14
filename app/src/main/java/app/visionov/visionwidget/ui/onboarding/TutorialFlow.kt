package app.visionov.visionwidget.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.visionov.visionwidget.R
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.UserFonts
import app.visionov.visionwidget.ui.theme.VisionType

/**
 * The tour of the app, shown once when onboarding is done.
 *
 * Nine pictures of the app itself, each with its own card already drawn on it. Nothing
 * here is live: the point is to show what the screens do before the user has anything on
 * them worth looking at, and a tour that drove the real screens would have nothing to
 * show on the first run — no vision, no three, no widget.
 *
 * So the only thing this draws is the way through: back, how far along, and next. The
 * pictures carry the rest.
 */

/**
 * The nine, in order.
 *
 * Numbered rather than named because they are one sequence rather than nine subjects —
 * what each one says is written on the picture, and a name here would be a second place
 * to keep it current.
 */
private val TUTORIAL_PAGES = listOf(
    R.drawable.tutorial_1,
    R.drawable.tutorial_2,
    R.drawable.tutorial_3,
    R.drawable.tutorial_4,
    R.drawable.tutorial_5,
    R.drawable.tutorial_6,
    R.drawable.tutorial_7,
    R.drawable.tutorial_8,
    R.drawable.tutorial_9
)

/** The bar the way through is drawn on, under the picture. */
private val BarFill = Color(0xFF111111)
private val BarInk = Color(0xFFF2EDE6)
private val BarInkMuted = BarInk.copy(alpha = 0.45f)

/** The step showing, as a filled lozenge; the rest as dots. */
private val DotSize = 6.dp
private val CurrentDotWidth = 22.dp
private val DotGap = 6.dp

private val BarHeight = 96.dp

/**
 * Back and next are the same width, which is what puts the steps between them on the
 * middle of the screen as well as on the middle of the gap. Two buttons of different
 * widths make those two centres different places, and whichever one the dots were put
 * on, they read as being slightly off the other.
 */
private val ButtonWidth = 84.dp

@Composable
fun TutorialFlow(onFinish: () -> Unit) {
    // The screen's own text, like every other screen that is not a widget, is set in the
    // default face rather than in whatever the widgets were dressed in.
    val userFont = UserFonts[UserFonts.DEFAULT_ID]

    var page by rememberSaveable { mutableIntStateOf(0) }
    val last = page == TUTORIAL_PAGES.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BarFill)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TutorialPicture(
                page = TUTORIAL_PAGES[page],
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            TutorialBar(
                page = page,
                count = TUTORIAL_PAGES.size,
                last = last,
                userFont = userFont,
                onBack = { if (page > 0) page-- },
                onNext = { if (last) onFinish() else page++ }
            )
        }
    }
}

/**
 * One picture, sat under the status bar rather than behind it.
 *
 * These are photographs of the app taken on a phone, so each already has a top edge of
 * its own. Run full bleed they would meet the real status bar and the clock would be
 * shown twice, a few minutes apart.
 */
@Composable
private fun TutorialPicture(@DrawableRes page: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(page),
            contentDescription = null,
            // Fitted, never cropped. A tour whose card is half off the side of the
            // screen on a narrow phone is worse than one with a margin on a wide one.
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Back, how far along, and next — the only part of this screen that does anything. */
@Composable
private fun TutorialBar(
    page: Int,
    count: Int,
    last: Boolean,
    userFont: UserFontChoice,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BarFill)
            .padding(bottom = navInset)
            .height(BarHeight)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left, centre and right are each given a third, and the two buttons are the
        // same width, so the steps land on the middle of the screen and on the middle
        // of the gap between the buttons at once — those are the same place only while
        // the buttons match.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            // Nothing to go back to on the first picture. The slot is still spent, so
            // that the steps do not slide across as it appears on the second.
            if (page > 0) {
                BarButton(label = "Back", userFont = userFont, onClick = onBack)
            }
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Progress(page = page, count = count)
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            BarButton(
                label = if (last) "Done" else "Next",
                userFont = userFont,
                onClick = onNext
            )
        }
    }
}

/**
 * Back and next, which are the same button with a different word in them.
 *
 * Held well under the scale the app's own buttons are set at. Those are the thing being
 * pressed on the screen they belong to; these sit under a picture that is the whole
 * point of the screen, and at the app's size they were competing with it.
 */
@Composable
private fun BarButton(label: String, userFont: UserFontChoice, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(ButtonWidth)
            .clip(RoundedCornerShape(percent = 50))
            .background(BarInk)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = VisionType.userFace(userFont, 13.sp, 17.sp),
            color = BarFill
        )
    }
}

/** The dots, and the same thing said in figures under them. */
@Composable
private fun Progress(page: Int, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(DotGap)) {
            repeat(count) { index ->
                val here = index == page
                Box(
                    modifier = Modifier
                        .size(width = if (here) CurrentDotWidth else DotSize, height = DotSize)
                        .clip(CircleShape)
                        .background(if (here) BarInk else BarInkMuted)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // The dots alone stop being countable at nine, so the figure says it outright.
        Text(
            text = "${page + 1} / $count",
            style = VisionType.eyebrow,
            color = BarInkMuted
        )
    }
}
