package io.ashkay.talon

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.ashkay.talon.data.SettingsRepository
import io.ashkay.talon.navigation.HomeDestination
import io.ashkay.talon.navigation.SessionDetailDestination
import io.ashkay.talon.navigation.SettingsDestination
import io.ashkay.talon.navigation.TasksDestination
import io.ashkay.talon.ui.home.HomeScreen
import io.ashkay.talon.ui.onboarding.OnboardingScreen
import io.ashkay.talon.ui.settings.SettingsScreen
import io.ashkay.talon.ui.tasks.SessionDetailScreen
import io.ashkay.talon.ui.tasks.TasksScreen
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.tab_home
import talon.composeapp.generated.resources.tab_settings
import talon.composeapp.generated.resources.tab_tasks

data class BottomNavItem(val route: Any, val label: @Composable () -> String, val icon: String)

@Composable
fun App(
  onOpenAccessibilitySettings: () -> Unit = {},
  onStartForegroundService: () -> Unit = {},
  onStopForegroundService: () -> Unit = {},
  onRequestNotificationPermission: () -> Unit = {},
  isAccessibilityEnabled: Boolean = false,
  isNotificationGranted: Boolean = false,
) {
  val settingsRepository = KoinPlatform.getKoin().get<SettingsRepository>()
  var onboardingCompleted by rememberSaveable {
    mutableStateOf(settingsRepository.isOnboardingCompleted())
  }

  MaterialTheme {
    if (onboardingCompleted) {
      MainShell(
        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        onStartForegroundService = onStartForegroundService,
        onStopForegroundService = onStopForegroundService,
        isAccessibilityEnabled = isAccessibilityEnabled,
      )
    } else {
      OnboardingScreen(
        settingsRepository = settingsRepository,
        isAccessibilityEnabled = isAccessibilityEnabled,
        isNotificationGranted = isNotificationGranted,
        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onFinish = { onboardingCompleted = true },
      )
    }
  }
}

@Composable
private fun MainShell(
  onOpenAccessibilitySettings: () -> Unit,
  onStartForegroundService: () -> Unit,
  onStopForegroundService: () -> Unit,
  isAccessibilityEnabled: Boolean,
) {
  val navController = rememberNavController()

  val bottomNavItems =
    listOf(
      BottomNavItem(HomeDestination, { stringResource(Res.string.tab_home) }, "🏠"),
      BottomNavItem(TasksDestination, { stringResource(Res.string.tab_tasks) }, "📋"),
      BottomNavItem(SettingsDestination, { stringResource(Res.string.tab_settings) }, "⚙"),
    )

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route.orEmpty()

  Scaffold(
    bottomBar = {
      NavigationBar(
        modifier = Modifier.heightIn(min = 50.dp, max = 80.dp),
        windowInsets = WindowInsets.navigationBars,
      ) {
        bottomNavItems.forEach { item ->
          val selected = currentRoute.contains(item.route::class.qualifiedName.orEmpty())
          NavigationBarItem(
            modifier = Modifier.height(50.dp),
            icon = { Text(text = item.icon, modifier = Modifier.size(24.dp)) },
            label = { Text(item.label()) },
            selected = selected,
            onClick = {
              navController.navigate(item.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
              }
            },
            colors =
              NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent,
              ),
          )
        }
      }
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = HomeDestination,
      modifier = Modifier.padding(innerPadding),
    ) {
      composable<HomeDestination> {
        HomeScreen(
          onOpenAccessibilitySettings = onOpenAccessibilitySettings,
          onStartForegroundService = onStartForegroundService,
          onStopForegroundService = onStopForegroundService,
          isAccessibilityEnabled = isAccessibilityEnabled,
        )
      }
      composable<TasksDestination> {
        TasksScreen(
          onSessionClick = { sessionId ->
            navController.navigate(SessionDetailDestination(sessionId))
          }
        )
      }
      composable<SessionDetailDestination> { backStackEntry ->
        val dest = backStackEntry.toRoute<SessionDetailDestination>()
        SessionDetailScreen(sessionId = dest.sessionId)
      }
      composable<SettingsDestination> {
        SettingsScreen(
          isAccessibilityEnabled = isAccessibilityEnabled,
          onOpenAccessibilitySettings = onOpenAccessibilitySettings,
        )
      }
    }
  }
}
