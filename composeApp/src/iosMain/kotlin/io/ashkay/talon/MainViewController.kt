package io.ashkay.talon

import androidx.compose.ui.window.ComposeUIViewController
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

fun MainViewController() = ComposeUIViewController { App() }

fun setupLogger() {
  Napier.base(DebugAntilog())
}
