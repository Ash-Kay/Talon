package io.ashkay.talon.model

data class AppInfo(val packageName: String, val label: String)

fun List<AppInfo>.toPromptString(): String = buildString {
  appendLine("Installed apps:")
  this@toPromptString.forEach { appendLine("- ${it.label} (${it.packageName})") }
}
