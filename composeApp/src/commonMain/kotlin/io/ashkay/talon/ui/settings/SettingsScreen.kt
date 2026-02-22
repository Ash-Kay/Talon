package io.ashkay.talon.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.ashkay.talon.agent.LlmProvider
import io.ashkay.talon.data.SettingsRepository
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.btn_open_accessibility_settings
import talon.composeapp.generated.resources.btn_save_key
import talon.composeapp.generated.resources.hint_api_key
import talon.composeapp.generated.resources.label_api_key_not_set
import talon.composeapp.generated.resources.label_api_key_saved
import talon.composeapp.generated.resources.label_llm_provider
import talon.composeapp.generated.resources.settings_accessibility_section
import talon.composeapp.generated.resources.settings_llm_section
import talon.composeapp.generated.resources.settings_title

@Composable
fun SettingsScreen(isAccessibilityEnabled: Boolean, onOpenAccessibilitySettings: () -> Unit) {
  val settingsRepository = KoinPlatform.getKoin().get<SettingsRepository>()
  var selectedProvider by remember { mutableStateOf(settingsRepository.getSelectedProvider()) }
  var apiKeySaved by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
    Text(
      text = stringResource(Res.string.settings_title),
      style = MaterialTheme.typography.headlineMedium,
    )

    Spacer(Modifier.height(24.dp))

    Text(
      text = stringResource(Res.string.settings_llm_section),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))

    LlmProviderSelector(
      selectedProvider = selectedProvider,
      onProviderSelected = {
        selectedProvider = it
        settingsRepository.setSelectedProvider(it)
        apiKeySaved = false
      },
    )

    Spacer(Modifier.height(12.dp))

    ApiKeyField(
      provider = selectedProvider,
      settingsRepository = settingsRepository,
      onSaved = { apiKeySaved = true },
    )

    if (apiKeySaved) {
      Text(
        text = stringResource(Res.string.label_api_key_saved),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp),
      )
    }

    Spacer(Modifier.height(32.dp))

    Text(
      text = stringResource(Res.string.settings_accessibility_section),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(12.dp))

    if (isAccessibilityEnabled) {
      Text(
        text = "Accessibility service is enabled ✓",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
      )
    } else {
      Button(onClick = onOpenAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.btn_open_accessibility_settings))
      }
    }
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
  settingsRepository: SettingsRepository,
  onSaved: () -> Unit,
) {
  var apiKeyInput by
    rememberSaveable(provider) { mutableStateOf(settingsRepository.getApiKey(provider)) }
  var keyVisible by remember { mutableStateOf(false) }
  val hasApiKey = settingsRepository.getApiKey(provider).isNotBlank()

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
        settingsRepository.setApiKey(provider, apiKeyInput)
        keyVisible = false
        onSaved()
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
