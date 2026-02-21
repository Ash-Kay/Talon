package io.ashkay.talon.platform

import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.AppInfo
import io.ashkay.talon.model.UiNode

interface UiTreeProvider {
  suspend fun getUiTree(): UiNode?
}

interface CommandExecutor {
  suspend fun execute(command: AgentCommand): Boolean
}

interface AppListProvider {
  suspend fun getInstalledApps(): List<AppInfo>
}

interface AppLauncher {
  suspend fun launchApp(packageName: String): Boolean
}

interface DeviceController : UiTreeProvider, CommandExecutor, AppListProvider, AppLauncher
