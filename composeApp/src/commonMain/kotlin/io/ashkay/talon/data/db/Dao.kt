package io.ashkay.talon.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
  @Insert suspend fun insert(session: AgentSessionEntity): Long

  @Update suspend fun update(session: AgentSessionEntity)

  @Query(
    "UPDATE sessions SET status = :status, resultSummary = :summary, completedAt = :completedAt WHERE id = :sessionId"
  )
  suspend fun completeSession(sessionId: Long, status: String, summary: String?, completedAt: Long)

  @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
  fun getAllSessionsFlow(): Flow<List<AgentSessionEntity>>

  @Query("SELECT * FROM sessions WHERE id = :id")
  suspend fun getSessionById(id: Long): AgentSessionEntity?

  @Query("SELECT * FROM sessions WHERE id = :id")
  fun getSessionByIdFlow(id: Long): Flow<AgentSessionEntity?>
}

@Dao
interface LogEntryDao {
  @Insert suspend fun insert(log: LogEntryEntity): Long

  @Update suspend fun update(log: LogEntryEntity)

  @Query("UPDATE logs SET status = :status WHERE id = :logId")
  suspend fun updateStatus(logId: Long, status: String)

  @Query("SELECT * FROM logs WHERE sessionId = :sessionId ORDER BY createdAt ASC")
  fun getLogsBySessionIdFlow(sessionId: Long): Flow<List<LogEntryEntity>>

  @Query("SELECT * FROM logs WHERE sessionId = :sessionId ORDER BY createdAt ASC")
  suspend fun getLogsBySessionId(sessionId: Long): List<LogEntryEntity>
}
