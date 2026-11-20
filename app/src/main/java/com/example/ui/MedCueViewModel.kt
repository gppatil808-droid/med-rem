package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MedCueApplication
import com.example.data.DayMaskHelper
import com.example.data.DoseLogEntity
import com.example.data.DoseStatus
import com.example.data.FrequencyType
import com.example.data.LocalDateHelper
import com.example.data.MedicineEntity
import com.example.data.MedicineForm
import com.example.data.MedicineRepository
import com.example.data.PreferencesRepository
import com.example.data.ReminderSettings
import com.example.data.ScheduleTime
import com.example.reminder.DueReminderEvent
import com.example.reminder.MedicineAlarmScheduler
import com.example.reminder.NotificationHelper
import com.example.reminder.ReminderEventBus
import com.example.tts.MedicineTtsFormatter
import com.example.tts.MedicineTtsManager
import com.example.tts.TtsPlaybackState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class TodayDoseItem(
    val medicine: MedicineEntity,
    val scheduleTime: ScheduleTime,
    val status: DoseStatus?,
    val actedAtMillis: Long?,
    val isDueNow: Boolean,
    val isOverdue: Boolean,
) {
    val key: String get() = "${medicine.id}_${scheduleTime.toStorageString()}"
}

data class DailyAdherenceBar(
    val dayKey: Int,
    val shortLabel: String,
    val scheduledCount: Int,
    val takenCount: Int,
) {
    val ratio: Float
        get() = if (scheduledCount <= 0) 0f else (takenCount.toFloat() / scheduledCount.toFloat()).coerceIn(0f, 1f)
}

data class ActiveReminderAlert(
    val medicine: MedicineEntity,
    val scheduleTime: ScheduleTime,
)

data class MedCueUiState(
    val isLoaded: Boolean = false,
    val allMedicines: List<MedicineEntity> = emptyList(),
    val todayDoses: List<TodayDoseItem> = emptyList(),
    val takenTodayCount: Int = 0,
    val totalTodayCount: Int = 0,
    val nextUpcomingDose: TodayDoseItem? = null,
    val weeklyAdherence: List<DailyAdherenceBar> = emptyList(),
    val recentLogs: List<DoseLogEntity> = emptyList(),
    val settings: ReminderSettings = ReminderSettings(),
    val ttsState: TtsPlaybackState = TtsPlaybackState(),
    val activeReminderAlert: ActiveReminderAlert? = null,
    val snackbarMessage: String? = null,
)

class MedCueViewModel(
    private val appContext: Context,
    private val repository: MedicineRepository,
    private val preferencesRepository: PreferencesRepository,
    private val alarmScheduler: MedicineAlarmScheduler,
    private val ttsManager: MedicineTtsManager,
) : ViewModel() {

    private val currentMinuteTick = MutableStateFlow(System.currentTimeMillis())
    private val activeAlertState = MutableStateFlow<ActiveReminderAlert?>(null)
    private val snackbarState = MutableStateFlow<String?>(null)
    private val triggeredReminderKeys = mutableSetOf<String>()

    private val coreDataFlow = combine(
        repository.observeAllMedicines(),
        repository.observeLogsSince(LocalDateHelper.dayKeyDaysAgo(6)),
        repository.observeRecentLogs(),
        preferencesRepository.settingsFlow,
        currentMinuteTick,
    ) { medicines, weekLogs, recentLogs, settings, nowMillis ->
        CoreSnapshot(medicines, weekLogs, recentLogs, settings, nowMillis)
    }

    val uiState: StateFlow<MedCueUiState> = combine(
        coreDataFlow,
        ttsManager.state,
        activeAlertState,
        snackbarState,
    ) { core, tts, alert, snackbar ->
        buildUiState(core, tts, alert, snackbar)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MedCueUiState(),
    )

    init {
        viewModelScope.launch {
            while (true) {
                delay(2_000L)
                currentMinuteTick.value = System.currentTimeMillis()
            }
        }

        viewModelScope.launch {
            uiState.collect { state ->
                if (state.isLoaded) {
                    checkForDueDosesNow(state)
                }
            }
        }

        viewModelScope.launch {
            ReminderEventBus.events.collect { event ->
                handleAlarmBusEvent(event)
            }
        }
    }

    private fun checkForDueDosesNow(state: MedCueUiState) {
        val nowCal = Calendar.getInstance()
        val todayKey = LocalDateHelper.calendarToKey(nowCal)
        val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

        val dueDose = state.todayDoses.firstOrNull { dose ->
            val timeMin = dose.scheduleTime.toMinutesOfDay()
            val triggerKey = "${todayKey}_${dose.medicine.id}_${dose.scheduleTime.toStorageString()}"
            val isPending = dose.status == null
            val isRightAtDueMinute = nowMinutes in timeMin..(timeMin + 1)
            isPending && isRightAtDueMinute && !triggeredReminderKeys.contains(triggerKey)
        } ?: return

        val triggerKey = "${todayKey}_${dueDose.medicine.id}_${dueDose.scheduleTime.toStorageString()}"
        triggeredReminderKeys.add(triggerKey)

        NotificationHelper.showReminderNotification(
            appContext,
            dueDose.medicine,
            dueDose.scheduleTime.toStorageString(),
        )
        activeAlertState.value = ActiveReminderAlert(dueDose.medicine, dueDose.scheduleTime)
        if (state.settings.autoSpeakReminders && dueDose.medicine.ttsEnabled) {
            viewModelScope.launch {
                delay(550L)
                speakDose(
                    medicine = dueDose.medicine,
                    scheduleTime = dueDose.scheduleTime,
                    dedupeKey = "dose_${dueDose.medicine.id}_${dueDose.scheduleTime.toStorageString()}",
                )
            }
        }
    }

    private suspend fun handleAlarmBusEvent(event: DueReminderEvent) {
        val medicine = repository.getMedicineById(event.medicineId) ?: return
        val scheduleTime = ScheduleTime.parse(event.timeStr) ?: return
        val todayKey = LocalDateHelper.todayKey()
        val triggerKey = "${todayKey}_${medicine.id}_${scheduleTime.toStorageString()}"
        val alreadyTriggeredInForeground = !event.isSnooze && !triggeredReminderKeys.add(triggerKey)

        activeAlertState.value = ActiveReminderAlert(medicine, scheduleTime)
        val settings = uiState.value.settings
        if (!alreadyTriggeredInForeground && settings.autoSpeakReminders && medicine.ttsEnabled) {
            delay(550L)
            speakDose(
                medicine = medicine,
                scheduleTime = scheduleTime,
                dedupeKey = "dose_${medicine.id}_${scheduleTime.toStorageString()}",
            )
        }
    }

    fun rescheduleAllAlarms() {
        viewModelScope.launch {
            alarmScheduler.rescheduleAllActive()
        }
    }

    private data class CoreSnapshot(
        val medicines: List<MedicineEntity>,
        val weekLogs: List<DoseLogEntity>,
        val recentLogs: List<DoseLogEntity>,
        val settings: ReminderSettings,
        val nowMillis: Long,
    )

    private fun buildUiState(
        core: CoreSnapshot,
        tts: TtsPlaybackState,
        alert: ActiveReminderAlert?,
        snackbar: String?,
    ): MedCueUiState {
        val nowCal = Calendar.getInstance().apply { timeInMillis = core.nowMillis }
        val todayKey = LocalDateHelper.calendarToKey(nowCal)
        val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

        val todayLogsByKey = core.weekLogs
            .filter { it.dateKey == todayKey }
            .associateBy { "${it.medicineId}_${it.scheduledTimeStr}" }

        val todayDoses = core.medicines
            .filter { it.isScheduledOnCalendarDay(nowCal) }
            .flatMap { med ->
                med.scheduledTimes().map { time ->
                    val logKey = "${med.id}_${time.toStorageString()}"
                    val log = todayLogsByKey[logKey]
                    val timeMin = time.toMinutesOfDay()
                    val isPending = log == null || log.status == DoseStatus.SNOOZED
                    val isDueNow = isPending && nowMinutes in (timeMin - 15)..(timeMin + 45)
                    val isOverdue = isPending && nowMinutes > (timeMin + 45)
                    TodayDoseItem(
                        medicine = med,
                        scheduleTime = time,
                        status = log?.status,
                        actedAtMillis = log?.actedAtMillis,
                        isDueNow = isDueNow,
                        isOverdue = isOverdue,
                    )
                }
            }
            .sortedBy { it.scheduleTime }

        val takenToday = todayDoses.count { it.status == DoseStatus.TAKEN }
        val nextUpcoming = todayDoses.firstOrNull {
            (it.status == null || it.status == DoseStatus.SNOOZED) &&
                it.scheduleTime.toMinutesOfDay() >= nowMinutes - 15
        } ?: todayDoses.firstOrNull { it.status == null || it.status == DoseStatus.SNOOZED }

        val weeklyBars = (6 downTo 0).map { daysAgo ->
            val key = LocalDateHelper.dayKeyDaysAgo(daysAgo, core.nowMillis)
            val dayCal = LocalDateHelper.keyToCalendar(key)
            val scheduledForDay = core.medicines
                .filter { it.isScheduledOnCalendarDay(dayCal) }
                .sumOf { it.scheduledTimes().size }
            val takenForDay = core.weekLogs.count {
                it.dateKey == key && it.status == DoseStatus.TAKEN
            }
            DailyAdherenceBar(
                dayKey = key,
                shortLabel = if (daysAgo == 0) "Today" else LocalDateHelper.shortWeekdayLabel(key),
                scheduledCount = scheduledForDay,
                takenCount = takenForDay,
            )
        }

        return MedCueUiState(
            isLoaded = true,
            allMedicines = core.medicines,
            todayDoses = todayDoses,
            takenTodayCount = takenToday,
            totalTodayCount = todayDoses.size,
            nextUpcomingDose = nextUpcoming,
            weeklyAdherence = weeklyBars,
            recentLogs = core.recentLogs,
            settings = core.settings,
            ttsState = tts,
            activeReminderAlert = alert,
            snackbarMessage = snackbar,
        )
    }

    fun markDoseStatus(medicine: MedicineEntity, time: ScheduleTime, status: DoseStatus) {
        viewModelScope.launch {
            repository.recordDoseAction(
                medicine = medicine,
                scheduledTimeStr = time.toStorageString(),
                status = status,
            )
            NotificationHelper.cancelNotification(appContext, medicine.id, time.toStorageString())
            if (status == DoseStatus.SNOOZED) {
                val mins = uiState.value.settings.snoozeMinutes
                alarmScheduler.scheduleSnooze(medicine.id, time.toStorageString(), mins)
                snackbarState.value = "Snoozed ${medicine.name} for $mins min"
            } else if (status == DoseStatus.TAKEN) {
                snackbarState.value = "Marked ${medicine.name} as taken"
            } else {
                snackbarState.value = "Skipped ${medicine.name}"
            }
            if (activeAlertState.value?.medicine?.id == medicine.id &&
                activeAlertState.value?.scheduleTime == time
            ) {
                activeAlertState.value = null
            }
        }
    }

    fun undoDoseStatus(medicineId: Long, time: ScheduleTime) {
        viewModelScope.launch {
            val todayKey = LocalDateHelper.todayKey()
            triggeredReminderKeys.remove("${todayKey}_${medicineId}_${time.toStorageString()}")
            repository.clearDoseAction(medicineId, time.toStorageString())
        }
    }

    fun saveMedicine(
        existingId: Long?,
        name: String,
        dosage: String,
        quantity: String,
        form: MedicineForm,
        instructions: String,
        colorTag: Int,
        times: List<ScheduleTime>,
        frequencyType: FrequencyType,
        daysMask: Int,
        ttsEnabled: Boolean,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val cleanTimes = times.distinct().sorted()
            val existing = existingId?.takeIf { it > 0L }?.let { repository.getMedicineById(it) }
            val entity = MedicineEntity(
                id = existing?.id ?: 0L,
                name = name.trim(),
                dosage = dosage.trim(),
                quantity = quantity.trim(),
                form = form,
                instructions = instructions.trim(),
                colorTag = colorTag,
                scheduleTimesCsv = cleanTimes.joinToString(",") { it.toStorageString() },
                frequencyType = frequencyType,
                daysMask = if (frequencyType == FrequencyType.DAILY) DayMaskHelper.ALL_DAYS else daysMask,
                ttsEnabled = ttsEnabled,
                isActive = existing?.isActive ?: true,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            val savedId = repository.saveMedicine(entity)
            val savedEntity = entity.copy(id = savedId)
            triggeredReminderKeys.removeAll { it.contains("_${savedId}_") }
            alarmScheduler.scheduleMedicine(savedEntity)
            snackbarState.value = if (existing == null) {
                "Scheduled ${savedEntity.name}"
            } else {
                "Updated ${savedEntity.name}"
            }
            onSaved()
        }
    }

    fun toggleMedicineActive(medicine: MedicineEntity) {
        viewModelScope.launch {
            val nextActive = !medicine.isActive
            repository.setMedicineActive(medicine.id, nextActive)
            if (nextActive) {
                alarmScheduler.scheduleMedicine(medicine.copy(isActive = true))
                snackbarState.value = "Resumed reminders for ${medicine.name}"
            } else {
                alarmScheduler.cancelMedicine(medicine.id)
                snackbarState.value = "Paused reminders for ${medicine.name}"
            }
        }
    }

    fun deleteMedicine(medicine: MedicineEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelMedicine(medicine.id)
            repository.deleteMedicine(medicine)
            snackbarState.value = "Removed ${medicine.name}"
        }
    }

    fun speakDose(medicine: MedicineEntity, scheduleTime: ScheduleTime?, dedupeKey: String? = null) {
        val key = "med_${medicine.id}_${scheduleTime?.toStorageString() ?: "all"}"
        if (dedupeKey == null && ttsManager.state.value.isSpeaking && ttsManager.state.value.activeKey == key) {
            ttsManager.stop()
            return
        }
        ttsManager.speakMedicine(
            medicine = medicine,
            scheduledTime = scheduleTime,
            settings = uiState.value.settings,
            activeKey = key,
            isScheduledReminder = scheduleTime != null,
            dedupeKey = dedupeKey,
        )
    }

    fun speakTodaySummary() {
        if (ttsManager.state.value.isSpeaking &&
            ttsManager.state.value.activeKey == MedicineTtsManager.SUMMARY_KEY
        ) {
            ttsManager.stop()
            return
        }
        val pending = uiState.value.todayDoses
            .filter { it.status == null || it.status == DoseStatus.SNOOZED }
            .map { it.medicine to it.scheduleTime }
        ttsManager.speakTodaySummary(pending, uiState.value.settings)
    }

    fun previewDraftSpeech(
        name: String,
        dosage: String,
        quantity: String,
        instructions: String,
        firstTime: ScheduleTime?,
    ) {
        val previewMed = MedicineEntity(
            name = name.ifBlank { "Sample Medicine" },
            dosage = dosage.ifBlank { "500 mg" },
            quantity = quantity.ifBlank { "1 tablet" },
            instructions = instructions,
            scheduleTimesCsv = firstTime?.toStorageString() ?: "08:00",
        )
        val text = MedicineTtsFormatter.formatMedicineAnnouncement(
            medicine = previewMed,
            scheduledTime = firstTime ?: ScheduleTime(8, 0),
            includeInstructions = uiState.value.settings.includeInstructionsInSpeech,
            isScheduledReminder = true,
        )
        ttsManager.speakText(
            text = text,
            activeKey = "draft_preview",
            rate = uiState.value.settings.ttsSpeechRate,
            pitch = uiState.value.settings.ttsPitch,
        )
    }

    fun testVoiceFromSettings() {
        val settings = uiState.value.settings
        val sampleText = "Medication reminder for 8:00 AM. Please take Vitamin D3, 1000 international units, 1 softgel." +
            if (settings.includeInstructionsInSpeech) " Instructions: Take with breakfast." else ""
        ttsManager.speakText(
            text = sampleText,
            activeKey = "settings_test",
            rate = settings.ttsSpeechRate,
            pitch = settings.ttsPitch,
        )
    }

    fun triggerTestNotification(medicine: MedicineEntity) {
        val time = medicine.scheduledTimes().firstOrNull() ?: ScheduleTime(8, 0)
        NotificationHelper.showReminderNotification(appContext, medicine, time.toStorageString())
        if (uiState.value.settings.autoSpeakReminders && medicine.ttsEnabled) {
            speakDose(medicine, time)
        }
        snackbarState.value = "Sent reminder notification for ${medicine.name}"
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun openReminderFromIntent(medicineId: Long, timeStr: String, autoSpeak: Boolean) {
        if (medicineId <= 0L) return
        viewModelScope.launch {
            val medicine = repository.getMedicineById(medicineId) ?: return@launch
            val time = ScheduleTime.parse(timeStr) ?: medicine.scheduledTimes().firstOrNull() ?: ScheduleTime(8, 0)
            activeAlertState.value = ActiveReminderAlert(medicine, time)
            if (autoSpeak) {
                speakDose(medicine, time)
            }
        }
    }

    fun dismissReminderAlert() {
        activeAlertState.value = null
    }

    fun clearSnackbar() {
        snackbarState.value = null
    }

    fun clearTtsError() {
        ttsManager.clearError()
    }

    fun setAutoSpeakReminders(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoSpeakReminders(enabled) }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch { preferencesRepository.setSpeechRate(rate) }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch { preferencesRepository.setSpeechPitch(pitch) }
    }

    fun setIncludeInstructionsInSpeech(include: Boolean) {
        viewModelScope.launch { preferencesRepository.setIncludeInstructionsInSpeech(include) }
    }

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { preferencesRepository.setSnoozeMinutes(minutes) }
    }

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()

    suspend fun getMedicineById(id: Long): MedicineEntity? = repository.getMedicineById(id)

    companion object {
        fun factory(application: MedCueApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MedCueViewModel(
                        appContext = application.applicationContext,
                        repository = application.repository,
                        preferencesRepository = application.preferencesRepository,
                        alarmScheduler = application.alarmScheduler,
                        ttsManager = application.ttsManager,
                    ) as T
                }
            }
    }
}
