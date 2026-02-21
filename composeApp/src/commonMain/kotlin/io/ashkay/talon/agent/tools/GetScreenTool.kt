package io.ashkay.talon.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import io.ashkay.talon.model.toPromptString
import io.ashkay.talon.platform.DeviceController
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable

class GetScreenTool(private val deviceController: DeviceController) :
  SimpleTool<GetScreenTool.Args>(
    argsSerializer = Args.serializer(),
    name = "get_screen",
    description =
      "Captures the current screen UI tree. Returns a text representation of all visible elements with their index numbers. Call this BEFORE performing any click, type, or scroll action to see what is currently on screen.",
  ) {

  @Serializable
  data class Args(
    @property:LLMDescription("Unused placeholder, pass empty string") val placeholder: String = ""
  )

  override suspend fun execute(args: Args): String {
    Napier.d(tag = TAG) { "Executing get_screen" }
    val tree = deviceController.getUiTree()
    if (tree == null) {
      Napier.w(tag = TAG) { "UI tree is null" }
      return "ERROR: Could not capture screen. Accessibility service may not be running."
    }
    return tree.toPromptString()
  }

  companion object {
    private const val TAG = "GetScreenTool"
  }
}
