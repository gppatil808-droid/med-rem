package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.DayMaskHelper
import com.example.data.FrequencyType
import com.example.data.MedicineEntity
import com.example.data.MedicineRepository
import com.example.data.ScheduleTime
import java.util.Calendar

class MedicineAlarmScheduler(
    private val context: Context,
    private val repository: MedicineRepository,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleMedicine(medicine: MedicineEntity) {
        cancelMedicine(medicine.id)
        if (!medicine.isActive) return

        val times = medicine.scheduledTimes()
        times.forEachIndexed { index, scheduleTime ->
            val triggerAt = computeNextTriggerMillis(
                scheduleTime = scheduleTime,
                frequencyType = medicine.frequencyType,
                daysMask = medicine.daysMask,
            ) ?: return@forEachIndexed

            val pendingIntent = buildAlarmPendingIntent(
                medicineId = medicine.id,
                timeStr = scheduleTime.toStorageString(),
                requestCode = requestCodeForSlot(medicine.id, index),
                isSnooze = false,
            )
            scheduleExactOrFallback(triggerAt, pendingIntent, medicine.id, scheduleTime.toStorageString())
        }
    }

    fun scheduleSnooze(medicineId: Long, timeStr: String, minutes: Int) {
        val triggerAt = System.currentTimeMillis() + (minutes.coerceAtLeast(1) * 60_000L)
        val requestCode = (medicineId.toInt() * 100) + 99
        val pendingIntent = buildAlarmPendingIntent(
            medicineId = medicineId,
            timeStr = timeStr,
            requestCode = requestCode,
            isSnooze = true,
        )
        scheduleExactOrFallback(triggerAt, pendingIntent, medicineId, timeStr)
    }

    fun cancelMedicine(medicineId: Long) {
        for (slot in 0..24) {
            val intent = Intent(appContext, ReminderAlarmReceiver::class.java)
            val existing = PendingIntent.getBroadcast(
                appContext,
                requestCodeForSlot(medicineId, slot),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (existing != null) {
                alarmManager.cancel(existing)
                existing.cancel()
            }
        }
        val snoozeIntent = Intent(appContext, ReminderAlarmReceiver::class.java)
        val snoozeExisting = PendingIntent.getBroadcast(
            appContext,
            (medicineId.toInt() * 100) + 99,
            snoozeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (snoozeExisting != null) {
            alarmManager.cancel(snoozeExisting)
            snoozeExisting.cancel()
        }
    }

    suspend fun rescheduleAllActive() {
        val activeMedicines = repository.getAllActiveMedicines()
        activeMedicines.forEach { scheduleMedicine(it) }
    }

    private fun scheduleExactOrFallback(
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        medicineId: Long,
        timeStr: String,
    ) {
        if (canScheduleExactAlarms()) {
            val showIntent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(NotificationHelper.EXTRA_OPEN_MEDICINE_ID, medicineId)
                putExtra(NotificationHelper.EXTRA_OPEN_TIME_STR, timeStr)
                putExtra(NotificationHelper.EXTRA_AUTO_SPEAK, true)
            }
            val showPending = PendingIntent.getActivity(
                appContext,
                NotificationHelper.notificationIdFor(medicineId, timeStr) + 50,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showPending),
                    pendingIntent,
                )
                return
            } catch (_: SecurityException) {
                // Fall back below
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    }

    private fun buildAlarmPendingIntent(
        medicineId: Long,
        timeStr: String,
        requestCode: Int,
        isSnooze: Boolean,
    ): PendingIntent {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_MEDICINE_ALARM
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_TIME_STR, timeStr)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCodeForSlot(medicineId: Long, slotIndex: Int): Int {
        return (medicineId.toInt() * 100) + slotIndex
    }

    companion object {
        const val ACTION_MEDICINE_ALARM = "com.example.medcue.ACTION_MEDICINE_ALARM"
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_TIME_STR = "extra_time_str"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"

        fun computeNextTriggerMillis(
            scheduleTime: ScheduleTime,
            frequencyType: FrequencyType,
            daysMask: Int,
            nowMillis: Long = System.currentTimeMillis(),
        ): Long? {
            if (frequencyType == FrequencyType.SPECIFIC_DAYS && daysMask == 0) return null

            val candidate = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, scheduleTime.hour)
                set(Calendar.MINUTE, scheduleTime.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (candidate.timeInMillis <= nowMillis + 2_000L) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (frequencyType == FrequencyType.DAILY || daysMask == DayMaskHelper.ALL_DAYS) {
                return candidate.timeInMillis
            }

            repeat(7) {
                val dayIdx = DayMaskHelper.calendarToDayIndexMon0(candidate)
                if (DayMaskHelper.isDayEnabled(daysMask, dayIdx)) {
                    return candidate.timeInMillis
                }
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return null
        }
    }
}
