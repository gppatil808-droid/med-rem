package com.example.tts

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.example.MedCueApplication
import com.example.data.ScheduleTime
import com.example.reminder.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TtsAnnouncementService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val stopTimeoutRunnable = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val medicineName = intent?.getStringExtra(EXTRA_MEDICINE_NAME) ?: "Medicine"
        val medicineId = intent?.getLongExtra(EXTRA_MEDICINE_ID, -1L) ?: -1L
        val timeStr = intent?.getStringExtra(EXTRA_TIME_STR) ?: ""
        val delayForChime = intent?.getBooleanExtra(EXTRA_DELAY_FOR_CHIME, true) ?: true

        val notification = NotificationHelper.buildTtsServiceNotification(this, medicineName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.TTS_SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NotificationHelper.TTS_SERVICE_NOTIFICATION_ID, notification)
        }

        timeoutHandler.removeCallbacks(stopTimeoutRunnable)
        timeoutHandler.postDelayed(stopTimeoutRunnable, 20_000L)

        val app = application as MedCueApplication
        serviceScope.launch {
            if (delayForChime) {
                delay(650L)
            }
            val medicine = if (medicineId > 0L) app.repository.getMedicineById(medicineId) else null
            val settings = app.preferencesRepository.currentSettings()
            if (medicine != null) {
                val scheduleTime = ScheduleTime.parse(timeStr)
                app.ttsManager.speakMedicine(
                    medicine = medicine,
                    scheduledTime = scheduleTime,
                    settings = settings,
                    activeKey = "med_${medicine.id}_$timeStr",
                    isScheduledReminder = true,
                    dedupeKey = "dose_${medicine.id}_$timeStr",
                    onDone = {
                        timeoutHandler.post { stopSelf() }
                    },
                )
            } else {
                val fallbackText = intent?.getStringExtra(EXTRA_FALLBACK_TEXT)
                    ?: "Time to take $medicineName."
                app.ttsManager.speakText(
                    text = fallbackText,
                    activeKey = "service_fallback_$startId",
                    rate = settings.ttsSpeechRate,
                    pitch = settings.ttsPitch,
                    onDone = { timeoutHandler.post { stopSelf() } },
                )
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        timeoutHandler.removeCallbacks(stopTimeoutRunnable)
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MEDICINE_ID = "extra_medicine_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
        const val EXTRA_TIME_STR = "extra_time_str"
        const val EXTRA_FALLBACK_TEXT = "extra_fallback_text"
        const val EXTRA_DELAY_FOR_CHIME = "extra_delay_for_chime"

        fun createIntent(
            context: Context,
            medicineId: Long,
            medicineName: String,
            timeStr: String,
            delayForChime: Boolean = true,
        ): Intent = Intent(context, TtsAnnouncementService::class.java).apply {
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_TIME_STR, timeStr)
            putExtra(EXTRA_DELAY_FOR_CHIME, delayForChime)
        }
    }
}
