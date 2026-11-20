package com.example.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.MedCueApplication
import com.example.data.DoseStatus
import com.example.data.ScheduleTime
import com.example.tts.TtsAnnouncementService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getLongExtra(MedicineAlarmScheduler.EXTRA_MEDICINE_ID, -1L)
        val timeStr = intent.getStringExtra(MedicineAlarmScheduler.EXTRA_TIME_STR) ?: return
        val isSnooze = intent.getBooleanExtra(MedicineAlarmScheduler.EXTRA_IS_SNOOZE, false)
        if (medicineId <= 0L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as MedCueApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicine = app.repository.getMedicineById(medicineId)
                if (medicine != null && medicine.isActive) {
                    NotificationHelper.showReminderNotification(context, medicine, timeStr)
                    app.alarmScheduler.scheduleMedicine(medicine)
                    ReminderEventBus.emitDue(DueReminderEvent(medicine.id, timeStr, isSnooze))

                    val settings = app.preferencesRepository.currentSettings()
                    if (settings.autoSpeakReminders && medicine.ttsEnabled && !app.isAppInForeground) {
                        val startedService = try {
                            val ttsIntent = TtsAnnouncementService.createIntent(
                                context = context,
                                medicineId = medicine.id,
                                medicineName = medicine.name,
                                timeStr = timeStr,
                                delayForChime = true,
                            )
                            ContextCompat.startForegroundService(context, ttsIntent)
                            true
                        } catch (_: Exception) {
                            false
                        }
                        if (!startedService) {
                            app.ttsManager.speakMedicine(
                                medicine = medicine,
                                scheduledTime = ScheduleTime.parse(timeStr),
                                settings = settings,
                                activeKey = "med_${medicine.id}_$timeStr",
                                isScheduledReminder = true,
                                dedupeKey = "dose_${medicine.id}_$timeStr",
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1L)
        val timeStr = intent.getStringExtra(EXTRA_TIME_STR) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (medicineId <= 0L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as MedCueApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicine = app.repository.getMedicineById(medicineId)
                when (intent.action) {
                    ACTION_MARK_TAKEN -> {
                        if (medicine != null) {
                            app.repository.recordDoseAction(
                                medicine = medicine,
                                scheduledTimeStr = timeStr,
                                status = DoseStatus.TAKEN,
                            )
                        }
                        if (notifId != -1) {
                            NotificationHelper.cancelNotification(context, medicineId, timeStr)
                        }
                    }

                    ACTION_SNOOZE -> {
                        val settings = app.preferencesRepository.currentSettings()
                        if (medicine != null) {
                            app.repository.recordDoseAction(
                                medicine = medicine,
                                scheduledTimeStr = timeStr,
                                status = DoseStatus.SNOOZED,
                            )
                        }
                        app.alarmScheduler.scheduleSnooze(medicineId, timeStr, settings.snoozeMinutes)
                        NotificationHelper.cancelNotification(context, medicineId, timeStr)
                    }

                    ACTION_SPEAK -> {
                        val medName = medicine?.name
                            ?: intent.getStringExtra(EXTRA_MEDICINE_NAME)
                            ?: "Medicine"
                        val startedService = try {
                            val ttsIntent = TtsAnnouncementService.createIntent(
                                context = context,
                                medicineId = medicineId,
                                medicineName = medName,
                                timeStr = timeStr,
                                delayForChime = false,
                            )
                            ContextCompat.startForegroundService(context, ttsIntent)
                            true
                        } catch (_: Exception) {
                            false
                        }
                        if (!startedService && medicine != null) {
                            val settings = app.preferencesRepository.currentSettings()
                            app.ttsManager.speakMedicine(
                                medicine = medicine,
                                scheduledTime = ScheduleTime.parse(timeStr),
                                settings = settings,
                                activeKey = "med_${medicine.id}_$timeStr",
                                isScheduledReminder = true,
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_TAKEN = "com.example.medcue.ACTION_MARK_TAKEN"
        const val ACTION_SNOOZE = "com.example.medcue.ACTION_SNOOZE"
        const val ACTION_SPEAK = "com.example.medcue.ACTION_SPEAK"
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_TIME_STR = "extra_time_str"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            val pendingResult = goAsync()
            val app = context.applicationContext as MedCueApplication
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    app.alarmScheduler.rescheduleAllActive()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
