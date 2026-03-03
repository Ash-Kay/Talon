package io.ashkay.talon.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.russhwolf.settings.Settings
import io.ashkay.talon.agent.AgentViewModel
import io.ashkay.talon.agent.RunAgentUseCase
import io.ashkay.talon.data.SettingsRepository
import io.ashkay.talon.data.db.SessionRepository
import io.ashkay.talon.data.db.TalonDatabase
import io.ashkay.talon.data.db.getDatabaseBuilder
import io.ashkay.talon.ui.tasks.SessionDetailViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

expect fun deviceControllerModule(): Module

val settingsModule = module {
  single { Settings() }
  single { SettingsRepository(get()) }
}

val databaseModule = module {
  single<TalonDatabase> { getDatabaseBuilder().setDriver(BundledSQLiteDriver()).build() }
  single { get<TalonDatabase>().sessionDao() }
  single { get<TalonDatabase>().logEntryDao() }
  single { SessionRepository(get(), get()) }
}

val viewModelModule = module {
  single { RunAgentUseCase(get(), get(), get()) }
  viewModel { AgentViewModel(get(), get(), get()) }
  viewModel { params -> SessionDetailViewModel(get(), get(), get(), params.get()) }
}

fun getSharedModules() =
  listOf(deviceControllerModule(), settingsModule, databaseModule, viewModelModule)
