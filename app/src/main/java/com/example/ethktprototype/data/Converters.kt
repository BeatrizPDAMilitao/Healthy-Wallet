package com.example.ethktprototype.data

import androidx.room.TypeConverter
import com.example.ethktprototype.data.ConditionRequirement
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object Converters {
    @TypeConverter
    fun fromConditionsList(conditions: List<ConditionRequirement>?): String? {
        return conditions?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toConditionsList(data: String?): List<ConditionRequirement>? {
        return data?.let { Json.decodeFromString(it) }
    }
}