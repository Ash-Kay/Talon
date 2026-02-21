package io.ashkay.talon.platform

import android.content.Context
import android.content.Intent
import io.ashkay.talon.accessibility.TalonAccessibilityService
import io.ashkay.talon.accessibility.commands.InstalledAppsHandler
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.AppInfo
import io.ashkay.talon.model.UiNode
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDeviceController(private val context: Context) : DeviceController {

  private val installedAppsHandler = InstalledAppsHandler(context)

  override suspend fun getUiTree(): UiNode? =
    withContext(Dispatchers.Main) {
      Napier.d(tag = TAG) { "Requesting UI tree" }
      val tree = TalonAccessibilityService.instance?.captureUiTree()
      if (tree == null) Napier.w(tag = TAG) { "UI tree is null — service may not be connected" }
      tree
    }

  override suspend fun execute(command: AgentCommand): Boolean =
    withContext(Dispatchers.Main) {
      Napier.d(tag = TAG) { "Executing command: $command" }
      val result = TalonAccessibilityService.instance?.performCommand(command) ?: false
      Napier.d(tag = TAG) { "Command result: $result" }
      result
    }

  override suspend fun getInstalledApps(): List<AppInfo> =
    withContext(Dispatchers.IO) {
      Napier.d(tag = TAG) { "Requesting installed apps" }
      installedAppsHandler.getInstalledApps()
    }

  override suspend fun launchApp(packageName: String): Boolean =
    withContext(Dispatchers.Main) {
      Napier.d(tag = TAG) { "Launching app: $packageName" }
      try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
          launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(launchIntent)
          true
        } else {
          Napier.w(tag = TAG) { "No launch intent for $packageName" }
          false
        }
      } catch (e: Exception) {
        Napier.e(tag = TAG, throwable = e) { "Failed to launch $packageName" }
        false
      }
    }

  companion object {
    private const val TAG = "AndroidDeviceCtrl"
  }
}
