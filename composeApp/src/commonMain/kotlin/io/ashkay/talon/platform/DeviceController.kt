package io.ashkay.talon.platform

import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.UiNode

interface DeviceController {
  suspend fun getUiTree(): UiNode?

  suspend fun executeCommand(command: AgentCommand): Boolean
}
