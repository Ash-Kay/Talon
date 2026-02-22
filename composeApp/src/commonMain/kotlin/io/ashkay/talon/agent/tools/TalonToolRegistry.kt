package io.ashkay.talon.agent.tools

import ai.koog.agents.core.tools.ToolRegistry
import io.ashkay.talon.platform.DeviceController

typealias ToolLogCallback = (toolName: String, detail: String) -> Unit

object TalonToolRegistry {

  fun create(deviceController: DeviceController, onToolExecuted: ToolLogCallback = { _, _ -> }) =
    ToolRegistry {
      tool(GetScreenTool(deviceController, onToolExecuted))
      tool(ClickTool(deviceController, onToolExecuted))
      tool(TypeTextTool(deviceController, onToolExecuted))
      tool(ScrollTool(deviceController, onToolExecuted))
      tool(GoBackTool(deviceController, onToolExecuted))
      tool(GetInstalledAppsTool(deviceController, onToolExecuted))
      tool(LaunchAppTool(deviceController, onToolExecuted))
    }
}
