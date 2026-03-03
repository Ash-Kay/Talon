package io.ashkay.talon.agent

sealed class AgentRunStatus {
  data class Started(val sessionId: Long) : AgentRunStatus()

  data class Completed(val sessionId: Long, val summary: String) : AgentRunStatus()

  data class Failed(val sessionId: Long, val error: String) : AgentRunStatus()

  data class Cancelled(val sessionId: Long) : AgentRunStatus()
}
