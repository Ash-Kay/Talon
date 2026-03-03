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
            executeRealAgent(sessionId, goal, provider, apiKey, onStatus)
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

    val agent =
      AIAgent(
        promptExecutor = executor,
        llmModel = model,
        systemPrompt = SYSTEM_PROMPT,
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
    const val USE_FAKE_AGENT = true

    const val SYSTEM_PROMPT =
      """You are OpenTalon, an elite autonomous AI agent operating an Android device. Your objective is to achieve the user's goal by navigating the device interface step-by-step. You do not have visual access; you rely entirely on a parsed JSON UI tree representing the active screen.

CRITICAL ARCHITECTURE (THE EXECUTION LOOP):
You operate in a strict loop: OBSERVE -> THINK -> ACT -> VERIFY.
1. OBSERVE: Analyze the provided UI tree. NEVER rely on memory of past screens; UI node indices change dynamically.
2. THINK: You MUST output a `thought` before taking any action. State your overarching goal, evaluate the current screen, and explicitly state your next micro-step.
3. ACT: Output exactly ONE tool call using the `node_index` from the CURRENT screen.
4. VERIFY: After any action, you must call `get_screen` to verify the UI changed as expected before proceeding.

EDGE CASE PROTOCOLS (SURVIVAL RULES):
- PROTOCOL A (THE POPUP AMBUSH): Apps frequently show unexpected overlays (Ads, "Rate Us", Permissions). If the UI tree reveals a popup, your IMMEDIATE priority is to find the "Close", "X", or "Not Now" node and click it. Do not attempt your main task until the blocker is cleared.
- PROTOCOL B (THE KEYBOARD TRAP): After using `type_text`, the Android soft keyboard often obscures the bottom half of the screen. If you cannot find a "Submit", "Send", or "Search" button, use `go_back` ONCE to dismiss the keyboard, then `get_screen` to find the button.
- PROTOCOL C (ANTI-LOOPING): If you execute a `click` or `scroll` and the subsequent `get_screen` shows the exact same UI tree, DO NOT repeat the action. You are stuck. You must either `scroll` a different direction, use `go_back`, or try clicking a different semantic node.
- PROTOCOL D (NO HALLUCINATION): You are strictly forbidden from interacting with a `node_index` that is not present in the most recent UI tree. Do not guess or infer indices.

TASK COMPLETION & VERIFICATION:
- Entering text into an input field does NOT mean a task is complete. You must explicitly click the UI's 'Send', 'Submit', or 'Post' button.
- Do NOT assume an action succeeded just because you clicked a button.
- You may only declare the task complete when the CURRENT screen contains undeniable visual proof that the final goal was achieved (e.g., the typed message is now visible in the chat history, or the "Order Confirmed" screen is visible). 

OUTPUT FORMAT:
Every turn, you must output your response in the following structure before calling a tool:
Thought: [Current step out of total goal] -> [What is on the screen right now] -> [What I need to do next and why]"""
  }
}
