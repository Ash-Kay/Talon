package io.ashkay.talon.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ashkay.talon.agent.AgentRunStatus
import io.ashkay.talon.agent.RunAgentUseCase
import io.ashkay.talon.data.SettingsRepository
import io.ashkay.talon.data.db.AgentSessionEntity
import io.ashkay.talon.data.db.LogEntryEntity
import io.ashkay.talon.data.db.LogEntryStatus
import io.ashkay.talon.data.db.LogType
import io.ashkay.talon.data.db.SessionRepository
import io.ashkay.talon.data.db.SessionStatus
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

data class SessionDetailState(
  val session: AgentSessionEntity? = null,
  val logs: List<LogEntryEntity> = emptyList(),
  val userInput: String = "",
  val isAgentRunning: Boolean = false,
  val isAccessibilityEnabled: Boolean = false,
  val isOverlayEnabled: Boolean = false,
  val permissionMessages: List<String> = emptyList(),
)

sealed class SessionDetailSideEffect {
  data object ScrollToBottom : SessionDetailSideEffect()

  data class ShowOverlay(val sessionId: Long) : SessionDetailSideEffect()

  data object HideOverlay : SessionDetailSideEffect()

  data object StartForegroundService : SessionDetailSideEffect()

  data object StopForegroundService : SessionDetailSideEffect()
}

class SessionDetailViewModel(
  private val sessionRepository: SessionRepository,
  private val runAgentUseCase: RunAgentUseCase,
  private val settingsRepository: SettingsRepository,
  private val sessionId: Long,
) : ViewModel(), ContainerHost<SessionDetailState, SessionDetailSideEffect> {

  override val container =
    container<SessionDetailState, SessionDetailSideEffect>(SessionDetailState())

  init {
    Napier.d(tag = TAG) { "Initializing for sessionId=$sessionId" }
    if (sessionId != NEW_SESSION_ID) {
      observeSessionById(sessionId)
      observeLogsById(sessionId)
    }
  }

  fun refreshPermissions(isAccessibilityEnabled: Boolean, isOverlayEnabled: Boolean) = intent {
    val messages = buildList {
      if (!isAccessibilityEnabled) add(PERMISSION_ERROR_ACCESSIBILITY)
      if (!isOverlayEnabled) add(PERMISSION_ERROR_OVERLAY)
    }
    reduce {
      state.copy(
        isAccessibilityEnabled = isAccessibilityEnabled,
        isOverlayEnabled = isOverlayEnabled,
        permissionMessages = messages,
      )
    }
  }

  fun onInputChanged(input: String) = intent { reduce { state.copy(userInput = input) } }

  fun sendMessage() = intent {
    val message = state.userInput.trim()
    if (message.isBlank() || state.isAgentRunning) return@intent

    Napier.d(tag = TAG) { "User submitted: $message" }
    reduce { state.copy(userInput = "") }

    val provider = settingsRepository.getSelectedProvider()
    val currentSession = state.session
    val activeSessionId: Long
    val previousSummary: String?

    if (currentSession == null) {
      activeSessionId = sessionRepository.createSession(message, provider.name)
      previousSummary = null
      observeSessionById(activeSessionId)
      observeLogsById(activeSessionId)
    } else {
      activeSessionId = currentSession.id
      previousSummary = currentSession.resultSummary
    }

    sessionRepository.appendLog(
      sessionId = activeSessionId,
      message = message,
      type = LogType.USER_MESSAGE,
      status = LogEntryStatus.COMPLETED,
    )

    if (!state.isAccessibilityEnabled) {
      appendSystemError(activeSessionId, PERMISSION_ERROR_ACCESSIBILITY)
      sessionRepository.failSession(activeSessionId, "Missing accessibility permission")
      return@intent
    }
    if (!state.isOverlayEnabled) {
      appendSystemError(activeSessionId, PERMISSION_ERROR_OVERLAY)
      sessionRepository.failSession(activeSessionId, "Missing overlay permission")
      return@intent
    }

    val apiKey = settingsRepository.getApiKey(provider)
    if (apiKey.isBlank() && !RunAgentUseCase.USE_FAKE_AGENT) {
      appendSystemError(activeSessionId, PERMISSION_ERROR_API_KEY)
      sessionRepository.failSession(activeSessionId, "Missing API key")
      return@intent
    }

    if (currentSession != null) {
      sessionRepository.updateSessionStatus(activeSessionId, SessionStatus.RUNNING)
    }

    reduce { state.copy(isAgentRunning = true) }
    postSideEffect(SessionDetailSideEffect.StartForegroundService)
    postSideEffect(SessionDetailSideEffect.ShowOverlay(activeSessionId))

    runAgentUseCase.run(
      scope = viewModelScope,
      sessionId = activeSessionId,
      goal = message,
      previousSummary = previousSummary,
    ) { status ->
      intent {
        when (status) {
          is AgentRunStatus.Started -> {
            Napier.d(tag = TAG) { "Agent started for session ${status.sessionId}" }
          }
          is AgentRunStatus.Completed -> {
            Napier.d(tag = TAG) { "Agent completed: ${status.summary}" }
            reduce { state.copy(isAgentRunning = false) }
            postSideEffect(SessionDetailSideEffect.HideOverlay)
            postSideEffect(SessionDetailSideEffect.StopForegroundService)
          }
          is AgentRunStatus.Failed -> {
            Napier.d(tag = TAG) { "Agent failed: ${status.error}" }
            reduce { state.copy(isAgentRunning = false) }
            postSideEffect(SessionDetailSideEffect.HideOverlay)
            postSideEffect(SessionDetailSideEffect.StopForegroundService)
          }
          is AgentRunStatus.Cancelled -> {
            Napier.d(tag = TAG) { "Agent cancelled" }
            reduce { state.copy(isAgentRunning = false) }
            postSideEffect(SessionDetailSideEffect.HideOverlay)
            postSideEffect(SessionDetailSideEffect.StopForegroundService)
          }
        }
      }
    }
  }

  fun cancelAgent() {
    Napier.i(tag = TAG) { "Cancel agent requested" }
    runAgentUseCase.cancel()
  }

  private fun observeSessionById(id: Long) {
    sessionRepository
      .getSessionByIdFlow(id)
      .onEach { session ->
        intent {
          reduce {
            state.copy(session = session, isAgentRunning = session?.status == SessionStatus.RUNNING)
          }
        }
      }
      .launchIn(viewModelScope)
  }

  private fun observeLogsById(id: Long) {
    sessionRepository
      .getLogsBySessionIdFlow(id)
      .onEach { logs ->
        intent {
          reduce { state.copy(logs = logs) }
          postSideEffect(SessionDetailSideEffect.ScrollToBottom)
        }
      }
      .launchIn(viewModelScope)
  }

  private suspend fun appendSystemError(sessionId: Long, message: String) {
    sessionRepository.appendLog(
      sessionId = sessionId,
      message = message,
      type = LogType.ERROR,
      status = LogEntryStatus.ERROR,
    )
  }

  companion object {
    private const val TAG = "SessionDetailVM"
    const val NEW_SESSION_ID = -1L
    private const val PERMISSION_ERROR_ACCESSIBILITY =
      "Accessibility service is not enabled. Please enable it in device Settings to let Talon interact with your screen."
    private const val PERMISSION_ERROR_OVERLAY =
      "Overlay permission is not granted. Please enable \"Display over other apps\" in Settings so Talon can show progress."
    private const val PERMISSION_ERROR_API_KEY =
      "No API key configured. Please set your API key in Settings before running a task."
  }
}
