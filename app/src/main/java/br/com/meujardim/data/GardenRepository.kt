package br.com.meujardim.data

import android.content.Context
import br.com.meujardim.data.db.GardenDatabase
import br.com.meujardim.data.db.LogEntry
import br.com.meujardim.data.db.Plant
import br.com.meujardim.data.db.WateringLog
import br.com.meujardim.notifications.AlarmScheduler
import br.com.meujardim.notifications.NotificationHelper
import br.com.meujardim.util.PhotoStorage
import br.com.meujardim.util.toLocalDate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Ponto único para alterar plantas e registros de rega. Mantém notificações e
 * alarmes de adiamento coerentes com o estado do banco.
 */
class GardenRepository(
    private val context: Context,
    database: GardenDatabase,
    private val scheduler: AlarmScheduler
) {
    private val plantDao = database.plantDao()
    private val logDao = database.wateringLogDao()

    val plants: Flow<List<Plant>> = plantDao.observeAll()
    val allLogEntries: Flow<List<LogEntry>> = logDao.observeAllEntries()

    fun observePlant(id: Long): Flow<Plant?> = plantDao.observe(id)
    fun observeLogs(plantId: Long): Flow<List<WateringLog>> = logDao.observeForPlant(plantId)

    suspend fun getPlant(id: Long): Plant? = plantDao.get(id)

    suspend fun plantsDueBy(date: LocalDate = LocalDate.now()): List<Plant> = plantDao.getDueOnOrBefore(date)

    suspend fun snoozedPlants(): List<Plant> = plantDao.getSnoozed()

    /** Insere (id == 0) ou atualiza. Retorna o id da planta. */
    suspend fun savePlant(plant: Plant): Long {
        return if (plant.id == 0L) {
            plantDao.insert(plant)
        } else {
            val previous = plantDao.get(plant.id)
            if (previous?.photoPath != null && previous.photoPath != plant.photoPath) {
                PhotoStorage.delete(previous.photoPath)
            }
            plantDao.update(plant)
            // Se a próxima rega foi para o futuro, o aviso pendente deixa de fazer sentido.
            if (!plant.isDue()) clearReminders(plant.id)
            plant.id
        }
    }

    suspend fun deletePlant(plant: Plant) {
        clearReminders(plant.id)
        plant.photoPath?.let(PhotoStorage::delete)
        plantDao.delete(plant)
    }

    /**
     * Registra a rega: grava data e horário, recalcula a próxima rega,
     * cancela lembretes pendentes e atualiza o histórico.
     */
    suspend fun markWatered(plantId: Long, at: Long = System.currentTimeMillis()): Boolean {
        val plant = plantDao.get(plantId) ?: return false
        logDao.insert(
            WateringLog(plantId = plantId, wateredAt = at, scheduledDate = plant.nextWateringDate)
        )
        plantDao.update(
            plant.copy(
                lastWateredAt = at,
                nextWateringDate = at.toLocalDate().plusDays(plant.frequencyDays.toLong()),
                snoozedUntil = null
            )
        )
        clearReminders(plantId)
        return true
    }

    /** "Lembrar mais tarde": some com o aviso atual e agenda um novo. */
    suspend fun snooze(plantId: Long, minutes: Int) {
        val plant = plantDao.get(plantId) ?: return
        val until = System.currentTimeMillis() + minutes * 60_000L
        plantDao.update(plant.copy(snoozedUntil = until))
        NotificationHelper.cancel(context, plantId)
        scheduler.scheduleSnooze(plantId, until)
    }

    suspend fun clearSnooze(plantId: Long) {
        val plant = plantDao.get(plantId) ?: return
        if (plant.snoozedUntil != null) plantDao.update(plant.copy(snoozedUntil = null))
    }

    /** Correção de um registro (data/horário). Recalcula última e próxima rega da planta. */
    suspend fun updateLog(log: WateringLog) {
        logDao.update(log)
        recomputeFromLogs(log.plantId)
    }

    suspend fun deleteLog(log: WateringLog) {
        logDao.delete(log)
        recomputeFromLogs(log.plantId)
    }

    private suspend fun recomputeFromLogs(plantId: Long) {
        val plant = plantDao.get(plantId) ?: return
        val latest = logDao.latestForPlant(plantId)
        val updated = if (latest != null) {
            plant.copy(
                lastWateredAt = latest.wateredAt,
                nextWateringDate = latest.wateredAt.toLocalDate().plusDays(plant.frequencyDays.toLong())
            )
        } else {
            // Sem registros: mantém a próxima data atual, apenas limpa a última rega.
            plant.copy(lastWateredAt = null)
        }
        plantDao.update(updated)
        if (!updated.isDue()) clearReminders(plantId)
    }

    private fun clearReminders(plantId: Long) {
        NotificationHelper.cancel(context, plantId)
        scheduler.cancelSnooze(plantId)
    }
}
