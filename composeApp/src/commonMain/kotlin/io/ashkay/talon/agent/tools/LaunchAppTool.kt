package io.ashkay.talon.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import io.ashkay.talon.platform.DeviceController
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

class LaunchAppTool(private val deviceController: DeviceController) :
  SimpleTool<LaunchAppTool.Args>(
    argsSerializer = Args.serializer(),
    name = "launch_app",
    description =
      "Launches an app by its package name. Use get_installed_apps first to find the correct package name. After launching, call get_screen to see the new screen.",
  ) {

  @Serializable
  data class Args(
    @property:LLMDescription("The package name of the app to launch, e.g. com.google.android.keep")
    val packageName: String
  )

  override suspend fun execute(args: Args): String {
    Napier.d(tag = TAG) { "Launching app: ${args.packageName}" }
    val success = deviceController.launchApp(args.packageName)
    if (success) delay(ToolConstants.APP_LAUNCH_DELAY_MS)
    return if (success) "Launched ${args.packageName}. Call get_screen to see the current UI."
    else "Failed to launch ${args.packageName}. Check the package name."
  }

  companion object {
    private const val TAG = "LaunchAppTool"
  }
}
