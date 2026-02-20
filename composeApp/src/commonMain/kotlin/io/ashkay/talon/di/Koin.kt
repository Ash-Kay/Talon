package io.ashkay.talon.di

import org.koin.core.module.Module

expect fun deviceControllerModule(): Module

fun getSharedModules() = listOf(deviceControllerModule())
