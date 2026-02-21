package io.ashkay.talon.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import androidx.lifecycle.ViewModel
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.toPromptString
import io.ashkay.talon.platform.DeviceController
import io.github.aakira.napier.Napier
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class AgentViewModel(private val deviceController: DeviceController) :
  ViewModel(), ContainerHost<AgentState, AgentSideEffect> {

  override val container = container<AgentState, AgentSideEffect>(AgentState())

  fun refreshAccessibilityStatus(enabled: Boolean) = intent {
    Napier.d(tag = TAG) { "Accessibility enabled: $enabled" }
    reduce { state.copy(isAccessibilityEnabled = enabled) }
  }

  fun captureUiTree() = intent {
    Napier.d(tag = TAG) { "Capturing UI tree" }
    appendLog("Capturing UI tree...")
    val tree = deviceController.getUiTree()
    if (tree == null) {
      Napier.w(tag = TAG) { "UI tree is null" }
      appendLog("Failed: UI tree is null")
      postSideEffect(AgentSideEffect.ShowToast("UI tree unavailable"))
      return@intent
    }
    val prompt = tree.toPromptString()
    Napier.d(tag = TAG) { "UI tree captured:\n$prompt" }
    reduce { state.copy(uiTreeSnapshot = prompt) }
    appendLog("UI tree captured (${tree.children.size} top-level children)")
  }

  fun runAgent(goal: String, apiKey: String) = intent {
    if (goal.isBlank()) {
      postSideEffect(AgentSideEffect.ShowToast("Please enter a goal"))
      return@intent
    }
    Napier.i(tag = TAG) { "Running agent with goal: $goal" }
    reduce { state.copy(status = AgentStatus.Running, logs = emptyList()) }
    postSideEffect(AgentSideEffect.StartForegroundService)
    appendLog("Goal: $goal")

    try {
      appendLog("Capturing screen...")
      val tree = deviceController.getUiTree()
      if (tree == null) {
        Napier.w(tag = TAG) { "UI tree unavailable" }
        reduce { state.copy(status = AgentStatus.Error("UI tree unavailable")) }
        appendLog("Error: UI tree is null")
        postSideEffect(AgentSideEffect.StopForegroundService)
        return@intent
      }

      val nodeTree = tree.toPromptString()
      reduce { state.copy(uiTreeSnapshot = nodeTree) }
      appendLog("Screen captured, sending to AI...")

      val agent =
        AIAgent(
          promptExecutor = simpleGoogleAIExecutor(apiKey),
          llmModel = GoogleModels.Gemini2_5Flash,
        )

      val prompt = buildAgentPrompt(goal, nodeTree)
      Napier.d(tag = TAG) { "Agent prompt:\n$prompt" }

      val result = agent.run(prompt)
      Napier.i(tag = TAG) { "Agent response: $result" }
      appendLog("AI response: $result")

      val command = parseAgentResponse(result)
      if (command != null) {
        appendLog("Executing: $command")
        val success = deviceController.execute(command)
        appendLog(if (success) "Command executed successfully" else "Command execution failed")
        Napier.d(tag = TAG) { "Command $command result: $success" }
      } else {
        appendLog("No actionable command parsed from response")
      }

      reduce { state.copy(status = AgentStatus.Success(result)) }
      postSideEffect(AgentSideEffect.StopForegroundService)
    } catch (e: Exception) {
      Napier.e(tag = TAG, throwable = e) { "Agent failed" }
      reduce { state.copy(status = AgentStatus.Error(e.message ?: "Unknown error")) }
      appendLog("Error: ${e.message}")
      postSideEffect(AgentSideEffect.StopForegroundService)
    }
  }

  fun requestOpenAccessibilitySettings() = intent {
    Napier.d(tag = TAG) { "Requesting accessibility settings" }
    postSideEffect(AgentSideEffect.OpenAccessibilitySettings)
  }

  private fun buildAgentPrompt(goal: String, uiTree: String): String =
    """
    |You are a mobile device agent. The user wants: "$goal"
    |
    |Here is the current screen's UI tree. Each node has an index number in parentheses.
    |Nodes marked [clickable] can be clicked, [scrollable] can be scrolled, [editable] can receive text.
    |
    |<UITree>
    |$uiTree
    |</UITree>
    |
    |Respond with EXACTLY ONE action in this format:
    |CLICK <index>
    |TYPE <index> <text>
    |SCROLL <index> UP|DOWN|LEFT|RIGHT
    |BACK
    |
    |Return ONLY the action line, nothing else.
    """
      .trimMargin()

  private fun parseAgentResponse(response: String): AgentCommand? {
    val trimmed = response.trim()
    return when {
      trimmed.startsWith("CLICK") -> {
        val index = trimmed.removePrefix("CLICK").trim().toIntOrNull()
        index?.let { AgentCommand.Click(it) }
      }
      trimmed.startsWith("TYPE") -> {
        val parts = trimmed.removePrefix("TYPE").trim().split(" ", limit = 2)
        val index = parts.getOrNull(0)?.toIntOrNull()
        val text = parts.getOrNull(1)
        if (index != null && text != null) AgentCommand.Type(index, text) else null
      }
      trimmed.startsWith("SCROLL") -> {
        val parts = trimmed.removePrefix("SCROLL").trim().split(" ")
        val index = parts.getOrNull(0)?.toIntOrNull()
        val dir =
          when (parts.getOrNull(1)?.uppercase()) {
            "UP" -> io.ashkay.talon.model.ScrollDirection.UP
            "DOWN" -> io.ashkay.talon.model.ScrollDirection.DOWN
            "LEFT" -> io.ashkay.talon.model.ScrollDirection.LEFT
            "RIGHT" -> io.ashkay.talon.model.ScrollDirection.RIGHT
            else -> null
          }
        if (index != null && dir != null) AgentCommand.Scroll(index, dir) else null
      }
      trimmed == "BACK" -> AgentCommand.GoBack
      else -> null
    }
  }

  private fun appendLog(entry: String) = intent {
    Napier.d(tag = TAG) { "Log: $entry" }
    reduce { state.copy(logs = state.logs + entry) }
  }

  companion object {
    private const val TAG = "AgentViewModel"
  }
}
