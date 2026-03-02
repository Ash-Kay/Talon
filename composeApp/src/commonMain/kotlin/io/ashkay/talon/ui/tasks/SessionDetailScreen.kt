package io.ashkay.talon.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ashkay.talon.data.db.LogEntryEntity
import io.ashkay.talon.data.db.LogEntryStatus
import io.ashkay.talon.data.db.LogType
import io.ashkay.talon.data.db.SessionStatus
import io.ashkay.talon.data.db.currentTimeMillis
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import talon.composeapp.generated.resources.Res
import talon.composeapp.generated.resources.session_chat_completed
import talon.composeapp.generated.resources.session_chat_failed
import talon.composeapp.generated.resources.session_chat_hint
import talon.composeapp.generated.resources.session_chat_typing
import talon.composeapp.generated.resources.session_detail_no_logs

@Composable
fun SessionDetailScreen(
  sessionId: Long,
  onBackClick: () -> Unit = {},
  viewModel: SessionDetailViewModel = koinViewModel { parametersOf(sessionId) },
) {
  val state by viewModel.collectAsState()
  val listState = rememberLazyListState()

  viewModel.collectSideEffect { sideEffect ->
    when (sideEffect) {
      SessionDetailSideEffect.ScrollToBottom -> {
        if (state.logs.isNotEmpty()) {
          listState.animateScrollToItem(state.logs.lastIndex)
        }
      }
    }
  }

  LaunchedEffect(state.logs.size) {
    if (state.logs.isNotEmpty()) {
      listState.animateScrollToItem(state.logs.lastIndex)
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    ChatTopBar(
      title = state.session?.goal.orEmpty(),
      subtitle = buildSubtitle(state.session?.status),
      isRunning = state.session?.status == SessionStatus.RUNNING,
      onBackClick = onBackClick,
    )

    Box(
      modifier = Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.background)
    ) {
      if (state.logs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = stringResource(Res.string.session_detail_no_logs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          item { Spacer(Modifier.height(8.dp)) }
          itemsIndexed(state.logs, key = { _, log -> log.id }) { index, entry ->
            val showTimeSeparator =
              shouldShowTimeSeparator(
                current = entry,
                previous = if (index > 0) state.logs[index - 1] else null,
              )
            if (showTimeSeparator) {
              TimeSeparator(formatDateLabel(entry.createdAt))
            }
            ChatBubble(entry = entry)
          }
          item { Spacer(Modifier.height(8.dp)) }
        }
      }
    }

    val canSend =
      state.session?.status == SessionStatus.SUCCESS || state.session?.status == SessionStatus.ERROR

    if (
      canSend && false
    ) { // removed for now since we don't have a way to handle user messages in the agent flow yet
      ChatInputBar(
        value = state.userInput,
        onValueChange = { viewModel.onInputChanged(it) },
        onSend = { viewModel.sendMessage() },
        isSending = state.isSending,
      )
    } else if (state.session?.status == SessionStatus.RUNNING) {
      RunningIndicatorBar()
    }
  }
}

@Composable
private fun ChatTopBar(
  title: String,
  subtitle: String,
  isRunning: Boolean,
  onBackClick: () -> Unit,
) {
  Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 2.dp) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBackClick) {
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }

      Box(
        modifier =
          Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
      ) {
        Text(text = "\uD83E\uDD85", style = MaterialTheme.typography.titleMedium)
      }

      Spacer(Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val subtitleColor by
          animateColorAsState(
            targetValue =
              if (isRunning) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant
          )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          color = subtitleColor,
          maxLines = 1,
        )
      }
    }
  }
}

@Composable
private fun buildSubtitle(status: String?): String {
  return when (status) {
    SessionStatus.RUNNING -> stringResource(Res.string.session_chat_typing)
    SessionStatus.SUCCESS -> stringResource(Res.string.session_chat_completed)
    SessionStatus.ERROR -> stringResource(Res.string.session_chat_failed)
    else -> ""
  }
}

@Composable
private fun TimeSeparator(label: String) {
  Box(
    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun ChatBubble(entry: LogEntryEntity) {
  val isUserMessage = entry.type == LogType.USER_MESSAGE
  val bubbleColor =
    when {
      isUserMessage -> MaterialTheme.colorScheme.primary
      entry.status == LogEntryStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
      entry.status == LogEntryStatus.ONGOING -> MaterialTheme.colorScheme.secondaryContainer
      else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
  val textColor =
    when {
      isUserMessage -> MaterialTheme.colorScheme.onPrimary
      entry.status == LogEntryStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
      else -> MaterialTheme.colorScheme.onSurface
    }
  val timestampColor =
    when {
      isUserMessage -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
      else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

  val bubbleShape =
    if (isUserMessage) {
      RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
      RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }

  val arrangement = if (isUserMessage) Arrangement.End else Arrangement.Start

  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = arrangement,
  ) {
    Surface(shape = bubbleShape, color = bubbleColor, modifier = Modifier.widthIn(max = 300.dp)) {
      Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        val icon =
          when (entry.type) {
            LogType.TOOL_USE -> "\uD83D\uDD27 "
            LogType.AI_REPLY -> "\uD83E\uDD16 "
            LogType.ERROR -> "\u26A0\uFE0F "
            LogType.INFO -> "\u2139\uFE0F "
            else -> ""
          }

        Text(
          text = if (!isUserMessage) "$icon${entry.message}" else entry.message,
          style = MaterialTheme.typography.bodyMedium,
          color = textColor,
        )
        Spacer(Modifier.height(2.dp))
        Text(
          text = formatTime(entry.createdAt),
          style = MaterialTheme.typography.labelSmall,
          color = timestampColor,
          modifier = Modifier.align(Alignment.End),
        )
      }
    }
  }
}

@Composable
private fun ChatInputBar(
  value: String,
  onValueChange: (String) -> Unit,
  onSend: () -> Unit,
  isSending: Boolean,
) {
  Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 4.dp) {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.Bottom,
    ) {
      TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.weight(1f),
        placeholder = {
          Text(
            text = stringResource(Res.string.session_chat_hint),
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        shape = RoundedCornerShape(24.dp),
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
          ),
        maxLines = 4,
      )
      Spacer(Modifier.width(8.dp))
      IconButton(
        onClick = onSend,
        enabled = value.isNotBlank() && !isSending,
        modifier = Modifier.size(48.dp).clip(CircleShape),
        colors =
          IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
      ) {
        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Submit")
      }
    }
  }
}

@Composable
private fun RunningIndicatorBar() {
  Surface(color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 2.dp) {
    Row(
      modifier =
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Text(
        text = stringResource(Res.string.session_chat_typing),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

private fun shouldShowTimeSeparator(current: LogEntryEntity, previous: LogEntryEntity?): Boolean {
  if (previous == null) return true
  val currentDay = current.createdAt / 86_400_000L
  val previousDay = previous.createdAt / 86_400_000L
  return currentDay != previousDay
}

private fun formatDateLabel(millis: Long): String {
  val now = currentTimeMillis()
  val todayStart = now / 86_400_000L
  val messageDay = millis / 86_400_000L
  val diff = todayStart - messageDay

  return when {
    diff == 0L -> "Today"
    diff == 1L -> "Yesterday"
    diff < 7L -> "${diff}d ago"
    else -> {
      val totalDays = millis / 86_400_000L
      val year = 1970 + (totalDays / 365).toInt()
      val dayOfYear = (totalDays % 365).toInt()
      val month = (dayOfYear / 30) + 1
      val day = (dayOfYear % 30) + 1
      "$day/$month/$year"
    }
  }
}

private fun formatTime(millis: Long): String {
  val totalSeconds = millis / 1000
  val hours = ((totalSeconds % 86400) / 3600).toInt()
  val minutes = ((totalSeconds % 3600) / 60).toInt()
  val h = if (hours > 12) hours - 12 else if (hours == 0) 12 else hours
  val amPm = if (hours >= 12) "PM" else "AM"
  return "${h}:${minutes.toString().padStart(2, '0')} $amPm"
}
