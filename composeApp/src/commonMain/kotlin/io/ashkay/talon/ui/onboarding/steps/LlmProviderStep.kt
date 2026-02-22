package io.ashkay.talon.ui.onboarding.steps

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.ashkay.talon.agent.LlmProvider
import io.ashkay.talon.data.SettingsRepository
import io.ashkay.talon.ui.onboarding.components.StepFooter
import io.ashkay.talon.ui.onboarding.components.StepHeader
import io.ashkay.talon.ui.onboarding.components.StepScaffold
import org.jetbrains.compose.resources.stringResource
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.onboarding_provider_description
import talon.composeapp.generated.resources.onboarding_provider_title

@Composable
fun LlmProviderStep(
  settingsRepository: SettingsRepository,
  onNext: () -> Unit,
  onBack: () -> Unit,
) {
  var selected by remember { mutableStateOf(settingsRepository.getSelectedProvider()) }

  StepScaffold(onBack = onBack) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
      StepHeader(
        title = stringResource(Res.string.onboarding_provider_title),
        description = stringResource(Res.string.onboarding_provider_description),
      )

      Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        LlmProvider.entries.forEach { provider ->
          val isSelected = selected == provider
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                  width = if (isSelected) 2.dp else 1.dp,
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                  shape = RoundedCornerShape(12.dp),
                )
                .clickable { selected = provider }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = isSelected, onClick = { selected = provider })
            Text(
              text = provider.displayName,
              style = MaterialTheme.typography.bodyLarge,
              modifier = Modifier.padding(start = 12.dp),
            )
          }
        }
      }

      StepFooter(
        isEnabled = true,
        onNext = {
          settingsRepository.setSelectedProvider(selected)
          onNext()
        },
      )
    }
  }
}
