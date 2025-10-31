package com.dailydrug.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dailydrug.data.local.entity.MedicineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MedicineEntity): Long

    @Update
    suspend fun update(entity: MedicineEntity)

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getById(id: Long): MedicineEntity?

    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE id = :id")
    fun observeById(id: Long): Flow<MedicineEntity?>
}
