package com.silentport.silentport.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStatus(status: AppUsageStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): AppUsageStatus {
        return try {
            AppUsageStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            AppUsageStatus.RARE
        }
    }
}
