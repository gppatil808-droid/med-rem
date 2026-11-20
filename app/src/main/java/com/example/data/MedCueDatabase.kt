package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MedicineEntity::class, DoseLogEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(MedCueTypeConverters::class)
abstract class MedCueDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        @Volatile
        private var instance: MedCueDatabase? = null

        fun getInstance(context: Context): MedCueDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedCueDatabase::class.java,
                    "medcue_database",
                ).build().also { instance = it }
            }
        }
    }
}
