package com.example

import com.example.data.DayMaskHelper
import com.example.data.FrequencyType
import com.example.data.MedicineEntity
import com.example.data.ScheduleTime
import com.example.data.TimeBucket
import com.example.reminder.MedicineAlarmScheduler
import com.example.tts.MedicineTtsFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MedCueDomainUnitTest {

    @Test
    fun scheduleTime_parsesAndFormats12h() {
        val morning = ScheduleTime.parse("08:05")
        assertNotNull(morning)
        assertEquals("8:05 AM", morning!!.toFormatted12h())
        assertEquals(TimeBucket.MORNING, morning.timeBucket())

        val evening = ScheduleTime.parse("20:30")
        assertNotNull(evening)
        assertEquals("8:30 PM", evening!!.toFormatted12h())
        assertEquals(TimeBucket.EVENING, evening.timeBucket())

        assertNull(ScheduleTime.parse("25:00"))
    }

    @Test
    fun ttsFormatter_expandsUnitsAndIncludesInstructions() {
        val med = MedicineEntity(
            name = "Metformin",
            dosage = "500 mg",
            quantity = "1 tablet",
            instructions = "Take with breakfast",
            scheduleTimesCsv = "08:00",
        )
        val spoken = MedicineTtsFormatter.formatMedicineAnnouncement(
            medicine = med,
            scheduledTime = ScheduleTime(8, 0),
            includeInstructions = true,
            isScheduledReminder = true,
        )
        assertTrue(spoken.startsWith("Time to take Metformin."))
        assertTrue(spoken.contains("500 milligrams"))
        assertTrue(spoken.contains("Metformin"))
        assertTrue(spoken.contains("Take with breakfast"))
    }

    @Test
    fun alarmScheduler_computesNextFutureTrigger() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 08:00 already passed at 10:00 -> schedules tomorrow 08:00
        val next = MedicineAlarmScheduler.computeNextTriggerMillis(
            scheduleTime = ScheduleTime(8, 0),
            frequencyType = FrequencyType.DAILY,
            daysMask = DayMaskHelper.ALL_DAYS,
            nowMillis = now,
        )
        assertNotNull(next)
        val cal = Calendar.getInstance().apply { timeInMillis = next!! }
        assertEquals(21, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun dayMaskHelper_formatsWeekdaysAndWeekends() {
        val weekdaysMask = (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4)
        assertEquals("Weekdays", DayMaskHelper.formatDaysSummary(FrequencyType.SPECIFIC_DAYS, weekdaysMask))

        val weekendMask = (1 shl 5) or (1 shl 6)
        assertEquals("Weekends", DayMaskHelper.formatDaysSummary(FrequencyType.SPECIFIC_DAYS, weekendMask))
    }
}
