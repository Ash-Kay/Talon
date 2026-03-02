package io.ashkay.talon.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.ashkay.talon.model.Bounds
import io.ashkay.talon.model.UiNode
import java.util.concurrent.atomic.AtomicInteger

object UiTreeBuilder {

  private const val OWN_PACKAGE = "io.ashkay.talon"

  fun buildFrom(root: AccessibilityNodeInfo): UiNode? = root.toUiNode(AtomicInteger(0))

  private fun AccessibilityNodeInfo.toUiNode(counter: AtomicInteger): UiNode? {
    if (packageName?.toString() == OWN_PACKAGE) {
      return null
    }
    val nodeIndex = counter.getAndIncrement()
    val rect = Rect()
    getBoundsInScreen(rect)
    val kids = buildList {
      for (i in 0 until childCount) {
        val child = getChild(i) ?: continue
        val childNode = child.toUiNode(counter) ?: continue
        add(childNode)
      }
    }
    @Suppress("DEPRECATION")
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
}
