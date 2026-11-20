package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.data.MedCueDatabase
import com.example.data.MedicineRepository
import com.example.data.PreferencesRepository
import com.example.reminder.MedicineAlarmScheduler
import com.example.reminder.NotificationHelper
import com.example.tts.MedicineTtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedCueApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var isAppInForeground: Boolean = false
        private set

    private var startedActivityCount = 0

    val database: MedCueDatabase by lazy {
        MedCueDatabase.getInstance(this)
    }

    val repository: MedicineRepository by lazy {
        MedicineRepository(database.medicineDao(), database.doseLogDao())
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(this)
    }

    val alarmScheduler: MedicineAlarmScheduler by lazy {
        MedicineAlarmScheduler(this, repository)
    }

    lateinit var ttsManager: MedicineTtsManager
        private set

    override fun onCreate() {
        super.onCreate()
        ttsManager = MedicineTtsManager(this)
        NotificationHelper.createChannels(this)

        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStarted(activity: Activity) {
                    startedActivityCount++
                    isAppInForeground = startedActivityCount > 0
                }

                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) {
                    startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                    isAppInForeground = startedActivityCount > 0
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )

        applicationScope.launch {
            alarmScheduler.rescheduleAllActive()
        }
    }
}
