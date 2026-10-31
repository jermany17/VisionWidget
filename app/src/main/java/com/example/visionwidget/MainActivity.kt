package com.example.visionwidget

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.visionwidget.data.AppPreferences
import com.example.visionwidget.ui.ScaledToDesign
import com.example.visionwidget.ui.navigation.VisionApp
import com.example.visionwidget.widget.OpenGalleryExtra
import com.example.visionwidget.widget.OpenVisionExtra

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

        // A Gallery or Vision widget's tap carries one of these; every other entry point
        // leaves both unset and lands on Today as it always has.
        val openGallery = intent.getBooleanExtra(OpenGalleryExtra, false)
        val openVision = intent.getBooleanExtra(OpenVisionExtra, false)

        setContent {
            // Every screen is measured against one width, so a phone that is narrower
            // than that gets the same drawing a little smaller rather than a rearranged
            // one with the labels broken across lines.
            ScaledToDesign {
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
