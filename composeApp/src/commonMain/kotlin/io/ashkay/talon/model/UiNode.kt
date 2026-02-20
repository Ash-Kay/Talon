package io.ashkay.talon.model

data class UiNode(
  val index: Int,
  val className: String,
  val text: String?,
  val contentDescription: String?,
  val resourceId: String?,
  val isClickable: Boolean,
  val isScrollable: Boolean,
  val isEditable: Boolean,
  val isCheckable: Boolean,
  val isChecked: Boolean,
  val bounds: Bounds,
  val children: List<UiNode>,
)

data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

fun UiNode.toPromptString(depth: Int = 0): String {
  val indent = "  ".repeat(depth)
  val attrs = buildList {
    if (isClickable) add("clickable")
    if (isScrollable) add("scrollable")
    if (isEditable) add("editable")
    if (isCheckable && isChecked) add("checked")
    if (isCheckable && !isChecked) add("unchecked")
  }
  val attrStr = if (attrs.isNotEmpty()) " [${attrs.joinToString(", ")}]" else ""
  val label =
    listOfNotNull(text, contentDescription).joinToString(" / ").takeIf { it.isNotBlank() } ?: ""
  val labelStr = if (label.isNotEmpty()) " \"$label\"" else ""
  val line = "$indent(${index}) ${className}${labelStr}${attrStr}"
  return buildString {
    appendLine(line)
    children.forEach { append(it.toPromptString(depth + 1)) }
  }
}

fun UiNode.flatten(): List<UiNode> = listOf(this) + children.flatMap { it.flatten() }
