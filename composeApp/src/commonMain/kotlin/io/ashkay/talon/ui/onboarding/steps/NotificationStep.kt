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
import talon.composeapp.generated.resources.btn_finish
import talon.composeapp.generated.resources.btn_grant_notification
import talon.composeapp.generated.resources.onboarding_notification_description
import talon.composeapp.generated.resources.onboarding_notification_granted
import talon.composeapp.generated.resources.onboarding_notification_title

@Composable
fun NotificationStep(
  isNotificationGranted: Boolean,
  onRequestNotificationPermission: () -> Unit,
  onFinish: () -> Unit,
  onBack: () -> Unit,
) {
  StepScaffold(onBack = onBack) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
      StepHeader(
        title = stringResource(Res.string.onboarding_notification_title),
        description = stringResource(Res.string.onboarding_notification_description),
      )

      Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        if (isNotificationGranted) {
          Text(
            text = stringResource(Res.string.onboarding_notification_granted),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        } else {
          Button(onClick = onRequestNotificationPermission, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.btn_grant_notification))
          }
        }
      }

      StepFooter(
        isEnabled = true,
        onNext = onFinish,
        nextButtonText = stringResource(Res.string.btn_finish),
        showSkip = !isNotificationGranted,
        onSkip = onFinish,
      )
    }
  }
}
