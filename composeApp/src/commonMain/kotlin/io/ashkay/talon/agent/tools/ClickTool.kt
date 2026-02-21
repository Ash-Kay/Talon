package io.ashkay.talon.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.platform.DeviceController
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

class ClickTool(private val deviceController: DeviceController) :
  SimpleTool<ClickTool.Args>(
    argsSerializer = Args.serializer(),
    name = "click",
    description =
      "Clicks a UI element by its node index from the UI tree. Always call get_screen first to see available nodes and their indices.",
  ) {

  @Serializable
  data class Args(
    @property:LLMDescription("The index of the node to click, from the UI tree") val nodeIndex: Int
  )

  override suspend fun execute(args: Args): String {
    Napier.d(tag = TAG) { "Clicking node ${args.nodeIndex}" }
    val success = deviceController.execute(AgentCommand.Click(args.nodeIndex))
    if (success) delay(ToolConstants.UI_SETTLE_DELAY_MS)
    return if (success) "Clicked node ${args.nodeIndex} successfully"
    else "Failed to click node ${args.nodeIndex}"
  }

  companion object {
    private const val TAG = "ClickTool"
  }
}
