package ru.vlyashuk.roadbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "road_requests")
data class RoadRequestEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val type: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val authorName: String = "",
    val contact: String = "",
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)
