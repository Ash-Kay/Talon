package io.ashkay.talon.ui.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.btn_next
import talon.composeapp.generated.resources.btn_skip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepScaffold(
  onBack: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      if (onBack != null) {
        TopAppBar(
          title = {},
          navigationIcon = {
            IconButton(onClick = onBack) { Text("←", style = MaterialTheme.typography.titleLarge) }
          },
          windowInsets = WindowInsets(0),
        )
      }
    },
    contentWindowInsets = WindowInsets(0),
  ) { paddingValues ->
    Column(
      modifier =
        Modifier.fillMaxWidth().padding(paddingValues).padding(horizontal = 24.dp).imePadding()
    ) {
      content()
    }
  }
}

@Composable
fun StepHeader(title: String, description: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    )
    Spacer(Modifier.height(24.dp))
  }
}

@Composable
fun StepFooter(
  isEnabled: Boolean,
  onNext: () -> Unit,
  nextButtonText: String = stringResource(Res.string.btn_next),
  showSkip: Boolean = false,
  onSkip: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
    horizontalArrangement = if (showSkip) Arrangement.SpaceBetween else Arrangement.End,
  ) {
    if (showSkip && onSkip != null) {
      OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(stringResource(Res.string.btn_skip))
      }
    }
    Button(onClick = onNext, enabled = isEnabled, modifier = Modifier.weight(1f)) {
      Text(nextButtonText)
    }
  }
}
