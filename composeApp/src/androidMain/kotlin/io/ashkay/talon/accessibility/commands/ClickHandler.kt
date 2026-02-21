package io.ashkay.talon.accessibility.commands

import android.view.accessibility.AccessibilityNodeInfo
import io.ashkay.talon.accessibility.NodeFinder
import io.ashkay.talon.model.AgentCommand
import io.github.aakira.napier.Napier

class ClickHandler : CommandHandler<AgentCommand.Click> {

  override fun execute(root: AccessibilityNodeInfo, command: AgentCommand.Click): Boolean {
    val target = NodeFinder.findByIndex(root, command.nodeIndex)
    if (target == null) {
      Napier.w(tag = TAG) { "Node not found at index ${command.nodeIndex}" }
      return false
    }
    val clickable = NodeFinder.findClickableAncestor(target)
    if (clickable != null) {
      Napier.d(tag = TAG) { "Clicking ancestor of node ${command.nodeIndex}" }
      return clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
    Napier.d(tag = TAG) { "Clicking node ${command.nodeIndex} directly" }
    return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
  }

  companion object {
    private const val TAG = "ClickHandler"
  }
}
