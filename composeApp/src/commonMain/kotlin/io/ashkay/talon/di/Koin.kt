package io.ashkay.talon.di

import org.koin.core.module.Module

expect fun uiTreeProviderModule(): Module

fun getSharedModules() = listOf(uiTreeProviderModule())
