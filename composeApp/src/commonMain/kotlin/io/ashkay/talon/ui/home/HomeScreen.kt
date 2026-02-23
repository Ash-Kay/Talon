package io.ashkay.talon.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.ashkay.talon.agent.AgentSideEffect
import io.ashkay.talon.agent.AgentState
import io.ashkay.talon.agent.AgentStatus
import io.ashkay.talon.agent.AgentViewModel
import io.ashkay.talon.data.db.LogEntryEntity
import io.ashkay.talon.data.db.LogEntryStatus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.btn_open_accessibility_settings
import talon.composeapp.generated.resources.btn_run_agent
import talon.composeapp.generated.resources.hint_enter_goal
import talon.composeapp.generated.resources.home_accessibility_card_description
import talon.composeapp.generated.resources.home_accessibility_card_title
import talon.composeapp.generated.resources.label_agent_error
import talon.composeapp.generated.resources.label_agent_idle
import talon.composeapp.generated.resources.label_agent_running
import talon.composeapp.generated.resources.label_agent_success
import talon.composeapp.generated.resources.label_log_empty
import talon.composeapp.generated.resources.label_logs_title

@Composable
fun HomeScreen(
  onOpenAccessibilitySettings: () -> Unit,
  onStartForegroundService: () -> Unit,
  onStopForegroundService: () -> Unit,
  isAccessibilityEnabled: Boolean,
  viewModel: AgentViewModel = koinViewModel(),
) {
  val state by viewModel.collectAsState()

  LaunchedEffect(isAccessibilityEnabled) {
    viewModel.refreshAccessibilityStatus(isAccessibilityEnabled)
  }

  viewModel.collectSideEffect { sideEffect ->
    when (sideEffect) {
      is AgentSideEffect.OpenAccessibilitySettings -> onOpenAccessibilitySettings()
      is AgentSideEffect.StartForegroundService -> onStartForegroundService()
      is AgentSideEffect.StopForegroundService -> onStopForegroundService()
      is AgentSideEffect.ShowToast -> {}
    }
  }

  HomeContent(
    state = state,
    viewModel = viewModel,
    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
  )
}

@Composable
private fun HomeContent(
  state: AgentState,
  viewModel: AgentViewModel,
  onOpenAccessibilitySettings: () -> Unit,
) {
  var goal by rememberSaveable { mutableStateOf("") }
  val logs by viewModel.currentSessionLogs.collectAsState()

  Column(
    modifier =
      Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize().padding(16.dp)
  ) {
    if (!state.isAccessibilityEnabled) {
      AccessibilityErrorCard(onOpenAccessibilitySettings = onOpenAccessibilitySettings)
      Spacer(Modifier.height(12.dp))
    }

    StatusBanner(state)
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
      value = goal,
      onValueChange = { goal = it },
      label = { Text(stringResource(Res.string.hint_enter_goal)) },
      modifier = Modifier.fillMaxWidth(),
      minLines = 3,
      maxLines = 5,
    )

    Spacer(Modifier.height(12.dp))

    Button(
      onClick = { viewModel.runAgent(goal) },
      enabled =
        state.isAccessibilityEnabled && state.hasApiKey && state.status !is AgentStatus.Running,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(Res.string.btn_run_agent))
    }

    Spacer(Modifier.height(16.dp))

    if (logs.isNotEmpty()) {
      Text(
        text = stringResource(Res.string.label_logs_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(8.dp))
    }

    TimelineLogPanel(logs = logs, modifier = Modifier.weight(1f).fillMaxWidth())
  }
}

@Composable
private fun AccessibilityErrorCard(onOpenAccessibilitySettings: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(Res.string.home_accessibility_card_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = stringResource(Res.string.home_accessibility_card_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
      )
      Spacer(Modifier.height(12.dp))
      Button(
        onClick = onOpenAccessibilitySettings,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(Res.string.btn_open_accessibility_settings))
      }
    }
  }
}

@Composable
private fun StatusBanner(state: AgentState) {
  val (text, color) =
    when (state.status) {
      is AgentStatus.Idle ->
        stringResource(Res.string.label_agent_idle) to MaterialTheme.colorScheme.onBackground
      is AgentStatus.Running ->
        stringResource(Res.string.label_agent_running) to MaterialTheme.colorScheme.primary
      is AgentStatus.Success ->
        stringResource(Res.string.label_agent_success) to MaterialTheme.colorScheme.tertiary
      is AgentStatus.Error ->
        stringResource(Res.string.label_agent_error, (state.status).message) to
          MaterialTheme.colorScheme.error
    }
  Text(text = text, color = color, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun TimelineLogPanel(logs: List<LogEntryEntity>, modifier: Modifier = Modifier) {
  val listState = rememberLazyListState()

  LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1) }

  if (logs.isEmpty()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
      Text(
        text = stringResource(Res.string.label_log_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  } else {
    LazyColumn(state = listState, modifier = modifier) {
      itemsIndexed(logs) { index, entry ->
        TimelineItem(entry = entry, isLast = index == logs.lastIndex)
      }
    }
  }
}

@Composable
private fun TimelineItem(entry: LogEntryEntity, isLast: Boolean) {
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
