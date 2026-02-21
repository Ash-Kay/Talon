package io.ashkay.talon.accessibility.commands

import android.view.accessibility.AccessibilityNodeInfo
import io.ashkay.talon.accessibility.NodeFinder
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.ScrollDirection
import io.github.aakira.napier.Napier

class ScrollHandler : CommandHandler<AgentCommand.Scroll> {

  override fun execute(root: AccessibilityNodeInfo, command: AgentCommand.Scroll): Boolean {
    val target = NodeFinder.findByIndex(root, command.nodeIndex)
    if (target == null) {
      Napier.w(tag = TAG) { "Node not found at index ${command.nodeIndex}" }
      return false
    }
    val scrollable = NodeFinder.findScrollableAncestor(target) ?: target
    val action =
      when (command.direction) {
        ScrollDirection.UP,
        ScrollDirection.LEFT -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        ScrollDirection.DOWN,
        ScrollDirection.RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
      }
    Napier.d(tag = TAG) { "Scrolling node ${command.nodeIndex} ${command.direction}" }
    return scrollable.performAction(action)
  }

  companion object {
    private const val TAG = "ScrollHandler"
  }
}
