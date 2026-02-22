package io.ashkay.talon.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.platform.DeviceController
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

class TypeTextTool(
  private val deviceController: DeviceController,
  private val onToolExecuted: ToolLogCallback = { _, _ -> },
) :
  SimpleTool<TypeTextTool.Args>(
    argsSerializer = Args.serializer(),
    name = "type_text",
    description =
      "Types text into an editable UI element by its node index. The node must be marked as [editable] in the UI tree. Always call get_screen first.",
  ) {

  @Serializable
  data class Args(
    @property:LLMDescription("The index of the editable node to type into") val nodeIndex: Int,
    @property:LLMDescription("The text to type into the element") val text: String,
  )

  override suspend fun execute(args: Args): String {
    Napier.d(tag = TAG) { "Typing '${args.text}' into node ${args.nodeIndex}" }
    val success = deviceController.execute(AgentCommand.Type(args.nodeIndex, args.text))
    if (success) {
      delay(ToolConstants.UI_SETTLE_DELAY_MS)
      onToolExecuted("type_text", args.text)
    }
    return if (success) "Typed '${args.text}' into node ${args.nodeIndex}"
    else "Failed to type into node ${args.nodeIndex}"
  }

  companion object {
    private const val TAG = "TypeTextTool"
  }
}
