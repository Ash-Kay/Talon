package io.ashkay.talon.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
  val isSending: Boolean = false,
)

sealed class SessionDetailSideEffect {
  data object ScrollToBottom : SessionDetailSideEffect()
}

class SessionDetailViewModel(
  private val sessionRepository: SessionRepository,
  private val sessionId: Long,
) : ViewModel(), ContainerHost<SessionDetailState, SessionDetailSideEffect> {

  override val container =
    container<SessionDetailState, SessionDetailSideEffect>(SessionDetailState())

  init {
    Napier.d(tag = TAG) { "Initializing for sessionId=$sessionId" }
    observeSession()
    observeLogs()
  }

  private fun observeSession() {
    sessionRepository
      .getSessionByIdFlow(sessionId)
      .onEach { session ->
        Napier.d(tag = TAG) { "Session updated: $session" }
        intent { reduce { state.copy(session = session) } }
      }
      .launchIn(viewModelScope)
  }

  private fun observeLogs() {
    sessionRepository
      .getLogsBySessionIdFlow(sessionId)
      .onEach { logs ->
        Napier.d(tag = TAG) { "Logs updated: count=${logs.size}" }
        intent {
          reduce { state.copy(logs = logs) }
          postSideEffect(SessionDetailSideEffect.ScrollToBottom)
        }
      }
      .launchIn(viewModelScope)
  }

  fun onInputChanged(input: String) = intent { reduce { state.copy(userInput = input) } }

  fun sendMessage() = intent {
    val message = state.userInput.trim()
    if (message.isBlank() || state.isSending) return@intent

    val session = state.session ?: return@intent
    val isTerminal =
      session.status == SessionStatus.SUCCESS || session.status == SessionStatus.ERROR

    if (!isTerminal) return@intent

    Napier.d(tag = TAG) { "Sending user message: $message" }
    reduce { state.copy(isSending = true, userInput = "") }

    sessionRepository.appendLog(
      sessionId = sessionId,
      message = message,
      type = LogType.USER_MESSAGE,
      status = LogEntryStatus.COMPLETED,
    )

    reduce { state.copy(isSending = false) }
    postSideEffect(SessionDetailSideEffect.ScrollToBottom)
  }

  companion object {
    private const val TAG = "SessionDetailViewModel"
  }
}
