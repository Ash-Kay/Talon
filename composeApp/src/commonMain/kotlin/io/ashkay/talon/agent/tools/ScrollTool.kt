package io.ashkay.talon.agent.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.ScrollDirection
import io.ashkay.talon.platform.DeviceController
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable

class ScrollTool(private val deviceController: DeviceController) :
  SimpleTool<ScrollTool.Args>(
    argsSerializer = Args.serializer(),
    name = "scroll",
    description =
      "Scrolls a UI element by its node index. Direction must be one of: UP, DOWN, LEFT, RIGHT. The target node or its ancestor must be [scrollable]. Always call get_screen first.",
  ) {

  @Serializable
  data class Args(
    @property:LLMDescription("The index of the scrollable node") val nodeIndex: Int,
    @property:LLMDescription("The scroll direction: UP, DOWN, LEFT, or RIGHT") val direction: String,
  )

  override suspend fun execute(args: Args): String {
    Napier.d(tag = TAG) { "Scrolling node ${args.nodeIndex} ${args.direction}" }
    val dir =
      when (args.direction.uppercase()) {
        "UP" -> ScrollDirection.UP
        "DOWN" -> ScrollDirection.DOWN
        "LEFT" -> ScrollDirection.LEFT
        "RIGHT" -> ScrollDirection.RIGHT
        else -> return "ERROR: Invalid direction '${args.direction}'. Use UP, DOWN, LEFT, or RIGHT."
      }
    val success = deviceController.execute(AgentCommand.Scroll(args.nodeIndex, dir))
    return if (success) "Scrolled node ${args.nodeIndex} ${args.direction}"
    else "Failed to scroll node ${args.nodeIndex}"
  }

  companion object {
    private const val TAG = "ScrollTool"
  }
}
