package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Locale

enum class MedicineForm(val label: String) {
    TABLET("Tablet"),
    CAPSULE("Capsule"),
    LIQUID("Liquid"),
    INJECTION("Injection"),
    DROPS("Drops"),
    INHALER("Inhaler"),
    CREAM("Cream"),
}

enum class FrequencyType(val label: String) {
    DAILY("Every Day"),
    SPECIFIC_DAYS("Specific Days"),
}

enum class DoseStatus(val label: String) {
    TAKEN("Taken"),
    SKIPPED("Skipped"),
    SNOOZED("Snoozed"),
}

enum class TimeBucket(val label: String) {
    MORNING("Morning"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night"),
}

data class ScheduleTime(
    val hour: Int,
    val minute: Int,
) : Comparable<ScheduleTime> {

    fun toStorageString(): String =
        String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

    fun toFormatted12h(): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
    }

    fun toMinutesOfDay(): Int = hour * 60 + minute

    fun timeBucket(): TimeBucket = when (hour) {
        in 5..11 -> TimeBucket.MORNING
        in 12..16 -> TimeBucket.AFTERNOON
        in 17..20 -> TimeBucket.EVENING
        else -> TimeBucket.NIGHT
    }

    override fun compareTo(other: ScheduleTime): Int =
        toMinutesOfDay().compareTo(other.toMinutesOfDay())

    companion object {
        fun parse(raw: String): ScheduleTime? {
            val parts = raw.trim().split(":")
            if (parts.size != 2) return null
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            if (h !in 0..23 || m !in 0..59) return null
            return ScheduleTime(h, m)
        }
    }
}

object DayMaskHelper {
    const val ALL_DAYS = 0b1111111 // 127 (Mon..Sun)

    // Bit positions: 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
    val dayShortNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayInitials = listOf("M", "T", "W", "T", "F", "S", "S")

    fun isDayEnabled(mask: Int, dayIndexMon0: Int): Boolean {
        return (mask and (1 shl dayIndexMon0)) != 0
    }

    fun toggleDay(mask: Int, dayIndexMon0: Int): Int {
        return mask xor (1 shl dayIndexMon0)
    }

    fun calendarToDayIndexMon0(calendar: Calendar): Int {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    fun formatDaysSummary(frequencyType: FrequencyType, mask: Int): String {
        if (frequencyType == FrequencyType.DAILY || mask == ALL_DAYS) {
            return "Every day"
        }
        val active = dayShortNames.indices.filter { isDayEnabled(mask, it) }
        if (active.isEmpty()) return "No days selected"
        if (active == listOf(0, 1, 2, 3, 4)) return "Weekdays"
        if (active == listOf(5, 6)) return "Weekends"
        return active.joinToString(", ") { dayShortNames[it] }
    }
}

object LocalDateHelper {
    fun todayKey(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return calendarToKey(cal)
    }

    fun dayKeyDaysAgo(daysAgo: Int, nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        return calendarToKey(cal)
    }

    fun calendarToKey(cal: Calendar): Int {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return year * 10000 + month * 100 + day
    }

    fun keyToCalendar(dayKey: Int): Calendar {
        val year = dayKey / 10000
        val month = ((dayKey % 10000) / 100) - 1
        val day = dayKey % 100
        return Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun formatDayLabel(dayKey: Int, nowMillis: Long = System.currentTimeMillis()): String {
        val today = todayKey(nowMillis)
        val yesterday = dayKeyDaysAgo(1, nowMillis)
        return when (dayKey) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> {
                val cal = keyToCalendar(dayKey)
                val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
                val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                val year = cal.get(Calendar.YEAR)
                "$monthName $dayNum, $year"
            }
        }
    }

    fun shortWeekdayLabel(dayKey: Int): String {
        val cal = keyToCalendar(dayKey)
        return cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: ""
    }

    fun formatTimeMillis(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return ScheduleTime(
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
        ).toFormatted12h()
    }
}

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val dosage: String,
    val quantity: String,
    val form: MedicineForm = MedicineForm.TABLET,
    val instructions: String = "",
    val colorTag: Int = 0,
    val scheduleTimesCsv: String,
    val frequencyType: FrequencyType = FrequencyType.DAILY,
    val daysMask: Int = DayMaskHelper.ALL_DAYS,
    val ttsEnabled: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun scheduledTimes(): List<ScheduleTime> =
        scheduleTimesCsv.split(",")
            .mapNotNull { ScheduleTime.parse(it) }
            .distinct()
            .sorted()

    fun isScheduledOnCalendarDay(calendar: Calendar): Boolean {
        if (!isActive) return false
        if (frequencyType == FrequencyType.DAILY) return true
        val idx = DayMaskHelper.calendarToDayIndexMon0(calendar)
        return DayMaskHelper.isDayEnabled(daysMask, idx)
    }
}

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["medicineId"]),
        Index(value = ["dateKey"]),
    ],
)
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val medicineId: Long,
    val medicineNameSnapshot: String,
    val dosageSnapshot: String,
    val quantitySnapshot: String,
    val dateKey: Int,
    val scheduledTimeStr: String,
    val status: DoseStatus,
    val actedAtMillis: Long = System.currentTimeMillis(),
)
