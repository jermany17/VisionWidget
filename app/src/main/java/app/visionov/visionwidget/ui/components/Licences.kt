package app.visionov.visionwidget.ui.components

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import app.visionov.visionwidget.ui.theme.Canvas
import app.visionov.visionwidget.ui.theme.DMMono
import app.visionov.visionwidget.ui.theme.OnCanvas
import app.visionov.visionwidget.ui.theme.OnCanvasMuted
import app.visionov.visionwidget.ui.theme.Rule
import app.visionov.visionwidget.ui.theme.UserFontChoice
import app.visionov.visionwidget.ui.theme.VisionType

/**
 * One thing the app was built out of, and the terms it came under.
 *
 * [asset] is a licence file packaged with the app rather than a page to go and fetch:
 * these have to be readable with no network, and a link that 404s years from now is not
 * an attribution. [url] is for the one component whose terms are not ours to bundle.
 */
private data class LicenceEntry(
    val name: String,
    /** The short tag on the row — "OFL 1.1". */
    val tag: String,
    /** What the detail page is headed with — "FONT · SIL OPEN FONT LICENSE 1.1". */
    val heading: String,
    val asset: String? = null,
    val url: String? = null,
    /**
     * Whether the file's first line names the copyright holder, as every OFL.txt does
     * and the shared Apache text does not — it is one file standing for several
     * libraries, so it carries no one's name.
     */
    val copyrightOnFirstLine: Boolean = false
)

private const val OFL_HEADING = "FONT · SIL OPEN FONT LICENSE 1.1"

private fun font(name: String, key: String) = LicenceEntry(
    name = name,
    tag = "OFL 1.1",
    heading = OFL_HEADING,
    asset = "font_licenses/${key}_OFL.txt",
    copyrightOnFirstLine = true
)

/**
 * The nineteen faces the app can be set in, each under the Open Font Licence.
 *
 * Listed in the order they are offered in rather than alphabetically: the list is read
 * beside the picker, and two different orders for the same nineteen names is one of them
 * being wrong wherever it is looked at.
 */
private val FONT_LICENCES = listOf(
    font("Instrument Serif", "instrument_serif"),
    font("DM Sans", "dm_sans"),
    font("DM Mono", "dm_mono"),
    font("Libre Baskerville", "libre_baskerville"),
    font("Newsreader", "newsreader"),
    font("Lora", "lora"),
    font("EB Garamond", "eb_garamond"),
    font("Crimson Pro", "crimson_pro"),
    font("Playfair Display", "playfair_display"),
    font("Fraunces", "fraunces"),
    font("Space Grotesk", "space_grotesk"),
    font("Bricolage Grotesque", "bricolage_grotesque"),
    font("Outfit", "outfit"),
    font("Work Sans", "work_sans"),
    font("Public Sans", "public_sans"),
    font("Manrope", "manrope"),
    font("Syne", "syne"),
    font("IBM Plex Mono", "ibm_plex_mono"),
    font("JetBrains Mono", "jetbrains_mono")
)

private fun apache(name: String) = LicenceEntry(
    name = name,
    tag = "APACHE 2.0",
    heading = "APACHE LICENSE 2.0",
    asset = "licenses/apache_2_0.txt"
)

/** The libraries actually linked into the app, named as their projects are. */
private val SOFTWARE_LICENCES = listOf(
    apache("AndroidX Core"),
    apache("AndroidX Lifecycle"),
    apache("AndroidX Activity"),
    apache("Jetpack Compose"),
    apache("Jetpack Glance"),
    apache("AndroidX Room"),
    apache("Kotlin")
)

/**
 * The one bundled piece that is not open source.
 *
 * Play's billing library ships under Google's SDK terms rather than Apache 2.0 — its own
 * POM says so — and those terms are Google's to publish, not ours to copy into the app.
 * So this row links out where every other row opens a file, and is kept out of the
 * open-source list rather than being labelled as something it is not.
 */
private val PLAY_BILLING = LicenceEntry(
    name = "Google Play Billing Library",
    tag = "ANDROID SDK",
    heading = "ANDROID SOFTWARE DEVELOPMENT KIT LICENSE",
    url = "https://developer.android.com/studio/terms"
)

/** The licence text itself, which is read rather than skimmed, so it stays monospaced. */
private val LicenceBody = TextStyle(
    fontFamily = DMMono,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 17.sp
)

private val CopyrightShape = RoundedCornerShape(10.dp)

/**
 * What the LICENCES link opens — everything the app was made out of that someone else
 * made first.
 *
 * Kept inside the app rather than pointed at the web, and read out of files packaged
 * with it: an attribution that needs a network to be seen is one that disappears on a
 * plane, and the obligation does not.
 */
@Composable
fun LicencesPage(userFont: UserFontChoice, onBack: () -> Unit) {
    // Which entry is open, by name. The name rather than the entry itself, so the choice
    // survives the process being killed in the background and restored.
    var openName by rememberSaveable { mutableStateOf<String?>(null) }
    val open = remember(openName) {
        (FONT_LICENCES + SOFTWARE_LICENCES + PLAY_BILLING).firstOrNull { it.name == openName }
    }

    // One licence open is a page of its own within this one, so the system's back goes
    // up a level rather than straight out of the licences altogether.
    BackHandler(enabled = open != null) { openName = null }

    Box(
        Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
                .padding(top = 14.dp, bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            if (open == null) {
                LicenceList(
                    userFont = userFont,
                    onBack = onBack,
                    onOpen = { openName = it.name }
                )
            } else {
                LicenceDetail(
                    entry = open,
                    userFont = userFont,
                    onBack = { openName = null }
                )
            }
        }
    }
}

/** Every entry, in its two groups. */
@Composable
private fun LicenceList(
    userFont: UserFontChoice,
    onBack: () -> Unit,
    onOpen: (LicenceEntry) -> Unit
) {
    val context = LocalContext.current

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = "‹  Settings",
            style = VisionType.userFace(userFont, 13.sp, 17.sp),
            color = OnCanvasMuted,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onBack)
                .padding(vertical = 4.dp, horizontal = 2.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Licences",
            style = VisionType.sheetTitle(userFont),
            color = OnCanvas
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "The typefaces and open-source software inside Vision Widget.",
            style = VisionType.sheetLead(userFont),
            color = OnCanvasMuted
        )

        GroupHeading("FONTS · ${FONT_LICENCES.size}")
        HorizontalDivider(color = Rule, thickness = 1.dp)
        FONT_LICENCES.forEach {
            LicenceRow(entry = it, userFont = userFont, onClick = { onOpen(it) })
            HorizontalDivider(color = Rule, thickness = 1.dp)
        }

        GroupHeading("OPEN-SOURCE SOFTWARE")
        HorizontalDivider(color = Rule, thickness = 1.dp)
        SOFTWARE_LICENCES.forEach {
            LicenceRow(entry = it, userFont = userFont, onClick = { onOpen(it) })
            HorizontalDivider(color = Rule, thickness = 1.dp)
        }

        GroupHeading("BUNDLED SDK")
        HorizontalDivider(color = Rule, thickness = 1.dp)
        LicenceRow(
            entry = PLAY_BILLING,
            userFont = userFont,
            onClick = { openUrl(context, PLAY_BILLING.url) }
        )
        HorizontalDivider(color = Rule, thickness = 1.dp)

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Thank you to everyone who made these freely available.",
            style = VisionType.sheetLead(userFont),
            color = OnCanvasMuted
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GroupHeading(text: String) {
    Spacer(Modifier.height(26.dp))
    Text(text = text, style = VisionType.fieldLabel, color = OnCanvasMuted)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun LicenceRow(entry: LicenceEntry, userFont: UserFontChoice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = entry.name,
            style = VisionType.sheetRowField(userFont),
            color = OnCanvas,
            modifier = Modifier.weight(1f)
        )
        Text(text = entry.tag, style = VisionType.fieldCounter, color = OnCanvasMuted)
        Spacer(Modifier.height(0.dp))
        Text(
            text = "  →",
            style = VisionType.fieldCounter,
            color = OnCanvasMuted
        )
    }
}

/**
 * One licence in full.
 *
 * The text is read off the packaged file every time rather than held somewhere: it is a
 * few kilobytes, read once when a row is tapped, and a copy kept in memory for the life
 * of the process would be a copy that can drift from the file it is quoting.
 */
@Composable
private fun LicenceDetail(entry: LicenceEntry, userFont: UserFontChoice, onBack: () -> Unit) {
    val context = LocalContext.current
    val text = remember(entry.name) { readAsset(context, entry.asset) }

    // An OFL file opens with the copyright line; the shared Apache text does not, and
    // splitting it the same way would lift its first sentence out as if it were one.
    val copyright = if (entry.copyrightOnFirstLine) {
        text.lineSequence().firstOrNull { it.isNotBlank() }
    } else {
        null
    }
    val body = if (copyright != null) text.substringAfter(copyright).trim() else text.trim()

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = "‹  LICENCES",
            style = VisionType.sheetLabel,
            color = OnCanvasMuted,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onBack)
                .padding(vertical = 4.dp, horizontal = 2.dp)
        )

        Spacer(Modifier.height(16.dp))
        Text(text = entry.heading, style = VisionType.fieldLabel, color = OnCanvasMuted)
        Spacer(Modifier.height(8.dp))
        Text(text = entry.name, style = VisionType.sheetTitle(userFont), color = OnCanvas)

        if (copyright != null) {
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(CopyrightShape)
                    .border(1.dp, Rule, CopyrightShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(text = copyright, style = LicenceBody, color = OnCanvas)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(text = body, style = LicenceBody, color = OnCanvas)
        Spacer(Modifier.height(24.dp))
    }
}

/** The packaged licence file, or a line saying so if it could not be opened. */
private fun readAsset(context: Context, path: String?): String {
    if (path == null) return ""
    return runCatching {
        context.assets.open(path).bufferedReader().use { it.readText() }
    }.getOrElse { "This licence could not be read from the app package." }
}

private fun openUrl(context: Context, url: String?) {
    if (url == null) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
