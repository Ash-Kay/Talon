package io.ashkay.talon.di

import com.russhwolf.settings.Settings
import io.ashkay.talon.agent.AgentViewModel
import io.ashkay.talon.data.SettingsRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

expect fun deviceControllerModule(): Module

val settingsModule = module {
  single { Settings() }
  single { SettingsRepository(get()) }
}

val viewModelModule = module { viewModel { AgentViewModel(get(), get()) } }

fun getSharedModules() = listOf(deviceControllerModule(), settingsModule, viewModelModule)
