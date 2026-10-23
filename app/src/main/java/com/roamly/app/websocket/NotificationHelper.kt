package com.roamly.app.websocket

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.roamly.app.MainActivity
import com.roamly.app.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "roamly_notifications_channel"
        const val CHANNEL_NAME = "Roamly Уведомления"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Уведомления о бронированиях и событиях в Roamly"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        android.media.AudioAttributes.Builder()
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d("NotificationHelper", "✅ Канал уведомлений создан: $CHANNEL_ID")

                // Проверить все каналы
                val channels = notificationManager.notificationChannels
                Log.d("NotificationHelper", "Доступные каналы: ${channels.map { it.id }}")
            } catch (e: Exception) {
                Log.e("NotificationHelper", "❌ Ошибка создания канала: ${e.message}")
            }
        }
    }

    fun showNotification(title: String, message: String, notificationId: String) {
        Log.d("NotificationHelper", "🔔 Пытаюсь показать уведомление: $title")

        // Проверяем разрешение на Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            Log.d("NotificationHelper", "Разрешения на уведомления: $areNotificationsEnabled")

            if (!areNotificationsEnabled) {
                Log.e("NotificationHelper", "⚠️ Уведомления отключены в настройках!")
                return
            }
        }

        try {
            // Создаем Intent для открытия приложения при нажатии на уведомление
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("notification_id", notificationId)
                putExtra("open_notifications", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Иконка для уведомления
            val iconRes = R.drawable.ic_roamly

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setLights(Color.RED, 1000, 1000)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))

            // Для Android 12+ нужно добавлять звук и вибрацию особым образом
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // Для старых версий
                notificationBuilder
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setVibrate(longArrayOf(0, 500, 250, 500))
            }

            // Добавляем действие для просмотра уведомлений
            val viewIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("open_notifications", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val viewPendingIntent = PendingIntent.getActivity(
                context,
                1,
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder.addAction(
                R.drawable.ic_roamly,
                "Посмотреть",
                viewPendingIntent
            )

            // Пытаемся показать уведомление
            with(NotificationManagerCompat.from(context)) {
                try {
                    val uniqueId = NOTIFICATION_ID + System.currentTimeMillis().toInt()
                    notify(uniqueId, notificationBuilder.build())
                    Log.d("NotificationHelper", "✅ Уведомление показано! ID: $uniqueId")
                } catch (e: SecurityException) {
                    Log.e("NotificationHelper", "❌ SecurityException: ${e.message}")
                } catch (e: Exception) {
                    Log.e("NotificationHelper", "❌ Ошибка показа уведомления: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "❌ Ошибка создания уведомления: ${e.message}")
            e.printStackTrace()
        }
    }

    fun dismissAllNotifications() {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error dismissing notifications: ${e.message}")
        }
    }
}