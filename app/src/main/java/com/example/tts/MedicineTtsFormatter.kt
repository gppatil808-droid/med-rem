package com.example.tts

import com.example.data.MedicineEntity
import com.example.data.ReminderSettings
import com.example.data.ScheduleTime

object MedicineTtsFormatter {

    fun expandDosageForSpeech(rawDosage: String): String {
        val trimmed = rawDosage.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed
            .replace(Regex("""\bmg\b""", RegexOption.IGNORE_CASE), "milligrams")
            .replace(Regex("""\bml\b""", RegexOption.IGNORE_CASE), "milliliters")
            .replace(Regex("""\bmcg\b""", RegexOption.IGNORE_CASE), "micrograms")
            .replace(Regex("""\biu\b""", RegexOption.IGNORE_CASE), "international units")
            .replace(Regex("""\bg\b""", RegexOption.IGNORE_CASE), "grams")
    }

    fun formatMedicineAnnouncement(
        medicine: MedicineEntity,
        scheduledTime: ScheduleTime?,
        includeInstructions: Boolean,
        isScheduledReminder: Boolean = true,
    ): String {
        val spokenDosage = expandDosageForSpeech(medicine.dosage)
        val parts = mutableListOf<String>()

        if (isScheduledReminder) {
            parts.add("Time to take ${medicine.name}.")
        }

        val doseDetail = buildString {
            append("Please take ${medicine.name}")
            if (spokenDosage.isNotBlank()) {
                append(", $spokenDosage")
            }
            if (medicine.quantity.isNotBlank()) {
                append(", ${medicine.quantity}")
            }
            if (scheduledTime != null) {
                append(", scheduled for ${scheduledTime.toFormatted12h()}")
            }
            append(".")
        }
        parts.add(doseDetail)

        if (includeInstructions && medicine.instructions.isNotBlank()) {
            parts.add("Instructions: ${medicine.instructions.trim()}.")
        }

        return parts.joinToString(" ")
    }

    fun formatCabinetOverview(
        medicine: MedicineEntity,
        settings: ReminderSettings,
    ): String {
        val times = medicine.scheduledTimes().joinToString(" and ") { it.toFormatted12h() }
        val spokenDosage = expandDosageForSpeech(medicine.dosage)
        return buildString {
            append("${medicine.name}")
            if (spokenDosage.isNotBlank()) append(", $spokenDosage")
            if (medicine.quantity.isNotBlank()) append(", ${medicine.quantity}")
            if (times.isNotBlank()) append(". Scheduled for $times.")
            if (settings.includeInstructionsInSpeech && medicine.instructions.isNotBlank()) {
                append(" Note: ${medicine.instructions.trim()}.")
            }
        }
    }

    fun formatTodaySummary(
        pendingItems: List<Pair<MedicineEntity, ScheduleTime>>,
        settings: ReminderSettings,
    ): String {
        if (pendingItems.isEmpty()) {
            return "All scheduled doses for today are complete. Great job staying on track."
        }
        val count = pendingItems.size
        val header = if (count == 1) {
            "You have 1 remaining medicine scheduled today."
        } else {
            "You have $count remaining medicines scheduled today."
        }
        val details = pendingItems.take(6).joinToString(" ") { (med, time) ->
            val dosage = expandDosageForSpeech(med.dosage)
            val note = if (settings.includeInstructionsInSpeech && med.instructions.isNotBlank()) {
                " ${med.instructions.trim()}."
            } else {
                ""
            }
            "At ${time.toFormatted12h()}: ${med.name}, $dosage, ${med.quantity}.$note"
        }
        return "$header $details"
    }
}
