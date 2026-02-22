package io.ashkay.talon.ui.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.ashkay.talon.ui.onboarding.components.StepFooter
import io.ashkay.talon.ui.onboarding.components.StepHeader
import io.ashkay.talon.ui.onboarding.components.StepScaffold
import org.jetbrains.compose.resources.stringResource
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.btn_enable_accessibility
import talon.composeapp.generated.resources.onboarding_accessibility_description
import talon.composeapp.generated.resources.onboarding_accessibility_enabled
import talon.composeapp.generated.resources.onboarding_accessibility_title

@Composable
fun AccessibilityStep(
  isAccessibilityEnabled: Boolean,
  onOpenAccessibilitySettings: () -> Unit,
  onNext: () -> Unit,
  onBack: () -> Unit,
) {
  StepScaffold(onBack = onBack) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
      StepHeader(
        title = stringResource(Res.string.onboarding_accessibility_title),
        description = stringResource(Res.string.onboarding_accessibility_description),
      )

      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        if (isAccessibilityEnabled) {
          Text(
            text = stringResource(Res.string.onboarding_accessibility_enabled),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        } else {
          Button(onClick = onOpenAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.btn_enable_accessibility))
          }
        }
      }

      StepFooter(isEnabled = isAccessibilityEnabled, onNext = onNext)
    }
  }
}
