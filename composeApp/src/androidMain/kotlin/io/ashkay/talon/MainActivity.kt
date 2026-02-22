package io.ashkay.talon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import io.ashkay.talon.accessibility.TalonAccessibilityService
import io.ashkay.talon.service.AgentForegroundService

class MainActivity : ComponentActivity() {
  private var isAccessibilityEnabled by mutableStateOf(false)
  private var isNotificationGranted by mutableStateOf(false)

  private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      isNotificationGranted = granted
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    isNotificationGranted = checkNotificationPermission()

    setContent {
      App(
        onOpenAccessibilitySettings = {
          startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        onStartForegroundService = { AgentForegroundService.start(this) },
        onStopForegroundService = { AgentForegroundService.stop(this) },
        onRequestNotificationPermission = { requestNotificationPermission() },
        isAccessibilityEnabled = isAccessibilityEnabled,
        isNotificationGranted = isNotificationGranted,
      )
    }
  }

  override fun onResume() {
    super.onResume()
    isAccessibilityEnabled = TalonAccessibilityService.isRunning
    isNotificationGranted = checkNotificationPermission()
  }

  private fun checkNotificationPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

  private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
      isNotificationGranted = true
    }
  }
}
