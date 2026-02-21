package io.ashkay.talon.agent.tools

import ai.koog.agents.core.tools.ToolRegistry
import io.ashkay.talon.platform.DeviceController

object TalonToolRegistry {

  fun create(deviceController: DeviceController) = ToolRegistry {
    tool(GetScreenTool(deviceController))
    tool(ClickTool(deviceController))
    tool(TypeTextTool(deviceController))
    tool(ScrollTool(deviceController))
    tool(GoBackTool(deviceController))
    tool(GetInstalledAppsTool(deviceController))
    tool(LaunchAppTool(deviceController))
  }
}
