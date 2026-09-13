package com.example.visionwidget

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.visionwidget.data.AppPreferences
import com.example.visionwidget.ui.navigation.VisionApp

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

        setContent {
            VisionApp(
                showOnboarding = showOnboarding.value,
                onFinishOnboarding = {
                    preferences.hasSeenOnboarding = true
                    showOnboarding.value = false
                }
            )
        }
    }
}
