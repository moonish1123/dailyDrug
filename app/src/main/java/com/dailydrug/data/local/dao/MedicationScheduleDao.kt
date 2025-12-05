package com.dailydrug.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dailydrug.data.local.entity.MedicationScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MedicationScheduleEntity): Long

    @Update
    suspend fun update(entity: MedicationScheduleEntity)

    @Query("SELECT * FROM medication_schedules WHERE id = :id")
    suspend fun getById(id: Long): MedicationScheduleEntity?

    @Query("DELETE FROM medication_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM medication_schedules WHERE isActive = 1")
    fun observeActive(): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE medicineId = :medicineId")
    fun observeByMedicineId(medicineId: Long): Flow<List<MedicationScheduleEntity>>

    @Query("SELECT * FROM medication_schedules WHERE isActive = 1")
    suspend fun getActiveSchedules(): List<MedicationScheduleEntity>
}
