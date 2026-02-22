package io.ashkay.talon.ui.onboarding

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.ashkay.talon.data.SettingsRepository
import io.ashkay.talon.ui.onboarding.steps.AccessibilityStep
import io.ashkay.talon.ui.onboarding.steps.ApiKeyStep
import io.ashkay.talon.ui.onboarding.steps.LlmProviderStep
import io.ashkay.talon.ui.onboarding.steps.NotificationStep
import io.ashkay.talon.ui.onboarding.steps.WelcomeStep
import kotlinx.serialization.Serializable

@Serializable
sealed class OnboardingDestination {
  @Serializable data object WelcomeScreen : OnboardingDestination()

  @Serializable data object ProviderScreen : OnboardingDestination()

  @Serializable data object ApiKeyScreen : OnboardingDestination()

  @Serializable data object AccessibilityScreen : OnboardingDestination()

  @Serializable data object NotificationScreen : OnboardingDestination()
}

@Composable
fun OnboardingScreen(
  settingsRepository: SettingsRepository,
  isAccessibilityEnabled: Boolean,
  isNotificationGranted: Boolean,
  onOpenAccessibilitySettings: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onFinish: () -> Unit,
) {
  val navController = rememberNavController()

  Scaffold { paddingValues ->
    NavHost(
      navController = navController,
      startDestination = OnboardingDestination.WelcomeScreen,
      modifier = Modifier.padding(paddingValues),
    ) {
      composable<OnboardingDestination.WelcomeScreen> {
        WelcomeStep(onNext = { navController.navigate(OnboardingDestination.ProviderScreen) })
      }
      composable<OnboardingDestination.ProviderScreen> {
        LlmProviderStep(
          settingsRepository = settingsRepository,
          onNext = { navController.navigate(OnboardingDestination.ApiKeyScreen) },
          onBack = { navController.popBackStack() },
        )
      }
      composable<OnboardingDestination.ApiKeyScreen> {
        ApiKeyStep(
          settingsRepository = settingsRepository,
          onNext = { navController.navigate(OnboardingDestination.AccessibilityScreen) },
          onBack = { navController.popBackStack() },
        )
      }
      composable<OnboardingDestination.AccessibilityScreen> {
        AccessibilityStep(
          isAccessibilityEnabled = isAccessibilityEnabled,
          onOpenAccessibilitySettings = onOpenAccessibilitySettings,
          onNext = { navController.navigate(OnboardingDestination.NotificationScreen) },
          onBack = { navController.popBackStack() },
        )
      }
      composable<OnboardingDestination.NotificationScreen> {
        NotificationStep(
          isNotificationGranted = isNotificationGranted,
          onRequestNotificationPermission = onRequestNotificationPermission,
          onFinish = {
            settingsRepository.setOnboardingCompleted(true)
            onFinish()
          },
          onBack = { navController.popBackStack() },
        )
      }
    }
  }
}
