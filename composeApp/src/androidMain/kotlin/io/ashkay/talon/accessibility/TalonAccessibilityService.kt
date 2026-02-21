package io.ashkay.talon.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import io.ashkay.talon.accessibility.commands.ClickHandler
import io.ashkay.talon.accessibility.commands.ScrollHandler
import io.ashkay.talon.accessibility.commands.TypeHandler
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.UiNode
import io.github.aakira.napier.Napier

class TalonAccessibilityService : AccessibilityService() {

  private val clickHandler = ClickHandler()
  private val scrollHandler = ScrollHandler()
  private val typeHandler = TypeHandler()

  override fun onServiceConnected() {
    super.onServiceConnected()
    instance = this
    Napier.i(tag = TAG) { "Service connected" }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

  override fun onInterrupt() {
    Napier.w(tag = TAG) { "Service interrupted" }
  }

  override fun onDestroy() {
    super.onDestroy()
    instance = null
    Napier.i(tag = TAG) { "Service destroyed" }
  }

  fun captureUiTree(): UiNode? {
    val root = rootInActiveWindow
    if (root == null) {
      Napier.w(tag = TAG) { "rootInActiveWindow is null" }
      return null
    }
    Napier.d(tag = TAG) { "Capturing UI tree" }
    return UiTreeBuilder.buildFrom(root)
  }

  fun performCommand(command: AgentCommand): Boolean {
    Napier.d(tag = TAG) { "Executing command: $command" }
    return when (command) {
      is AgentCommand.Click -> {
        val root = rootInActiveWindow ?: return false
        clickHandler.execute(root, command)
      }
      is AgentCommand.GoBack -> {
        Napier.d(tag = TAG) { "Performing global back action" }
        performGlobalAction(GLOBAL_ACTION_BACK)
      }
      is AgentCommand.Scroll -> {
        val root = rootInActiveWindow ?: return false
        scrollHandler.execute(root, command)
      }
      is AgentCommand.Type -> {
        val root = rootInActiveWindow ?: return false
        typeHandler.execute(root, command)
      }
    }
  }

  companion object {
    private const val TAG = "TalonA11yService"

    @Volatile
    var instance: TalonAccessibilityService? = null
      private set

    val isRunning: Boolean
      get() = instance != null
  }
}
