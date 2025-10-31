package com.dailydrug.data.local.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import org.json.JSONArray

class DateTimeConverters {
    private val zone: ZoneId = ZoneId.systemDefault()

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromEpochMillis(value: Long?): LocalDateTime? =
        value?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }

    @TypeConverter
    fun localDateTimeToEpochMillis(dateTime: LocalDateTime?): Long? =
        dateTime?.atZone(zone)?.toInstant()?.toEpochMilli()

    @TypeConverter
    fun fromIsoTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun localTimeToIso(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun fromTimeJson(value: String?): List<LocalTime>? {
        if (value.isNullOrEmpty()) return emptyList()
        val json = JSONArray(value)
        val items = mutableListOf<LocalTime>()
        for (i in 0 until json.length()) {
            val timeString = json.optString(i)
            if (timeString.isNotBlank()) {
                items += LocalTime.parse(timeString.trim())
            }
        }
        return items
    }

    @TypeConverter
    fun timeListToJson(times: List<LocalTime>?): String? {
        if (times.isNullOrEmpty()) return "[]"
        val json = JSONArray()
        times.forEach { json.put(it.toString()) }
        return json.toString()
    }
}
