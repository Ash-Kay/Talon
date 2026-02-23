package io.ashkay.talon.ui.tasks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.ashkay.talon.data.db.AgentSessionEntity
import io.ashkay.talon.data.db.LogEntryEntity
import io.ashkay.talon.data.db.LogEntryStatus
import io.ashkay.talon.data.db.SessionRepository
import io.ashkay.talon.data.db.SessionStatus
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.session_detail_no_logs
import talon.composeapp.generated.resources.session_detail_title
import talon.composeapp.generated.resources.session_status_error
import talon.composeapp.generated.resources.session_status_running
import talon.composeapp.generated.resources.session_status_success

@Composable
fun SessionDetailScreen(sessionId: Long) {
  val sessionRepository = KoinPlatform.getKoin().get<SessionRepository>()
  val session by sessionRepository.getSessionByIdFlow(sessionId).collectAsState(initial = null)
  val logs by
    sessionRepository.getLogsBySessionIdFlow(sessionId).collectAsState(initial = emptyList())

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Text(
      text = stringResource(Res.string.session_detail_title),
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(12.dp))

    session?.let { s -> SessionHeader(s) }

    Spacer(Modifier.height(16.dp))

    if (logs.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(Res.string.session_detail_no_logs),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(logs) { index, entry ->
          DetailTimelineItem(entry = entry, isLast = index == logs.lastIndex)
        }
      }
    }
  }
}

@Composable
private fun SessionHeader(session: AgentSessionEntity) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = session.goal,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = session.provider,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        val (statusText, statusColor) =
          when (session.status) {
            SessionStatus.RUNNING ->
              stringResource(Res.string.session_status_running) to MaterialTheme.colorScheme.primary
            SessionStatus.SUCCESS ->
              stringResource(Res.string.session_status_success) to Color(0xFF4CAF50)
            SessionStatus.ERROR ->
              stringResource(Res.string.session_status_error) to MaterialTheme.colorScheme.error
            else -> session.status to MaterialTheme.colorScheme.onSurface
          }
        Text(text = statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)
      }
      if (!session.resultSummary.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
          text = session.resultSummary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun DetailTimelineItem(entry: LogEntryEntity, isLast: Boolean) {
  val indicatorColor =
    when (entry.status) {
      LogEntryStatus.COMPLETED -> Color(0xFF4CAF50)
      LogEntryStatus.ONGOING -> MaterialTheme.colorScheme.primary
      LogEntryStatus.ERROR -> MaterialTheme.colorScheme.error
      else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
  val lineColor = MaterialTheme.colorScheme.outlineVariant

  Row(modifier = Modifier.fillMaxWidth()) {
    Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.TopCenter) {
      Canvas(modifier = Modifier.size(32.dp, 40.dp)) {
        val centerX = size.width / 2f
        val indicatorY = 12f

        if (!isLast) {
          drawLine(
            color = lineColor,
            start = Offset(centerX, indicatorY + 10f),
            end = Offset(centerX, size.height),
            strokeWidth = 2f,
          )
        }

        when (entry.status) {
          LogEntryStatus.COMPLETED -> {
            drawCircle(
              color = indicatorColor,
              radius = 8f,
              center = Offset(centerX, indicatorY),
              style = Fill,
            )
          }
          LogEntryStatus.ONGOING -> {
            drawCircle(
              color = indicatorColor,
              radius = 7f,
              center = Offset(centerX, indicatorY),
              style = Stroke(width = 2.5f),
            )
          }
          LogEntryStatus.ERROR -> {
            drawLine(
              color = indicatorColor,
              start = Offset(centerX - 5f, indicatorY - 5f),
              end = Offset(centerX + 5f, indicatorY + 5f),
              strokeWidth = 2.5f,
            )
            drawLine(
              color = indicatorColor,
              start = Offset(centerX + 5f, indicatorY - 5f),
              end = Offset(centerX - 5f, indicatorY + 5f),
              strokeWidth = 2.5f,
            )
          }
          else -> {
            drawCircle(
              color = indicatorColor,
              radius = 4f,
              center = Offset(centerX, indicatorY),
              style = Fill,
            )
          }
        }
      }
    }

    Text(
      text = entry.message,
      style = MaterialTheme.typography.bodySmall,
      color =
        when (entry.status) {
          LogEntryStatus.ERROR -> MaterialTheme.colorScheme.error
          LogEntryStatus.ONGOING -> MaterialTheme.colorScheme.primary
          else -> MaterialTheme.colorScheme.onSurface
        },
      modifier = Modifier.weight(1f).padding(top = 4.dp, bottom = 8.dp),
    )
  }
}
