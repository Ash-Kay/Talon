package io.ashkay.talon

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
  MaterialTheme {
    Column(
      modifier =
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
          .safeContentPadding()
          .fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val scope = rememberCoroutineScope()
      Button(onClick = { scope.launch { runAgent() } }) { Text("Click me!") }
    }
  }
}

suspend fun runAgent() {
  val apiKey = ""

  // Create an agent
  val agent =
    AIAgent(promptExecutor = simpleGoogleAIExecutor(apiKey), llmModel = GoogleModels.Gemini2_5Flash)

  // Run the agent
  val result = agent.run("Hello! How can you help me?")
  println(result)
}
