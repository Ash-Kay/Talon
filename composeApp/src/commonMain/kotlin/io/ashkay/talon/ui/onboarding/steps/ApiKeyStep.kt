package io.ashkay.talon.ui.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import io.ashkay.talon.data.SettingsRepository
import io.ashkay.talon.ui.onboarding.components.StepFooter
import io.ashkay.talon.ui.onboarding.components.StepHeader
import io.ashkay.talon.ui.onboarding.components.StepScaffold
import org.jetbrains.compose.resources.stringResource
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.hint_api_key
import talon.composeapp.generated.resources.onboarding_api_key_description
import talon.composeapp.generated.resources.onboarding_api_key_title

@Composable
fun ApiKeyStep(settingsRepository: SettingsRepository, onNext: () -> Unit, onBack: () -> Unit) {
  val provider = settingsRepository.getSelectedProvider()
  var apiKey by remember { mutableStateOf(settingsRepository.getApiKey(provider)) }
  var keyVisible by remember { mutableStateOf(false) }

  StepScaffold(onBack = onBack) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
      StepHeader(
        title = stringResource(Res.string.onboarding_api_key_title),
        description =
          stringResource(Res.string.onboarding_api_key_description, provider.displayName),
      )

      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        OutlinedTextField(
          value = apiKey,
          onValueChange = { apiKey = it },
          label = { Text(stringResource(Res.string.hint_api_key)) },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          visualTransformation =
            if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { keyVisible = !keyVisible }) {
              Text(
                text = if (keyVisible) "🙈" else "👁",
                style = MaterialTheme.typography.bodyLarge,
              )
            }
          },
        )
      }

      StepFooter(
        isEnabled = apiKey.isNotBlank(),
        onNext = {
          settingsRepository.setApiKey(provider, apiKey)
          onNext()
        },
      )
    }
  }
}
