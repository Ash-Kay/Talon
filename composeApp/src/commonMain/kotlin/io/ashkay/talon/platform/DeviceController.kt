package io.ashkay.talon.platform

import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.UiNode

interface UiTreeProvider {
  suspend fun getUiTree(): UiNode?
}

interface CommandExecutor {
  suspend fun execute(command: AgentCommand): Boolean
}

interface DeviceController : UiTreeProvider, CommandExecutor
