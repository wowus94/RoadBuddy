package ru.vlyashuk.roadbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.vlyashuk.roadbuddy.data.local.entity.RoadRequestEntity

@Dao
interface RoadRequestDao {
    @Query("SELECT * FROM road_requests")
    fun getAll(): Flow<List<RoadRequestEntity>>

    @Query("SELECT * FROM road_requests WHERE id = :id LIMIT 1")
    fun getById(id: String): Flow<RoadRequestEntity?>

    @Query(
        """
        SELECT * FROM road_requests
        WHERE (:type IS NULL OR type = :type)
          AND (:status IS NULL OR status = :status)
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observePageDesc(
        type: String?,
        status: String?,
        limit: Int,
        offset: Int
    ): Flow<List<RoadRequestEntity>>

    @Query(
        """
        SELECT * FROM road_requests
        WHERE (:type IS NULL OR type = :type)
          AND (:status IS NULL OR status = :status)
        ORDER BY createdAt ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun observePageAsc(
        type: String?,
        status: String?,
        limit: Int,
        offset: Int
    ): Flow<List<RoadRequestEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM road_requests
        WHERE (:type IS NULL OR type = :type)
          AND (:status IS NULL OR status = :status)
        """
    )
    fun observeCount(type: String?, status: String?): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM road_requests
        WHERE (:type IS NULL OR type = :type)
          AND (:status IS NULL OR status = :status)
        """
    )
    suspend fun countFiltered(type: String?, status: String?): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: RoadRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(requests: List<RoadRequestEntity>)

    @Delete
    suspend fun delete(request: RoadRequestEntity)

    @Query("DELETE FROM road_requests WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM road_requests")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(requests: List<RoadRequestEntity>) {
        deleteAll()
        upsertAll(requests)
    }
}
