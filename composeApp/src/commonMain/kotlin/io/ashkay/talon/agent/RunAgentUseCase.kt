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
import io.ashkay.talon.agent.tools.TalonToolRegistry
import io.ashkay.talon.data.SettingsRepository
import io.ashkay.talon.data.db.LogEntryStatus
import io.ashkay.talon.data.db.LogType
import io.ashkay.talon.data.db.SessionRepository
import io.ashkay.talon.platform.DeviceController
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RunAgentUseCase(
  private val deviceController: DeviceController,
  private val settingsRepository: SettingsRepository,
  private val sessionRepository: SessionRepository,
) {

  private var agentJob: Job? = null

  val isRunning: Boolean
    get() = agentJob?.isActive == true

  fun cancel() {
    Napier.i(tag = TAG) { "Cancel agent requested" }
    agentJob?.cancel()
    agentJob = null
  }

  suspend fun run(
    scope: CoroutineScope,
    sessionId: Long,
    goal: String,
    previousSummary: String? = null,
    onStatus: suspend (AgentRunStatus) -> Unit,
  ) {
    if (isRunning) {
      Napier.w(tag = TAG) { "Agent already running, ignoring" }
      return
    }

    val provider = settingsRepository.getSelectedProvider()
    val apiKey = settingsRepository.getApiKey(provider)

    sessionRepository.appendLog(sessionId, "Goal: $goal", LogType.INFO)

    if (!USE_FAKE_AGENT) {
      sessionRepository.appendLog(sessionId, "Provider: ${provider.displayName}", LogType.INFO)
    } else {
      sessionRepository.appendLog(sessionId, "Provider: Fake (dev mode)", LogType.INFO)
    }

    onStatus(AgentRunStatus.Started(sessionId))

    agentJob =
      scope.launch {
        try {
          if (USE_FAKE_AGENT) {
            executeFakeAgent(sessionId, goal, onStatus)
          } else {
            executeRealAgent(sessionId, goal, provider, apiKey, previousSummary, onStatus)
          }
        } catch (e: CancellationException) {
          Napier.i(tag = TAG) { "Agent cancelled" }
          sessionRepository.appendLog(
            sessionId = sessionId,
            message = "Agent stopped by user",
            type = LogType.INFO,
            status = LogEntryStatus.COMPLETED,
          )
          sessionRepository.failSession(sessionId, "Cancelled by user")
          onStatus(AgentRunStatus.Cancelled(sessionId))
        } catch (e: Exception) {
          Napier.e(tag = TAG, throwable = e) { "Agent failed" }
          sessionRepository.appendLog(
            sessionId = sessionId,
            message = "Error: ${e.message}",
            type = LogType.ERROR,
            status = LogEntryStatus.ERROR,
          )
          sessionRepository.failSession(sessionId, e.message)
          onStatus(AgentRunStatus.Failed(sessionId, e.message ?: "Unknown error"))
        }
      }
  }

  private suspend fun executeRealAgent(
    sessionId: Long,
    goal: String,
    provider: LlmProvider,
    apiKey: String,
    previousSummary: String?,
    onStatus: suspend (AgentRunStatus) -> Unit,
  ) {
    val toolRegistry =
      TalonToolRegistry.create(deviceController) { toolName, detail ->
        CoroutineScope(Dispatchers.Default).launch {
          sessionRepository.appendLog(
            sessionId = sessionId,
            message = formatToolLog(toolName, detail),
            type = LogType.TOOL_USE,
            detail = detail,
          )
        }
      }
    val (executor, model) = createExecutorAndModel(provider, apiKey)

    val systemPrompt =
      if (previousSummary != null) {
        "$SYSTEM_PROMPT\n\nPREVIOUS TASK CONTEXT:\nThe user previously asked you to do something and here is the summary of what happened:\n$previousSummary\n\nThe user is now giving you a follow-up instruction. Use the above context to understand what has already been done."
      } else {
        SYSTEM_PROMPT
      }

    val agent =
      AIAgent(
        promptExecutor = executor,
        llmModel = model,
        systemPrompt = systemPrompt,
        toolRegistry = toolRegistry,
        strategy = talonAgentStrategy,
        maxIterations = MAX_AGENT_ITERATIONS,
      )

    val agentLogId =
      sessionRepository.appendLog(
        sessionId = sessionId,
        message = "Agent started...",
        type = LogType.INFO,
        status = LogEntryStatus.ONGOING,
      )
    Napier.d(tag = TAG) { "Agent created with ${provider.displayName}" }

    val result = agent.run(goal)

    Napier.i(tag = TAG) { "Agent completed: $result" }
    sessionRepository.updateLogStatus(agentLogId, LogEntryStatus.COMPLETED)
    sessionRepository.appendLog(sessionId, "Agent completed", LogType.AI_REPLY)
    sessionRepository.completeSession(sessionId, result)
    onStatus(AgentRunStatus.Completed(sessionId, result))
  }

  private suspend fun executeFakeAgent(
    sessionId: Long,
    goal: String,
    onStatus: suspend (AgentRunStatus) -> Unit,
  ) {
    Napier.i(tag = TAG) { "[FAKE] Running fake agent" }

    val agentLogId =
      sessionRepository.appendLog(
        sessionId = sessionId,
        message = "Agent started...",
        type = LogType.INFO,
        status = LogEntryStatus.ONGOING,
      )

    delay(1000)
    sessionRepository.appendLog(sessionId, "\uD83D\uDCF1 Fetched installed apps", LogType.TOOL_USE)

    delay(800)
    sessionRepository.appendLog(
      sessionId,
      "\uD83D\uDE80 Launched app: com.android.chrome",
      LogType.TOOL_USE,
    )
    deviceController.launchApp("com.android.chrome")

    delay(1500)
    sessionRepository.appendLog(sessionId, "\uD83D\uDCF7 Screen captured", LogType.TOOL_USE)
    deviceController.getUiTree()

    delay(1000)
    sessionRepository.appendLog(sessionId, "\uD83D\uDC46 Clicked: Search bar", LogType.TOOL_USE)

    delay(800)
    sessionRepository.appendLog(sessionId, "\u2328\uFE0F Typed: \"$goal\"", LogType.TOOL_USE)

    delay(1200)
    sessionRepository.appendLog(sessionId, "\uD83D\uDCF7 Screen captured", LogType.TOOL_USE)

    delay(500)
    sessionRepository.updateLogStatus(agentLogId, LogEntryStatus.COMPLETED)
    sessionRepository.appendLog(sessionId, "Agent completed", LogType.AI_REPLY)
    val summary = "[FAKE] Opened Chrome successfully"
    sessionRepository.completeSession(sessionId, summary)
    onStatus(AgentRunStatus.Completed(sessionId, summary))
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

  private fun formatToolLog(toolName: String, detail: String): String =
    when (toolName) {
      "get_screen" -> "\uD83D\uDCF7 Screen captured"
      "click" -> "\uD83D\uDC46 Clicked: $detail"
      "type_text" -> "\u2328\uFE0F Typed: \"$detail\""
      "scroll" -> "\uD83D\uDCDC Scrolled: $detail"
      "go_back" -> "\u25C0\uFE0F Navigated back"
      "launch_app" -> "\uD83D\uDE80 Launched app: $detail"
      "get_installed_apps" -> "\uD83D\uDCF1 Fetched installed apps"
      else -> "\uD83D\uDD27 $toolName: $detail"
    }

  companion object {
    private const val TAG = "RunAgentUseCase"
    private const val MAX_AGENT_ITERATIONS = 100
    const val USE_FAKE_AGENT = false

    const val SYSTEM_PROMPT =
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
- If you enter a text input field, press the UI submit button after typing to ensure the input is registered.
- When the task is complete, respond with a summary of what you did."""
  }
}
