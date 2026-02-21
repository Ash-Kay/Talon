package io.ashkay.talon.agent

sealed class AgentStatus {
  data object Idle : AgentStatus()

  data object Running : AgentStatus()

  data class Success(val result: String) : AgentStatus()

  data class Error(val message: String) : AgentStatus()
}

data class AgentState(
  val status: AgentStatus = AgentStatus.Idle,
  val logs: List<String> = emptyList(),
  val uiTreeSnapshot: String? = null,
  val isAccessibilityEnabled: Boolean = false,
)
