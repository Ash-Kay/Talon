package io.ashkay.talon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ashkay.talon.agent.AgentSideEffect
import io.ashkay.talon.agent.AgentState
import io.ashkay.talon.agent.AgentStatus
import io.ashkay.talon.agent.AgentViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.btn_get_ui_tree
import talon.composeapp.generated.resources.btn_open_accessibility_settings
import talon.composeapp.generated.resources.btn_run_agent
import talon.composeapp.generated.resources.hint_enter_goal
import talon.composeapp.generated.resources.label_accessibility_not_enabled
import talon.composeapp.generated.resources.label_agent_error
import talon.composeapp.generated.resources.label_agent_idle
import talon.composeapp.generated.resources.label_agent_running
import talon.composeapp.generated.resources.label_agent_success
import talon.composeapp.generated.resources.label_log_empty

@Composable
fun App(
  onOpenAccessibilitySettings: () -> Unit = {},
  onStartForegroundService: () -> Unit = {},
  onStopForegroundService: () -> Unit = {},
  isAccessibilityEnabled: Boolean = false,
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

  MaterialTheme { AgentScreen(state = state, viewModel = viewModel) }
}

@Composable
private fun AgentScreen(state: AgentState, viewModel: AgentViewModel) {
  var goal by rememberSaveable { mutableStateOf("") }

  Column(
    modifier =
      Modifier.background(MaterialTheme.colorScheme.background)
        .safeContentPadding()
        .fillMaxSize()
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    StatusBanner(state)
    Spacer(Modifier.height(12.dp))

    if (!state.isAccessibilityEnabled) {
      AccessibilityPrompt(viewModel)
    }

    OutlinedTextField(
      value = goal,
      onValueChange = { goal = it },
      label = { Text(stringResource(Res.string.hint_enter_goal)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(
        onClick = { viewModel.captureUiTree() },
        enabled = state.isAccessibilityEnabled && state.status !is AgentStatus.Running,
      ) {
        Text(stringResource(Res.string.btn_get_ui_tree))
      }
      Button(
        onClick = { viewModel.runAgent(goal, apiKey = "") },
        enabled = state.isAccessibilityEnabled && state.status !is AgentStatus.Running,
      ) {
        Text(stringResource(Res.string.btn_run_agent))
      }
    }

    Spacer(Modifier.height(16.dp))
    LogPanel(logs = state.logs, modifier = Modifier.weight(1f).fillMaxWidth())
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
private fun AccessibilityPrompt(viewModel: AgentViewModel) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(Res.string.label_accessibility_not_enabled),
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = { viewModel.requestOpenAccessibilitySettings() }) {
      Text(stringResource(Res.string.btn_open_accessibility_settings))
    }
  }
}

@Composable
private fun LogPanel(logs: List<String>, modifier: Modifier = Modifier) {
  val listState = rememberLazyListState()

  LaunchedEffect(logs.size) { if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1) }

  if (logs.isEmpty()) {
    Text(
      text = stringResource(Res.string.label_log_empty),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  } else {
    LazyColumn(state = listState, modifier = modifier) {
      items(logs) { entry ->
        Text(
          text = entry,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(vertical = 2.dp),
        )
      }
    }
  }
}
