package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

class MedCueTypeConverters {
    @TypeConverter
    fun fromMedicineForm(value: MedicineForm): String = value.name

    @TypeConverter
    fun toMedicineForm(raw: String): MedicineForm =
        enumValues<MedicineForm>().firstOrNull { it.name == raw } ?: MedicineForm.TABLET

    @TypeConverter
    fun fromFrequencyType(value: FrequencyType): String = value.name

    @TypeConverter
    fun toFrequencyType(raw: String): FrequencyType =
        enumValues<FrequencyType>().firstOrNull { it.name == raw } ?: FrequencyType.DAILY

    @TypeConverter
    fun fromDoseStatus(value: DoseStatus): String = value.name

    @TypeConverter
    fun toDoseStatus(raw: String): DoseStatus =
        enumValues<DoseStatus>().firstOrNull { it.name == raw } ?: DoseStatus.TAKEN
}

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY isActive DESC, createdAt DESC")
    fun observeAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE isActive = 1")
    suspend fun getAllActiveMedicines(): List<MedicineEntity>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun getMedicineById(id: Long): MedicineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("UPDATE medicines SET isActive = :active WHERE id = :id")
    suspend fun setMedicineActive(id: Long, active: Boolean)
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs WHERE dateKey = :dateKey ORDER BY actedAtMillis DESC")
    fun observeLogsForDate(dateKey: Int): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE dateKey >= :fromDateKey ORDER BY dateKey DESC, actedAtMillis DESC")
    fun observeLogsSince(fromDateKey: Int): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs ORDER BY dateKey DESC, actedAtMillis DESC LIMIT 150")
    fun observeRecentLogs(): Flow<List<DoseLogEntity>>

    @Query(
        "SELECT * FROM dose_logs WHERE medicineId = :medicineId AND dateKey = :dateKey AND scheduledTimeStr = :timeStr LIMIT 1",
    )
    suspend fun findLogForDose(medicineId: Long, dateKey: Int, timeStr: String): DoseLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: DoseLogEntity): Long

    @Query(
        "DELETE FROM dose_logs WHERE medicineId = :medicineId AND dateKey = :dateKey AND scheduledTimeStr = :timeStr",
    )
    suspend fun deleteLogForDose(medicineId: Long, dateKey: Int, timeStr: String)
}
