package io.ashkay.talon

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import io.ashkay.talon.platform.AndroidOverlayUiController
import io.ashkay.talon.platform.OverlayUiController
import io.ashkay.talon.service.AgentForegroundService
import org.koin.mp.KoinPlatform

class MainActivity : ComponentActivity() {
  private var isAccessibilityEnabled by mutableStateOf(false)
  private var isOverlayEnabled by mutableStateOf(false)
  private var isNotificationGranted by mutableStateOf(false)

  private val notificationPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      isNotificationGranted = granted
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    isNotificationGranted = checkNotificationPermission()
    isOverlayEnabled = checkOverlayPermission()

    setContent {
      App(
        onOpenAccessibilitySettings = {
          startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        onOpenOverlaySettings = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(
              Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
          }
        },
        onStartForegroundService = { AgentForegroundService.start(this) },
        onStopForegroundService = { AgentForegroundService.stop(this) },
        onRequestNotificationPermission = { requestNotificationPermission() },
        isAccessibilityEnabled = isAccessibilityEnabled,
        isOverlayEnabled = isOverlayEnabled,
        isNotificationGranted = isNotificationGranted,
        onShowOverlay = { sessionId -> showOverlay(sessionId) },
        onHideOverlay = { hideOverlay() },
        onCancelAgent = { hideOverlay() },
        registerCancelAgent = { cancelFn ->
          AndroidOverlayUiController.stopAgentCallback = cancelFn
        },
      )
    }
  }

  override fun onResume() {
    super.onResume()
    isAccessibilityEnabled = TalonAccessibilityService.isRunning
    isOverlayEnabled = checkOverlayPermission()
    isNotificationGranted = checkNotificationPermission()
  }

  private fun showOverlay(sessionId: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
      return
    }
    val overlayController = KoinPlatform.getKoin().get<OverlayUiController>()
    overlayController.show(sessionId)
  }

  private fun hideOverlay() {
    val overlayController = KoinPlatform.getKoin().get<OverlayUiController>()
    overlayController.hide()
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

  private fun checkOverlayPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Settings.canDrawOverlays(this)
    } else {
      true
    }
}
