package io.ashkay.talon.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class AgentSessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val goal: String,
  val provider: String,
  val status: String = SessionStatus.RUNNING,
  val resultSummary: String? = null,
  val startedAt: Long = currentTimeMillis(),
  val completedAt: Long? = null,
)

@Entity(
  tableName = "logs",
  foreignKeys =
    [
      ForeignKey(
        entity = AgentSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("sessionId")],
)
data class LogEntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val sessionId: Long,
  val type: String = LogType.INFO,
  val status: String = LogEntryStatus.COMPLETED,
  val message: String,
  val detail: String? = null,
  val createdAt: Long = currentTimeMillis(),
)

object SessionStatus {
  const val RUNNING = "running"
  const val SUCCESS = "success"
  const val ERROR = "error"
  const val CANCELLED = "cancelled"
}

object LogType {
  const val INFO = "info"
  const val TOOL_USE = "tool_use"
  const val AI_REPLY = "ai_reply"
  const val ERROR = "error"
}

object LogEntryStatus {
  const val ONGOING = "ongoing"
  const val COMPLETED = "completed"
  const val ERROR = "error"
}
