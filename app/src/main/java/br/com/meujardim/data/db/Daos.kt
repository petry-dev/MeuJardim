package br.com.meujardim.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :id")
    fun observe(id: Long): Flow<Plant?>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun get(id: Long): Plant?

    @Query("SELECT * FROM plants WHERE nextWateringDate <= :date ORDER BY nextWateringDate, name COLLATE NOCASE")
    suspend fun getDueOnOrBefore(date: LocalDate): List<Plant>

    @Query("SELECT * FROM plants WHERE snoozedUntil IS NOT NULL")
    suspend fun getSnoozed(): List<Plant>

    @Insert
    suspend fun insert(plant: Plant): Long

    @Update
    suspend fun update(plant: Plant)

    @Delete
    suspend fun delete(plant: Plant)
}

@Dao
interface WateringLogDao {
    @Insert
    suspend fun insert(log: WateringLog): Long

    @Update
    suspend fun update(log: WateringLog)

    @Delete
    suspend fun delete(log: WateringLog)

    @Query("SELECT * FROM watering_logs WHERE id = :id")
    suspend fun get(id: Long): WateringLog?

    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY wateredAt DESC")
    fun observeForPlant(plantId: Long): Flow<List<WateringLog>>

    @Query("SELECT * FROM watering_logs WHERE plantId = :plantId ORDER BY wateredAt DESC LIMIT 1")
    suspend fun latestForPlant(plantId: Long): WateringLog?

    @Query(
        """
        SELECT watering_logs.*, plants.name AS plantName, plants.photoPath AS plantPhoto
        FROM watering_logs
        INNER JOIN plants ON plants.id = watering_logs.plantId
        ORDER BY watering_logs.wateredAt DESC
        """
    )
    fun observeAllEntries(): Flow<List<LogEntry>>
}
