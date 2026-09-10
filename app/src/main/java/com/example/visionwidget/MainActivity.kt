package com.example.visionwidget

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.visionwidget.ui.navigation.VisionApp

class MainActivity : ComponentActivity() {
    // Re-armed on every foreground so the onboarding flow shows each time the app is
    // opened, not just on a cold start. Not persisted anywhere on purpose.
    private val showOnboarding = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The canvas is always light, so pin the system bar icons to dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            VisionApp(
                showOnboarding = showOnboarding.value,
                onFinishOnboarding = { showOnboarding.value = false }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        showOnboarding.value = true
    }
}
