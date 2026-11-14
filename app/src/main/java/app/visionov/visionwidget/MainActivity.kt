package app.visionov.visionwidget

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import app.visionov.visionwidget.data.AppPreferences
import app.visionov.visionwidget.ui.ScaledToDesign
import app.visionov.visionwidget.ui.gallery.PreviewExport
import kotlinx.coroutines.launch
import app.visionov.visionwidget.ui.navigation.VisionApp
import app.visionov.visionwidget.ui.onboarding.TutorialFlow
import app.visionov.visionwidget.widget.OpenGalleryExtra
import app.visionov.visionwidget.widget.OpenVisionExtra

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The canvas is always light, so pin the system bar icons to dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        // Read once per launch: the flag is already set by the time a rotation brings us
        // back through here, so the flow can't reappear mid-session.
        val preferences = AppPreferences(this)
        val showOnboarding = mutableStateOf(!preferences.hasSeenOnboarding)

        // Anyone already through onboarding when this arrived has been using the app
        // for weeks and does not want a tour of it. The flag is new, so it reads false
        // for them too — counted as seen here, once, rather than left to surprise them
        // on the next launch.
        if (preferences.hasSeenOnboarding && !preferences.hasSeenTutorial) {
            preferences.hasSeenTutorial = true
        }

        // The tour follows onboarding. Read once per launch like the flow above it, so
        // that a rotation part way through does not start it again.
        val showTutorial = mutableStateOf(!preferences.hasSeenTutorial)

        // A Gallery or Vision widget's tap carries one of these; every other entry point
        // leaves both unset and lands on Today as it always has.
        val openGallery = intent.getBooleanExtra(OpenGalleryExtra, false)
        val openVision = intent.getBooleanExtra(OpenVisionExtra, false)

        // Redraws the pictures the launcher's widget picker shows. Debug builds only,
        // and only when asked for by name — see [PreviewExport] for how to run it.
        if (intent.getBooleanExtra(PreviewExport.EXTRA, false)) {
            lifecycleScope.launch { PreviewExport.run(this@MainActivity) }
        }

        setContent {
            // Every screen is measured against one width, so a phone that is narrower
            // than that gets the same drawing a little smaller rather than a rearranged
            // one with the labels broken across lines.
            ScaledToDesign {
                // The tour sits between onboarding and the app: onboarding asks for a
                // goal, the tour shows what the app does with it, and only then is
                // there anything on the screens worth arriving at.
                if (!showOnboarding.value && showTutorial.value) {
                    TutorialFlow(
                        onFinish = {
                            preferences.hasSeenTutorial = true
                            showTutorial.value = false
                        }
                    )
                } else {
                    VisionApp(
                        showOnboarding = showOnboarding.value,
                        onFinishOnboarding = {
                            preferences.hasSeenOnboarding = true
                            showOnboarding.value = false
                        },
                        openGallery = openGallery,
                        openVision = openVision
                    )
                }
            }
        }
    }
}
