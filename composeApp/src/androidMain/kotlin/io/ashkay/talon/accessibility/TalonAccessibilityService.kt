package io.ashkay.talon.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.Bounds
import io.ashkay.talon.model.ScrollDirection
import io.ashkay.talon.model.UiNode
import io.github.aakira.napier.Napier
import java.util.concurrent.atomic.AtomicInteger

class TalonAccessibilityService : AccessibilityService() {

  override fun onServiceConnected() {
    super.onServiceConnected()
    instance = this
    Napier.d(tag = TAG) { "Accessibility service connected" }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

  override fun onInterrupt() {}

  override fun onDestroy() {
    super.onDestroy()
    instance = null
    Napier.d(tag = TAG) { "Accessibility service destroyed" }
  }

  fun captureUiTree(): UiNode? {
    val root = rootInActiveWindow ?: return null
    val counter = AtomicInteger(0)
    return root.toUiNode(counter)
  }

  fun performCommand(command: AgentCommand): Boolean =
    when (command) {
      is AgentCommand.Click -> performClick(command.nodeIndex)
      is AgentCommand.GoBack -> performGlobalAction(GLOBAL_ACTION_BACK)
      is AgentCommand.Scroll -> performScroll(command.nodeIndex, command.direction)
      is AgentCommand.Type -> performType(command.nodeIndex, command.text)
    }

  private fun performClick(nodeIndex: Int): Boolean {
    val root = rootInActiveWindow ?: return false
    val target = findNodeByIndex(root, AtomicInteger(0), nodeIndex) ?: return false
    var node: AccessibilityNodeInfo? = target
    while (node != null) {
      if (node.isClickable) {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
      }
      node = node.parent
    }
    return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
  }

  private fun performScroll(nodeIndex: Int, direction: ScrollDirection): Boolean {
    val root = rootInActiveWindow ?: return false
    val target = findNodeByIndex(root, AtomicInteger(0), nodeIndex)
    val action =
      when (direction) {
        ScrollDirection.UP,
        ScrollDirection.LEFT -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        ScrollDirection.DOWN,
        ScrollDirection.RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
      }
    return target?.performAction(action) ?: false
  }

  private fun performType(nodeIndex: Int, text: String): Boolean {
    val root = rootInActiveWindow ?: return false
    val target = findNodeByIndex(root, AtomicInteger(0), nodeIndex)
    if (target == null) {
      return false
    }
    target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    val args = android.os.Bundle()
    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
    return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
  }

  private fun findNodeByIndex(
    node: AccessibilityNodeInfo,
    counter: AtomicInteger,
    targetIndex: Int,
  ): AccessibilityNodeInfo? {
    if (counter.getAndIncrement() == targetIndex) return node
    for (i in 0 until node.childCount) {
      val child = node.getChild(i) ?: continue
      val found = findNodeByIndex(child, counter, targetIndex)
      if (found != null) return found
    }
    return null
  }

  companion object {
    private const val TAG = "TalonA11y"

    @Volatile
    var instance: TalonAccessibilityService? = null
      private set

    val isRunning: Boolean
      get() = instance != null
  }
}

private fun AccessibilityNodeInfo.toUiNode(counter: AtomicInteger): UiNode {
  val nodeIndex = counter.getAndIncrement()
  val rect = Rect()
  getBoundsInScreen(rect)
  val kids = buildList {
    for (i in 0 until childCount) {
      val child = getChild(i) ?: continue
      add(child.toUiNode(counter))
    }
  }
  return UiNode(
    index = nodeIndex,
    className = className?.toString()?.substringAfterLast('.') ?: "View",
    text = text?.toString(),
    contentDescription = contentDescription?.toString(),
    resourceId = viewIdResourceName?.substringAfterLast('/'),
    isClickable = isClickable,
    isScrollable = isScrollable,
    isEditable = isEditable,
    isCheckable = isCheckable,
    isChecked = isChecked,
    bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
    children = kids,
  )
}
