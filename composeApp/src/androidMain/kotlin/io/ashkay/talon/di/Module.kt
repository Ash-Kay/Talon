package io.ashkay.talon.di

import io.ashkay.talon.platform.AndroidDeviceController
import io.ashkay.talon.platform.DeviceController
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun deviceControllerModule(): Module = module {
  single<DeviceController> { AndroidDeviceController() }
}
