package br.com.meujardim.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.meujardim.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Executa trabalho assíncrono mantendo o receiver vivo até terminar. */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/**
 * Disparado nos horários configurados e ao fim de um adiamento.
 * Nunca marca planta como regada: apenas avisa o que continua pendente.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.container
        runAsync {
            when (intent.action) {
                ACTION_SLOT -> {
                    val index = intent.getIntExtra(EXTRA_SLOT_INDEX, -1)
                    val settings = app.settings.current()
                    val slot = settings.slots.firstOrNull { it.index == index } ?: return@runAsync
                    // Alarmes exatos não se repetem sozinhos: agenda o de amanhã.
                    if (slot.enabled) app.scheduler.scheduleSlot(slot) else return@runAsync

                    val enabled = settings.enabledSlots
                    val position = enabled.indexOfFirst { it.index == index }
                    val stageForToday = when {
                        position <= 0 -> ReminderStage.FIRST
                        position == enabled.lastIndex -> ReminderStage.LAST
                        else -> ReminderStage.MIDDLE
                    }
                    val today = LocalDate.now()
                    val now = System.currentTimeMillis()
                    app.repository.plantsDueBy(today)
                        .filter { (it.snoozedUntil ?: 0L) <= now } // adiadas têm alarme próprio
                        .forEach { plant ->
                            val stage = if (plant.isOverdue(today)) ReminderStage.OVERDUE else stageForToday
                            NotificationHelper.showReminder(context, plant, stage)
                        }
                }

                ACTION_SNOOZE_FIRED -> {
                    val plantId = intent.getLongExtra(EXTRA_PLANT_ID, -1L)
                    app.repository.clearSnooze(plantId)
                    val plant = app.repository.getPlant(plantId) ?: return@runAsync
                    if (plant.isDue()) {
                        val stage = if (plant.isOverdue()) ReminderStage.OVERDUE else ReminderStage.SNOOZED
                        NotificationHelper.showReminder(context, plant, stage)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_SLOT = "br.com.meujardim.action.REMINDER_SLOT"
        const val ACTION_SNOOZE_FIRED = "br.com.meujardim.action.SNOOZE_FIRED"
        const val EXTRA_SLOT_INDEX = "slot_index"
        const val EXTRA_PLANT_ID = "plant_id"
    }
}

/** Ações rápidas "Já reguei!" e "Lembrar mais tarde", tratadas sem abrir o app. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val plantId = intent.getLongExtra(EXTRA_PLANT_ID, -1L)
        if (plantId < 0) return
        // Some com a notificação na hora, antes do trabalho em segundo plano.
        NotificationHelper.cancel(context, plantId)
        val app = context.container
        runAsync {
            when (intent.action) {
                ACTION_WATERED -> app.repository.markWatered(plantId)
                ACTION_SNOOZE -> app.repository.snooze(plantId, app.settings.current().snoozeMinutes)
            }
        }
    }

    companion object {
        const val ACTION_WATERED = "br.com.meujardim.action.WATERED"
        const val ACTION_SNOOZE = "br.com.meujardim.action.SNOOZE"
        const val EXTRA_PLANT_ID = "plant_id"
    }
}

/** Alarmes do sistema se perdem ao reiniciar o aparelho; aqui eles são recriados. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.container
        runAsync {
            app.scheduler.scheduleAll(app.settings.current())
            val now = System.currentTimeMillis()
            app.repository.snoozedPlants().forEach { plant ->
                val at = maxOf(plant.snoozedUntil ?: now, now + 60_000L)
                app.scheduler.scheduleSnooze(plant.id, at)
            }
        }
    }
}
