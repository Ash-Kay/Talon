package io.ashkay.talon.agent

sealed class AgentSideEffect {
  data class ShowToast(val message: String) : AgentSideEffect()

  data object OpenAccessibilitySettings : AgentSideEffect()

  data object StartForegroundService : AgentSideEffect()

  data object StopForegroundService : AgentSideEffect()
}
