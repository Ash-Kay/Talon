package io.ashkay.talon.accessibility.commands

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.ashkay.talon.model.AppInfo
import io.github.aakira.napier.Napier

class InstalledAppsHandler(private val context: Context) {

  fun getInstalledApps(): List<AppInfo> {
    Napier.d(tag = TAG) { "Querying installed apps" }
    val pm = context.packageManager
    val packages =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION") pm.getInstalledPackages(0)
      }
    val apps =
      packages
        .map { packageInfo ->
          AppInfo(
            packageName = packageInfo.packageName,
            label =
              packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName,
          )
        }
        .sortedBy { it.label.lowercase() }
    Napier.d(tag = TAG) { "Found ${apps.size} installed apps" }
    return apps
  }

  companion object {
    private const val TAG = "InstalledAppsHandler"
  }
}
