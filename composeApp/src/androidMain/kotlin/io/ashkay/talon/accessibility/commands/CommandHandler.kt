package io.ashkay.talon.accessibility.commands

import android.view.accessibility.AccessibilityNodeInfo

interface CommandHandler<T> {
  fun execute(root: AccessibilityNodeInfo, command: T): Boolean
}
