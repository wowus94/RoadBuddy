package ru.vlyashuk.roadbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.data.local.entity.RoadRequestEntity

@Dao
interface RoadRequestDao {
    @Query("SELECT * FROM road_requests")
    fun getAll(): Flow<List<RoadRequestEntity>>

    @Query("SELECT * FROM road_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RoadRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: RoadRequestEntity)

    @Delete
    suspend fun delete(request: RoadRequestEntity)

    @Query("DELETE FROM road_requests")
    suspend fun deleteAll()
}