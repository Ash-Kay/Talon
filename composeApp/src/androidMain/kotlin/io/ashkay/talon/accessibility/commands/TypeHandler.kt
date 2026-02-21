package io.ashkay.talon.accessibility.commands

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import io.ashkay.talon.accessibility.NodeFinder
import io.ashkay.talon.model.AgentCommand
import io.github.aakira.napier.Napier

class TypeHandler : CommandHandler<AgentCommand.Type> {

  override fun execute(root: AccessibilityNodeInfo, command: AgentCommand.Type): Boolean {
    val target = NodeFinder.findByIndex(root, command.nodeIndex)
    if (target == null) {
      Napier.w(tag = TAG) { "Node not found at index ${command.nodeIndex}" }
      return false
    }
    target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    val args =
      Bundle().apply {
        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, command.text)
      }
    Napier.d(tag = TAG) { "Typing '${command.text}' into node ${command.nodeIndex}" }
    return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
  }

  companion object {
    private const val TAG = "TypeHandler"
  }
}
