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
import io.ashkay.talon.model.AgentCommand
import io.ashkay.talon.model.toPromptString
import io.ashkay.talon.platform.DeviceController
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
  val deviceController = koinInject<DeviceController>()
  MaterialTheme {
    Column(
      modifier =
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
          .safeContentPadding()
          .fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val scope = rememberCoroutineScope()
      Button(
        onClick = {
          scope.launch {
            val uiTree = deviceController.getUiTree()
            val promptTree = uiTree?.toPromptString()
            println(promptTree)

            deviceController.executeCommand(AgentCommand.Click(nodeIndex = 15))
          }
        }
      ) {
        Text("Get UI Tree")
      }
      Button(onClick = { scope.launch { runAgent(deviceController) } }) { Text("Run agent") }
      Button(onClick = { println("AGENT CLICK") }) { Text("Agent Click here!") }
    }
  }
}

suspend fun runAgent(deviceController: DeviceController) {
  val apiKey = ""

  // Create an agent
  val agent =
    AIAgent(promptExecutor = simpleGoogleAIExecutor(apiKey), llmModel = GoogleModels.Gemini2_5Flash)

  val nodeTree = deviceController.getUiTree()?.toPromptString()

  println("UI TREE: $nodeTree")

  // Run the agent
  val result =
    agent.run(
      "I am passing you a UI tree. There is a button node with text 'Agent Click here!', return ONLY AND ONLY that node index so that it can be clicked" +
        "\n<UITree>\n" +
        nodeTree +
        "\n</UITree>"
    )

  println("CLICKING NODE: $result")
  deviceController.executeCommand(AgentCommand.Click(nodeIndex = result.toInt()))
}
