package io.ashkay.talon.platform

import io.ashkay.talon.accessibility.TalonAccessibilityService
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.UiNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDeviceController : DeviceController {

  override suspend fun getUiTree(): UiNode? =
    withContext(Dispatchers.Main) { TalonAccessibilityService.instance?.captureUiTree() }

  override suspend fun executeCommand(command: AgentCommand): Boolean =
    withContext(Dispatchers.Main) {
      TalonAccessibilityService.instance?.performCommand(command) ?: false
    }
}
