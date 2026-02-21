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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.ashkay.talon.agent.AgentSideEffect
import io.ashkay.talon.agent.AgentState
import io.ashkay.talon.agent.AgentStatus
import io.ashkay.talon.agent.AgentViewModel
import io.ashkay.talon.agent.LlmProvider
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.btn_open_accessibility_settings
import talon.composeapp.generated.resources.btn_run_agent
import talon.composeapp.generated.resources.btn_save_key
import talon.composeapp.generated.resources.hint_api_key
import talon.composeapp.generated.resources.hint_enter_goal
import talon.composeapp.generated.resources.label_accessibility_not_enabled
import talon.composeapp.generated.resources.label_agent_error
import talon.composeapp.generated.resources.label_agent_idle
import talon.composeapp.generated.resources.label_agent_running
import talon.composeapp.generated.resources.label_agent_success
import talon.composeapp.generated.resources.label_api_key_not_set
import talon.composeapp.generated.resources.label_llm_provider
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

    LlmProviderSelector(
      selectedProvider = state.selectedProvider,
      onProviderSelected = { viewModel.selectProvider(it) },
    )

    Spacer(Modifier.height(8.dp))

    ApiKeyField(
      provider = state.selectedProvider,
      hasApiKey = state.hasApiKey,
      onSave = { viewModel.saveApiKey(it) },
      loadStoredKey = { viewModel.getStoredApiKey() },
    )

    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
      value = goal,
      onValueChange = { goal = it },
      label = { Text(stringResource(Res.string.hint_enter_goal)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
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
    LogPanel(logs = state.logs, modifier = Modifier.weight(1f).fillMaxWidth())
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LlmProviderSelector(
  selectedProvider: LlmProvider,
  onProviderSelected: (LlmProvider) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = Modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = selectedProvider.displayName,
      onValueChange = {},
      readOnly = true,
      label = { Text(stringResource(Res.string.label_llm_provider)) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier =
        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      LlmProvider.entries.forEach { provider ->
        DropdownMenuItem(
          text = { Text(provider.displayName) },
          onClick = {
            onProviderSelected(provider)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
private fun ApiKeyField(
  provider: LlmProvider,
  hasApiKey: Boolean,
  onSave: (String) -> Unit,
  loadStoredKey: () -> String,
) {
  var apiKeyInput by rememberSaveable(provider) { mutableStateOf(loadStoredKey()) }
  var keyVisible by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    OutlinedTextField(
      value = apiKeyInput,
      onValueChange = { apiKeyInput = it },
      label = { Text(stringResource(Res.string.hint_api_key)) },
      modifier = Modifier.weight(1f),
      singleLine = true,
      visualTransformation =
        if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
      trailingIcon = {
        IconButton(onClick = { keyVisible = !keyVisible }) {
          Text(text = if (keyVisible) "🙈" else "👁", style = MaterialTheme.typography.bodyLarge)
        }
      },
    )
    Button(
      onClick = {
        onSave(apiKeyInput)
        keyVisible = false
      }
    ) {
      Text(stringResource(Res.string.btn_save_key))
    }
  }

  if (!hasApiKey) {
    Text(
      text = stringResource(Res.string.label_api_key_not_set, provider.displayName),
      color = MaterialTheme.colorScheme.error,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
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
