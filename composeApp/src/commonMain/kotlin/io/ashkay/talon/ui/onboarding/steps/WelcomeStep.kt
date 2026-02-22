package io.ashkay.talon.ui.onboarding.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.btn_get_started
import talon.composeapp.generated.resources.onboarding_welcome_description
import talon.composeapp.generated.resources.onboarding_welcome_subtitle
import talon.composeapp.generated.resources.onboarding_welcome_title

@Composable
fun WelcomeStep(onNext: () -> Unit) {
  val titleScale = remember { Animatable(0.3f) }
  val titleAlpha = remember { Animatable(0f) }
  var showSubtitle by remember { mutableStateOf(false) }
  var showDescription by remember { mutableStateOf(false) }
  var showButton by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { titleAlpha.animateTo(1f, animationSpec = tween(durationMillis = 800)) }

  LaunchedEffect(Unit) {
    titleScale.animateTo(
      1f,
      animationSpec =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    )
  }

  LaunchedEffect(Unit) {
    delay(500)
    showSubtitle = true
    delay(400)
    showDescription = true
    delay(400)
    showButton = true
  }

  Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
    Column(
      modifier = Modifier.fillMaxWidth().align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        text = "🦅",
        fontSize = 72.sp,
        modifier =
          Modifier.graphicsLayer {
              scaleX = titleScale.value
              scaleY = titleScale.value
            }
            .alpha(titleAlpha.value),
      )

      Spacer(Modifier.height(16.dp))

      Text(
        text = stringResource(Res.string.onboarding_welcome_title),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier =
          Modifier.graphicsLayer {
              scaleX = titleScale.value
              scaleY = titleScale.value
            }
            .alpha(titleAlpha.value),
      )

      Spacer(Modifier.height(8.dp))

      AnimatedVisibility(
        visible = showSubtitle,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 2 },
      ) {
        Text(
          text = stringResource(Res.string.onboarding_welcome_subtitle),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
          textAlign = TextAlign.Center,
        )
      }

      Spacer(Modifier.height(32.dp))

      AnimatedVisibility(
        visible = showDescription,
        enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 2 },
      ) {
        Text(
          text = stringResource(Res.string.onboarding_welcome_description),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          textAlign = TextAlign.Center,
          lineHeight = 24.sp,
        )
      }
    }

    AnimatedVisibility(
      visible = showButton,
      enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it },
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
    ) {
      Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text(
          text = stringResource(Res.string.btn_get_started),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(vertical = 4.dp),
        )
      }
    }
  }
}
