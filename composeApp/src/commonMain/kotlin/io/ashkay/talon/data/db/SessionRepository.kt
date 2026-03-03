package io.ashkay.talon.data.db

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao, private val logEntryDao: LogEntryDao) {

  fun getAllSessionsFlow(): Flow<List<AgentSessionEntity>> = sessionDao.getAllSessionsFlow()

  fun getSessionByIdFlow(sessionId: Long): Flow<AgentSessionEntity?> =
    sessionDao.getSessionByIdFlow(sessionId)

  fun getLogsBySessionIdFlow(sessionId: Long): Flow<List<LogEntryEntity>> =
    logEntryDao.getLogsBySessionIdFlow(sessionId)

  suspend fun createSession(goal: String, provider: String): Long {
    val entity =
      AgentSessionEntity(name = goal, provider = provider, status = SessionStatus.RUNNING)
    val id = sessionDao.insert(entity)
    Napier.d(tag = TAG) { "Session created: id=$id, goal=$goal" }
    return id
  }

  suspend fun completeSession(sessionId: Long, summary: String?) {
    if (!summary.isNullOrBlank()) {
      appendLog(
        sessionId = sessionId,
        message = summary,
        type = LogType.SUMMARY,
        status = LogEntryStatus.COMPLETED,
      )
    }
    sessionDao.completeSession(
      sessionId = sessionId,
      status = SessionStatus.SUCCESS,
      completedAt = currentTimeMillis(),
    )
    Napier.d(tag = TAG) { "Session completed: id=$sessionId" }
  }

  suspend fun failSession(sessionId: Long, errorMessage: String?) {
    if (!errorMessage.isNullOrBlank()) {
      appendLog(
        sessionId = sessionId,
        message = errorMessage,
        type = LogType.SUMMARY,
        status = LogEntryStatus.ERROR,
      )
    }
    sessionDao.completeSession(
      sessionId = sessionId,
      status = SessionStatus.ERROR,
      completedAt = currentTimeMillis(),
    )
    Napier.d(tag = TAG) { "Session failed: id=$sessionId, error=$errorMessage" }
  }

  suspend fun appendLog(
    sessionId: Long,
    message: String,
    type: String = LogType.INFO,
    status: String = LogEntryStatus.COMPLETED,
    detail: String? = null,
  ): Long {
    val entity =
      LogEntryEntity(
        sessionId = sessionId,
        type = type,
        status = status,
        message = message,
        detail = detail,
      )
    val id = logEntryDao.insert(entity)
    Napier.d(tag = TAG) { "Log appended: sessionId=$sessionId, type=$type, msg=$message" }
    return id
  }

  suspend fun updateLogStatus(logId: Long, status: String) {
    logEntryDao.updateStatus(logId, status)
  }

  suspend fun updateSessionStatus(sessionId: Long, status: String) {
    sessionDao.updateStatus(sessionId, status)
    Napier.d(tag = TAG) { "Session status updated: id=$sessionId, status=$status" }
  }

  companion object {
    private const val TAG = "SessionRepository"
  }
}
