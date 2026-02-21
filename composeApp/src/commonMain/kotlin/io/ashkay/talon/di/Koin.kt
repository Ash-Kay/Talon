package io.ashkay.talon.di

import io.ashkay.talon.agent.AgentViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

expect fun deviceControllerModule(): Module

val viewModelModule = module { viewModel { AgentViewModel(get()) } }

fun getSharedModules() = listOf(deviceControllerModule(), viewModelModule)
