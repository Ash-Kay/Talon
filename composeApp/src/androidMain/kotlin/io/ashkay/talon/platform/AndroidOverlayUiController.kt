package io.ashkay.talon.platform

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.ashkay.talon.data.db.AgentSessionEntity
import io.ashkay.talon.data.db.LogEntryEntity
import io.ashkay.talon.data.db.LogEntryStatus
import io.ashkay.talon.data.db.SessionRepository
import io.ashkay.talon.data.db.SessionStatus
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class AndroidOverlayUiController(
  private val context: Context,
  private val sessionRepository: SessionRepository,
  private val onStopAgent: () -> Unit,
) : OverlayUiController {

  private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  private var overlayView: ComposeView? = null

  private val _isVisible = MutableStateFlow(false)
  override val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

  private var currentSessionId: Long? = null

  override fun show(sessionId: Long) {
    if (overlayView != null) {
      Napier.d(tag = TAG) { "Overlay already visible, updating session" }
      currentSessionId = sessionId
      return
    }

    Napier.d(tag = TAG) { "Showing overlay for sessionId=$sessionId" }
    currentSessionId = sessionId

    val layoutParams =
      WindowManager.LayoutParams(
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.WRAP_CONTENT,
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
          else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
          PixelFormat.TRANSLUCENT,
        )
        .apply {
          gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
          y = 80
        }

    val composeView =
      ComposeView(context).apply {
        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        setContent {
          MaterialTheme(colorScheme = darkColorScheme()) {
            OverlayContent(
              logsFlow = observeLogs(),
              sessionFlow = observeSession(),
              onStop = { stopAgent() },
              onDismiss = { hide() },
            )
          }
        }
      }

    overlayView = composeView

    try {
      windowManager.addView(composeView, layoutParams)
      _isVisible.value = true
    } catch (e: Exception) {
      Napier.e(tag = TAG, throwable = e) { "Failed to add overlay" }
      overlayView = null
    }
  }

  override fun hide() {
    Napier.d(tag = TAG) { "Hiding overlay" }
    overlayView?.let {
      try {
        windowManager.removeView(it)
      } catch (e: Exception) {
        Napier.e(tag = TAG, throwable = e) { "Failed to remove overlay" }
      }
    }
    overlayView = null
    _isVisible.value = false
  }

  override fun observeLogs(): Flow<List<LogEntryEntity>> {
    val sid = currentSessionId ?: return flowOf(emptyList())
    return sessionRepository.getLogsBySessionIdFlow(sid)
  }

  override fun stopAgent() {
    Napier.d(tag = TAG) { "Stop agent requested from overlay" }
    onStopAgent()
    hide()
  }

  private fun observeSession(): Flow<AgentSessionEntity?> {
    val sid = currentSessionId ?: return flowOf(null)
    return sessionRepository.getSessionByIdFlow(sid)
  }

  companion object {
    private const val TAG = "OverlayUiCtrl"

    @Volatile var stopAgentCallback: (() -> Unit)? = null
  }
}

private class OverlayLifecycleOwner :
  androidx.lifecycle.LifecycleOwner, androidx.savedstate.SavedStateRegistryOwner {
  private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
  private val savedStateRegistryController =
    androidx.savedstate.SavedStateRegistryController.create(this)

  override val lifecycle: androidx.lifecycle.Lifecycle = lifecycleRegistry
  override val savedStateRegistry: androidx.savedstate.SavedStateRegistry =
    savedStateRegistryController.savedStateRegistry

  init {
    savedStateRegistryController.performRestore(null)
  }

  fun handleLifecycleEvent(event: androidx.lifecycle.Lifecycle.Event) {
    lifecycleRegistry.handleLifecycleEvent(event)
  }
}

@Composable
private fun OverlayContent(
  logsFlow: Flow<List<LogEntryEntity>>,
  sessionFlow: Flow<AgentSessionEntity?>,
  onStop: () -> Unit,
  onDismiss: () -> Unit,
) {
  val logs by logsFlow.collectAsState(initial = emptyList())
  val session by sessionFlow.collectAsState(initial = null)
  val listState = rememberLazyListState()
  var expanded by remember { mutableStateOf(true) }

  val isRunning = session?.status == SessionStatus.RUNNING
  val isStopped =
    session?.status == SessionStatus.SUCCESS ||
      session?.status == SessionStatus.ERROR ||
      session?.status == SessionStatus.CANCELLED

  LaunchedEffect(logs.size) {
    if (logs.isNotEmpty()) {
      listState.animateScrollToItem(logs.lastIndex)
    }
  }

  val statusDotColor =
    when (session?.status) {
      SessionStatus.RUNNING -> Color(0xFF4CAF50)
      SessionStatus.SUCCESS -> Color(0xFF82B1FF)
      SessionStatus.ERROR -> Color(0xFFFF8A80)
      else -> Color(0xFF757575)
    }

  val statusText =
    when (session?.status) {
      SessionStatus.RUNNING -> "Talon is working\u2026"
      SessionStatus.SUCCESS -> "Task completed"
      SessionStatus.ERROR -> "Task failed"
      SessionStatus.CANCELLED -> "Task cancelled"
      else -> "Talon"
    }

  Surface(
    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    shape = RoundedCornerShape(16.dp),
    color = Color(0x9d121212),
    shadowElevation = 8.dp,
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusDotColor))
        Spacer(Modifier.width(8.dp))
        Text(
          text = statusText,
          style = MaterialTheme.typography.labelMedium,
          color = Color.White,
          modifier = Modifier.weight(1f),
        )

        IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
          Text(
            text = if (expanded) "\u2796" else "\u2795",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
          )
        }

        Spacer(Modifier.width(4.dp))

        if (isRunning) {
          IconButton(
            onClick = onStop,
            modifier = Modifier.size(28.dp).clip(CircleShape),
            colors =
              IconButtonDefaults.iconButtonColors(
                containerColor = Color(0xFFE53935),
                contentColor = Color.White,
              ),
          ) {
            Text(text = "\u25A0", color = Color.White, style = MaterialTheme.typography.labelSmall)
          }
        } else if (isStopped) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp).clip(CircleShape),
            colors =
              IconButtonDefaults.iconButtonColors(
                containerColor = Color(0xFF424242),
                contentColor = Color.White,
              ),
          ) {
            Text(text = "\u2715", color = Color.White, style = MaterialTheme.typography.labelSmall)
          }
        }
      }

      AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
      ) {
        Column {
          Spacer(Modifier.height(8.dp))

          if (logs.isEmpty()) {
            Text(
              text = "Starting agent\u2026",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White.copy(alpha = 0.6f),
            )
          } else {
            LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxWidth().height(200.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              items(logs.takeLast(MAX_VISIBLE_LOGS), key = { it.id }) { entry ->
                OverlayLogItem(entry)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OverlayLogItem(entry: LogEntryEntity) {
  val icon =
    when (entry.type) {
      "tool_use" -> "\uD83D\uDD27"
      "ai_reply" -> "\uD83E\uDD16"
      "error" -> "\u26A0\uFE0F"
      else -> "\u2139\uFE0F"
    }
  val textColor =
    when (entry.status) {
      LogEntryStatus.ERROR -> Color(0xFFFF8A80)
      LogEntryStatus.ONGOING -> Color(0xFF82B1FF)
      else -> Color.White.copy(alpha = 0.85f)
    }

  Text(
    text = "$icon ${entry.message}",
    style = MaterialTheme.typography.bodySmall,
    color = textColor,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
  )
}

private const val MAX_VISIBLE_LOGS = 50
