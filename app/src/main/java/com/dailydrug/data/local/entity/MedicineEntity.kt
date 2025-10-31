package com.dailydrug.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val color: Int,
    val memo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
