package io.ashkay.talon.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.clients.openrouter.OpenRouterModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenRouterExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import androidx.lifecycle.ViewModel
import io.ashkay.talon.agent.tools.TalonToolRegistry
import io.ashkay.talon.data.SettingsRepository
import io.ashkay.talon.platform.DeviceController
import io.github.aakira.napier.Napier
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class AgentViewModel(
  private val deviceController: DeviceController,
  private val settingsRepository: SettingsRepository,
) : ViewModel(), ContainerHost<AgentState, AgentSideEffect> {

  override val container =
    container<AgentState, AgentSideEffect>(
      AgentState(
        selectedProvider = settingsRepository.getSelectedProvider(),
        hasApiKey =
          settingsRepository.getApiKey(settingsRepository.getSelectedProvider()).isNotBlank(),
      )
    )

  fun refreshAccessibilityStatus(enabled: Boolean) = intent {
    Napier.d(tag = TAG) { "Accessibility enabled: $enabled" }
    reduce { state.copy(isAccessibilityEnabled = enabled) }
  }

  fun selectProvider(provider: LlmProvider) = intent {
    Napier.d(tag = TAG) { "Provider selected: ${provider.displayName}" }
    settingsRepository.setSelectedProvider(provider)
    val hasKey = settingsRepository.getApiKey(provider).isNotBlank()
    reduce { state.copy(selectedProvider = provider, hasApiKey = hasKey) }
  }

  fun saveApiKey(apiKey: String) = intent {
    val provider = state.selectedProvider
    Napier.d(tag = TAG) { "Saving API key for ${provider.displayName}" }
    settingsRepository.setApiKey(provider, apiKey)
    reduce { state.copy(hasApiKey = apiKey.isNotBlank()) }
  }

  fun getStoredApiKey(): String =
    settingsRepository.getApiKey(settingsRepository.getSelectedProvider())

  fun runAgent(goal: String) = intent {
    if (goal.isBlank()) {
      postSideEffect(AgentSideEffect.ShowToast("Please enter a goal"))
      return@intent
    }
    val provider = state.selectedProvider
    val apiKey = settingsRepository.getApiKey(provider)
    if (apiKey.isBlank()) {
      postSideEffect(AgentSideEffect.ShowToast("Please set an API key for ${provider.displayName}"))
      return@intent
    }
    Napier.i(tag = TAG) { "Running agent with goal: $goal, provider: ${provider.displayName}" }
    reduce { state.copy(status = AgentStatus.Running, logs = emptyList()) }
    postSideEffect(AgentSideEffect.StartForegroundService)
    appendLog("Goal: $goal")
    appendLog("Provider: ${provider.displayName}")

    try {
      val toolRegistry = TalonToolRegistry.create(deviceController)
      val (executor, model) = createExecutorAndModel(provider, apiKey)

      val agent =
        AIAgent(
          promptExecutor = executor,
          llmModel = model,
          systemPrompt = SYSTEM_PROMPT,
          toolRegistry = toolRegistry,
          strategy = talonAgentStrategy,
          maxIterations = MAX_AGENT_ITERATIONS,
        )

      appendLog("Agent started...")
      Napier.d(tag = TAG) { "Agent created with ${provider.displayName}" }

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

  private fun createExecutorAndModel(
    provider: LlmProvider,
    apiKey: String,
  ): Pair<PromptExecutor, LLModel> =
    when (provider) {
      LlmProvider.GOOGLE -> simpleGoogleAIExecutor(apiKey) to GoogleModels.Gemini2_5Flash
      LlmProvider.OPEN_AI -> simpleOpenAIExecutor(apiKey) to OpenAIModels.Chat.GPT4o
      LlmProvider.ANTHROPIC -> simpleAnthropicExecutor(apiKey) to AnthropicModels.Sonnet_3_5
      LlmProvider.OPEN_ROUTER -> simpleOpenRouterExecutor(apiKey) to OpenRouterModels.Gemini2_5Flash
    }

  private fun appendLog(entry: String) = intent {
    Napier.d(tag = TAG) { "Log: $entry" }
    reduce { state.copy(logs = state.logs + entry) }
  }

  companion object {
    private const val TAG = "AgentViewModel"
    private const val MAX_AGENT_ITERATIONS = 100

    private const val SYSTEM_PROMPT =
      """You are Talon, an autonomous mobile device agent. You control an Android phone by using tools.

WORKFLOW:
1. First understand the user's goal.
2. If you need to open an app, use get_installed_apps to find the package name, then launch_app to open it.
3. After any navigation action (launch, click, back, scroll), ALWAYS call get_screen to see the updated UI.
4. Use the node indices from get_screen to interact with elements via click, type_text, or scroll.
5. Continue step by step until the user's goal is fully completed.

RULES:
- ALWAYS call get_screen before and after clicking, typing, or scrolling so you have fresh node indices.
- After launching an app, wait and then call get_screen.
- If a click didn't change the screen, try scrolling to find the target element.
- If you are stuck, try go_back and re-approach.
- When the task is complete, respond with a summary of what you did."""
  }
}
