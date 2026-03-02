package io.ashkay.talon.platform

import io.ashkay.talon.data.db.LogEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface OverlayUiController {
  val isVisible: StateFlow<Boolean>

  fun show(sessionId: Long)

  fun hide()

  fun observeLogs(): Flow<List<LogEntryEntity>>

  fun stopAgent()
}
