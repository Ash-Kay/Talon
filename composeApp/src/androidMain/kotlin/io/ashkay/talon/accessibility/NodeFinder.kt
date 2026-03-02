package io.ashkay.talon.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.atomic.AtomicInteger

object NodeFinder {

  private const val OWN_PACKAGE = "io.ashkay.talon"

  fun findByIndex(root: AccessibilityNodeInfo, targetIndex: Int): AccessibilityNodeInfo? =
    findRecursive(root, AtomicInteger(0), targetIndex)

  fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    var current: AccessibilityNodeInfo? = node
    while (current != null) {
      if (current.isClickable) return current
      current = current.parent
    }
    return null
  }

  fun findScrollableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    var current: AccessibilityNodeInfo? = node
    while (current != null) {
      if (current.isScrollable) return current
      current = current.parent
    }
    return null
  }

  private fun findRecursive(
    node: AccessibilityNodeInfo,
    counter: AtomicInteger,
    targetIndex: Int,
  ): AccessibilityNodeInfo? {
    if (node.packageName?.toString() == OWN_PACKAGE) return null
    if (counter.getAndIncrement() == targetIndex) return node
    for (i in 0 until node.childCount) {
      val child = node.getChild(i) ?: continue
      val found = findRecursive(child, counter, targetIndex)
      if (found != null) return found
    }
    return null
  }
}
