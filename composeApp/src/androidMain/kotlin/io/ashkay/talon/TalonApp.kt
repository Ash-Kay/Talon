package io.ashkay.talon

import android.app.Application
import io.ashkay.talon.di.getSharedModules
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TalonApp : Application() {
  override fun onCreate() {
    super.onCreate()

    Napier.base(DebugAntilog())

    startKoin {
      androidContext(this@TalonApp)
      androidLogger()
      modules(getSharedModules())
    }
  }
}
