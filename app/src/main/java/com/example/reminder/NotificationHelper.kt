package com.example.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.MedicineEntity
import com.example.data.ScheduleTime

object NotificationHelper {
    const val CHANNEL_REMINDERS = "medcue_reminders_v2"
    const val CHANNEL_TTS_SERVICE = "medcue_tts_service"
    const val TTS_SERVICE_NOTIFICATION_ID = 9001

    const val EXTRA_OPEN_MEDICINE_ID = "extra_open_medicine_id"
    const val EXTRA_OPEN_TIME_STR = "extra_open_time_str"
    const val EXTRA_AUTO_SPEAK = "extra_auto_speak"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notification_channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_reminders_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 180, 300)
                setSound(soundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val ttsChannel = NotificationChannel(
                CHANNEL_TTS_SERVICE,
                context.getString(R.string.notification_channel_tts_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_tts_desc)
                setSound(null, null)
            }
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(ttsChannel)
        }
    }

    fun notificationIdFor(medicineId: Long, timeStr: String): Int {
        return (medicineId.toInt() * 1000) + (timeStr.hashCode() and 0x3FF)
    }

    fun buildTtsServiceNotification(context: Context, medicineName: String): Notification {
        createChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_TTS_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_medcue)
            .setContentTitle("Time for $medicineName")
            .setContentText("Speaking medication reminder")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun showReminderNotification(
        context: Context,
        medicine: MedicineEntity,
        timeStr: String,
    ) {
        createChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notifId = notificationIdFor(medicine.id, timeStr)
        val formattedTime = ScheduleTime.parse(timeStr)?.toFormatted12h() ?: timeStr
        val summaryLine = listOf(medicine.dosage, medicine.quantity)
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MEDICINE_ID, medicine.id)
            putExtra(EXTRA_OPEN_TIME_STR, timeStr)
            putExtra(EXTRA_AUTO_SPEAK, true)
        }
        val openPending = PendingIntent.getActivity(
            context,
            notifId + 1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val takenIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_MARK_TAKEN
            putExtra(ReminderActionReceiver.EXTRA_MEDICINE_ID, medicine.id)
            putExtra(ReminderActionReceiver.EXTRA_TIME_STR, timeStr)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val takenPending = PendingIntent.getBroadcast(
            context,
            notifId + 2,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
            putExtra(ReminderActionReceiver.EXTRA_MEDICINE_ID, medicine.id)
            putExtra(ReminderActionReceiver.EXTRA_TIME_STR, timeStr)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            notifId + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val speakIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SPEAK
            putExtra(ReminderActionReceiver.EXTRA_MEDICINE_ID, medicine.id)
            putExtra(ReminderActionReceiver.EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(ReminderActionReceiver.EXTRA_TIME_STR, timeStr)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val speakPending = PendingIntent.getBroadcast(
            context,
            notifId + 4,
            speakIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentText = buildString {
            append("Take ${medicine.name}")
            if (summaryLine.isNotBlank()) append(" ($summaryLine)")
            append(" • $formattedTime")
        }
        val bigText = buildString {
            append(contentText)
            if (medicine.instructions.isNotBlank()) {
                append("\n${medicine.instructions.trim()}")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_medcue)
            .setContentTitle("Medicine Reminder: ${medicine.name}")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .addAction(0, "Take", takenPending)
            .addAction(0, "Snooze", snoozePending)
            .addAction(0, "Speak", speakPending)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }

    fun cancelNotification(context: Context, medicineId: Long, timeStr: String) {
        val notifId = notificationIdFor(medicineId, timeStr)
        NotificationManagerCompat.from(context).cancel(notifId)
    }
}
