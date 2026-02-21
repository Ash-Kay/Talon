package io.ashkay.talon.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import io.ashkay.talon.model.toPromptString
import io.ashkay.talon.platform.DeviceController
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable

class GetInstalledAppsTool(private val deviceController: DeviceController) :
  SimpleTool<GetInstalledAppsTool.Args>(
    argsSerializer = Args.serializer(),
    name = "get_installed_apps",
    description =
      "Returns a list of all installed apps on the device with their package names. Use this to find the correct package name before launching an app.",
  ) {

  @Serializable
  data class Args(
    @property:LLMDescription("Unused placeholder, pass empty string") val placeholder: String = ""
  )

  override suspend fun execute(args: Args): String {
    Napier.d(tag = TAG) { "Executing get_installed_apps" }
    val apps = deviceController.getInstalledApps()
    Napier.d(tag = TAG) { "Found ${apps.size} apps" }
    return apps.toPromptString()
  }

  companion object {
    private const val TAG = "GetAppsTool"
  }
}
