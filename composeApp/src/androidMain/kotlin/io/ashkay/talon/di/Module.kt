package io.ashkay.talon.di

import io.ashkay.talon.agent.RunAgentUseCase
import io.ashkay.talon.platform.AndroidDeviceController
import io.ashkay.talon.platform.AndroidOverlayUiController
import io.ashkay.talon.platform.DeviceController
import io.ashkay.talon.platform.OverlayUiController
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun deviceControllerModule(): Module = module {
  single<DeviceController> { AndroidDeviceController(androidContext()) }
  single<OverlayUiController> {
    AndroidOverlayUiController(
      context = androidContext(),
      sessionRepository = get(),
      onStopAgent = { get<RunAgentUseCase>().cancel() },
    )
  }
}
