package io.ashkay.talon.data

import com.russhwolf.settings.Settings
import io.ashkay.talon.agent.LlmProvider
import io.github.aakira.napier.Napier

class SettingsRepository(private val settings: Settings) {

  fun getApiKey(provider: LlmProvider): String = settings.getString(apiKeyKey(provider), "")

  fun setApiKey(provider: LlmProvider, apiKey: String) {
    Napier.d(tag = TAG) { "Saving API key for ${provider.displayName}" }
    settings.putString(apiKeyKey(provider), apiKey)
  }

  fun getSelectedProvider(): LlmProvider {
    val name = settings.getString(KEY_SELECTED_PROVIDER, LlmProvider.GOOGLE.name)
    return try {
      LlmProvider.valueOf(name)
    } catch (_: IllegalArgumentException) {
      LlmProvider.GOOGLE
    }
  }

  fun setSelectedProvider(provider: LlmProvider) {
    Napier.d(tag = TAG) { "Saving selected provider: ${provider.displayName}" }
    settings.putString(KEY_SELECTED_PROVIDER, provider.name)
  }

  private fun apiKeyKey(provider: LlmProvider): String = "${KEY_API_KEY_PREFIX}${provider.name}"

  companion object {
    private const val TAG = "SettingsRepo"
    private const val KEY_SELECTED_PROVIDER = "selected_llm_provider"
    private const val KEY_API_KEY_PREFIX = "api_key_"
  }
}
