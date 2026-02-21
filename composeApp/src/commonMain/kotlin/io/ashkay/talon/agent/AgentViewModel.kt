package io.ashkay.talon.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import androidx.lifecycle.ViewModel
import io.ashkay.talon.agent.tools.TalonToolRegistry
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
    Napier.d(tag = TAG) { "UI tree captured" }
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
      val toolRegistry = TalonToolRegistry.create(deviceController)

      val agent =
        AIAgent(
          promptExecutor = simpleGoogleAIExecutor(apiKey),
          llmModel = GoogleModels.Gemini2_5Flash,
          systemPrompt = SYSTEM_PROMPT,
          toolRegistry = toolRegistry,
          strategy = talonAgentStrategy,
          maxIterations = MAX_AGENT_ITERATIONS,
        )

      appendLog("Agent started...")
      Napier.d(tag = TAG) { "Agent created with toolRegistry" }

      val result = agent.run(goal)

      Napier.i(tag = TAG) { "Agent completed: $result" }
      appendLog("Agent completed: $result")
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

  private fun appendLog(entry: String) = intent {
    Napier.d(tag = TAG) { "Log: $entry" }
    reduce { state.copy(logs = state.logs + entry) }
  }

  companion object {
    private const val TAG = "AgentViewModel"
    private const val MAX_AGENT_ITERATIONS = 30

    private const val SYSTEM_PROMPT =
      """You are Talon, an autonomous mobile device agent. You control an Android phone by using tools.

WORKFLOW:
1. First understand the user's goal.
2. If you need to open an app, use get_installed_apps to find the package name, then launch_app to open it.
3. After any navigation action (launch, click, back, scroll), ALWAYS call get_screen to see the updated UI.
4. Use the node indices from get_screen to interact with elements via click, type_text, or scroll.
5. Continue step by step until the user's goal is fully completed.

RULES:
- ALWAYS call get_screen before clicking, typing, or scrolling so you have fresh node indices.
- After launching an app, wait and then call get_screen.
- If a click didn't change the screen, try scrolling to find the target element.
- If you are stuck, try go_back and re-approach.
- When the task is complete, respond with a summary of what you did."""
  }
}
