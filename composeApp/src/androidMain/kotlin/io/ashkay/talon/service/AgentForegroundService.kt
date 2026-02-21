package io.ashkay.talon.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import io.ashkay.talon.MainActivity
import io.ashkay.talon.R
import io.github.aakira.napier.Napier

class AgentForegroundService : Service() {

  override fun onCreate() {
    super.onCreate()
    Napier.i(tag = TAG) { "Service created" }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannel()
    }
  }

  @Suppress("MissingForegroundServiceTypeException")
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Napier.i(tag = TAG) { "Service started" }
    val notification = buildNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    super.onDestroy()
    Napier.i(tag = TAG) { "Service destroyed" }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun createNotificationChannel() {
    val channel = NotificationChannel(CHANNEL_ID, "Talon Agent", NotificationManager.IMPORTANCE_LOW)
    channel.description = "Keeps Talon agent active while performing tasks"
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
  }

  private fun buildNotification(): Notification {
    val pendingIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Notification.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.agent_notification_title))
        .setContentText(getString(R.string.agent_notification_text))
        .setSmallIcon(android.R.drawable.ic_menu_manage)
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .build()
    } else {
      @Suppress("DEPRECATION")
      Notification.Builder(this)
        .setContentTitle(getString(R.string.agent_notification_title))
        .setContentText(getString(R.string.agent_notification_text))
        .setSmallIcon(android.R.drawable.ic_menu_manage)
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .build()
    }
  }

  companion object {
    private const val TAG = "AgentFgService"
    private const val CHANNEL_ID = "talon_agent_channel"
    private const val NOTIFICATION_ID = 1001

    fun start(context: Context) {
      Napier.d(tag = TAG) { "Starting foreground service" }
      val intent = Intent(context, AgentForegroundService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      Napier.d(tag = TAG) { "Stopping foreground service" }
      val intent = Intent(context, AgentForegroundService::class.java)
      context.stopService(intent)
    }
  }
}
