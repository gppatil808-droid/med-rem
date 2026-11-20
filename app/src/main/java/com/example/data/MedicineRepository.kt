package com.example.data

import kotlinx.coroutines.flow.Flow

class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val doseLogDao: DoseLogDao,
) {
    fun observeAllMedicines(): Flow<List<MedicineEntity>> = medicineDao.observeAllMedicines()

    fun observeLogsForDate(dateKey: Int): Flow<List<DoseLogEntity>> =
        doseLogDao.observeLogsForDate(dateKey)

    fun observeLogsSince(fromDateKey: Int): Flow<List<DoseLogEntity>> =
        doseLogDao.observeLogsSince(fromDateKey)

    fun observeRecentLogs(): Flow<List<DoseLogEntity>> = doseLogDao.observeRecentLogs()

    suspend fun getAllActiveMedicines(): List<MedicineEntity> =
        medicineDao.getAllActiveMedicines()

    suspend fun getMedicineById(id: Long): MedicineEntity? = medicineDao.getMedicineById(id)

    suspend fun saveMedicine(medicine: MedicineEntity): Long {
        return if (medicine.id == 0L) {
            medicineDao.insertMedicine(medicine)
        } else {
            medicineDao.updateMedicine(medicine)
            medicine.id
        }
    }

    suspend fun deleteMedicine(medicine: MedicineEntity) {
        medicineDao.deleteMedicine(medicine)
    }

    suspend fun setMedicineActive(id: Long, active: Boolean) {
        medicineDao.setMedicineActive(id, active)
    }

    suspend fun recordDoseAction(
        medicine: MedicineEntity,
        scheduledTimeStr: String,
        status: DoseStatus,
        dateKey: Int = LocalDateHelper.todayKey(),
    ) {
        val existing = doseLogDao.findLogForDose(medicine.id, dateKey, scheduledTimeStr)
        val entry = DoseLogEntity(
            id = existing?.id ?: 0L,
            medicineId = medicine.id,
            medicineNameSnapshot = medicine.name,
            dosageSnapshot = medicine.dosage,
            quantitySnapshot = medicine.quantity,
            dateKey = dateKey,
            scheduledTimeStr = scheduledTimeStr,
            status = status,
            actedAtMillis = System.currentTimeMillis(),
        )
        doseLogDao.upsertLog(entry)
    }

    suspend fun clearDoseAction(
        medicineId: Long,
        scheduledTimeStr: String,
        dateKey: Int = LocalDateHelper.todayKey(),
    ) {
        doseLogDao.deleteLogForDose(medicineId, dateKey, scheduledTimeStr)
    }
}
