package io.ashkay.talon.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.platform.DeviceController
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

class GoBackTool(
  private val deviceController: DeviceController,
  private val onToolExecuted: ToolLogCallback = { _, _ -> },
) :
  SimpleTool<GoBackTool.Args>(
    argsSerializer = Args.serializer(),
    name = "go_back",
    description = "Presses the system back button to navigate to the previous screen.",
  ) {

  @Serializable
  data class Args(
    @property:LLMDescription("Unused placeholder, pass empty string") val placeholder: String = ""
  )

  override suspend fun execute(args: Args): String {
    Napier.d(tag = TAG) { "Executing go_back" }
    val success = deviceController.execute(AgentCommand.GoBack)
    if (success) {
      delay(ToolConstants.UI_SETTLE_DELAY_MS)
      onToolExecuted("go_back", "")
    }
    return if (success) "Navigated back successfully" else "Failed to go back"
  }

  companion object {
    private const val TAG = "GoBackTool"
  }
}
