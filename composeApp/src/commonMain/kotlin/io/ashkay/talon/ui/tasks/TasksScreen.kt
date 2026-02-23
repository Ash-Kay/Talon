package io.ashkay.talon.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ashkay.talon.data.db.AgentSessionEntity
import io.ashkay.talon.data.db.SessionRepository
import io.ashkay.talon.data.db.SessionStatus
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.session_status_cancelled
import talon.composeapp.generated.resources.session_status_error
import talon.composeapp.generated.resources.session_status_running
import talon.composeapp.generated.resources.session_status_success
import talon.composeapp.generated.resources.tasks_empty
import talon.composeapp.generated.resources.tasks_title

@Composable
fun TasksScreen(onSessionClick: (Long) -> Unit = {}) {
  val sessionRepository = KoinPlatform.getKoin().get<SessionRepository>()
  val sessions by sessionRepository.getAllSessionsFlow().collectAsState(initial = emptyList())

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    Text(
      text = stringResource(Res.string.tasks_title),
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(12.dp))

    if (sessions.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(Res.string.tasks_empty),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sessions, key = { it.id }) { session ->
          SessionCard(session = session, onClick = { onSessionClick(session.id) })
          Spacer(Modifier.height(8.dp))
        }
      }
    }
  }
}

@Composable
private fun SessionCard(session: AgentSessionEntity, onClick: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = session.goal,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        SessionStatusBadge(session.status)
      }
      Spacer(Modifier.height(6.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = session.provider,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
          text = formatTimestamp(session.startedAt),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (!session.resultSummary.isNullOrBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(
          text = session.resultSummary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun SessionStatusBadge(status: String) {
  val (label, color) =
    when (status) {
      SessionStatus.RUNNING ->
        stringResource(Res.string.session_status_running) to MaterialTheme.colorScheme.primary
      SessionStatus.SUCCESS ->
        stringResource(Res.string.session_status_success) to Color(0xFF4CAF50)
      SessionStatus.ERROR ->
        stringResource(Res.string.session_status_error) to MaterialTheme.colorScheme.error
      SessionStatus.CANCELLED ->
        stringResource(Res.string.session_status_cancelled) to MaterialTheme.colorScheme.outline
      else -> status to MaterialTheme.colorScheme.onSurface
    }
  Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
}

private fun formatTimestamp(millis: Long): String {
  val seconds = millis / 1000
  val minutes = seconds / 60
  val hours = minutes / 60
  val days = hours / 24
  val now = io.ashkay.talon.data.db.currentTimeMillis()
  val diffMs = now - millis
  val diffMinutes = diffMs / 60_000
  val diffHours = diffMinutes / 60
  val diffDays = diffHours / 24

  return when {
    diffMinutes < 1 -> "Just now"
    diffMinutes < 60 -> "${diffMinutes}m ago"
    diffHours < 24 -> "${diffHours}h ago"
    diffDays < 7 -> "${diffDays}d ago"
    else -> "${days / 365}y ${(days % 365) / 30}mo ago"
  }
}
