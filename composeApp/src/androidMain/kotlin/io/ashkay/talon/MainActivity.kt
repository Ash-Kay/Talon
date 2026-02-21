package io.ashkay.talon

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ashkay.talon.accessibility.TalonAccessibilityService
import io.ashkay.talon.service.AgentForegroundService

class MainActivity : ComponentActivity() {
  private var isAccessibilityEnabled by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      App(
        onOpenAccessibilitySettings = {
          startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        onStartForegroundService = { AgentForegroundService.start(this) },
        onStopForegroundService = { AgentForegroundService.stop(this) },
        isAccessibilityEnabled = isAccessibilityEnabled,
      )
    }
  }

  override fun onResume() {
    super.onResume()
    isAccessibilityEnabled = TalonAccessibilityService.isRunning
  }
}
