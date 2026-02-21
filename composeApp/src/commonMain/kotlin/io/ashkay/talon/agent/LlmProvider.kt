package io.ashkay.talon.agent

enum class LlmProvider(val displayName: String) {
  GOOGLE("Google Gemini"),
  OPEN_AI("OpenAI"),
  ANTHROPIC("Anthropic"),
  OPEN_ROUTER("OpenRouter"),
}
