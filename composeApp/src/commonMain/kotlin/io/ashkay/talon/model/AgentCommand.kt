package io.ashkay.talon.model

sealed class AgentCommand {
  data class Click(val nodeIndex: Int) : AgentCommand()

  data object GoBack : AgentCommand()

  data class Scroll(val nodeIndex: Int, val direction: ScrollDirection) : AgentCommand()

  data class Type(val nodeIndex: Int, val text: String) : AgentCommand()

  data object GetInstalledApps : AgentCommand()

  data class LaunchApp(val packageName: String) : AgentCommand()
}

enum class ScrollDirection {
  UP,
  DOWN,
  LEFT,
  RIGHT,
}
