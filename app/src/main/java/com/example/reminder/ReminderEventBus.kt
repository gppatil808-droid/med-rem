package com.example.reminder

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class DueReminderEvent(
    val medicineId: Long,
    val timeStr: String,
    val isSnooze: Boolean = false,
)

object ReminderEventBus {
    private val _events = MutableSharedFlow<DueReminderEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<DueReminderEvent> = _events.asSharedFlow()

    fun emitDue(event: DueReminderEvent) {
        _events.tryEmit(event)
    }
}
