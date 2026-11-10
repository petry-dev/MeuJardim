package br.com.meujardim.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import br.com.meujardim.data.prefs.AppSettings
import br.com.meujardim.data.prefs.ReminderSlot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Agenda os alarmes do sistema que disparam os lembretes, mesmo com o app fechado.
 *
 * Estratégia: um alarme por horário configurado (não por planta). Quando dispara,
 * o [ReminderReceiver] consulta o banco e notifica cada planta pendente. Assim, regar
 * uma planta automaticamente "cancela" os lembretes restantes dela no dia, e a lista
 * de pendências é sempre a do momento do disparo.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** Reagenda os três horários. Chamado ao abrir o app, ao mudar ajustes e após reiniciar. */
    fun scheduleAll(settings: AppSettings) {
        settings.slots.forEach { slot ->
            cancelSlot(slot.index)
            if (slot.enabled) scheduleSlot(slot)
        }
    }

    fun scheduleSlot(slot: ReminderSlot) {
        val triggerAt = nextOccurrence(slot.hour, slot.minute)
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_SLOT)
            .putExtra(ReminderReceiver.EXTRA_SLOT_INDEX, slot.index)
        setAlarm(triggerAt, slotPendingIntent(slot.index, intent))
    }

    private fun cancelSlot(index: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_SLOT)
        alarmManager.cancel(slotPendingIntent(index, intent))
    }

    fun scheduleSnooze(plantId: Long, triggerAt: Long) {
        setAlarm(triggerAt, snoozePendingIntent(plantId))
    }

    fun cancelSnooze(plantId: Long) {
        alarmManager.cancel(snoozePendingIntent(plantId))
    }

    private fun setAlarm(triggerAt: Long, pi: PendingIntent) {
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // Sem permissão de alarme exato: o sistema pode atrasar alguns minutos.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun slotPendingIntent(index: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_SLOT_BASE + index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun snoozePendingIntent(plantId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_SNOOZE_FIRED)
            // O data diferencia os PendingIntents de cada planta.
            .setData(android.net.Uri.parse("meujardim://snooze/$plantId"))
            .putExtra(ReminderReceiver.EXTRA_PLANT_ID, plantId)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_SNOOZE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextOccurrence(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var candidate = LocalDate.now(zone).atTime(LocalTime.of(hour, minute))
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        return candidate.atZone(zone).toInstant().toEpochMilli()
    }

    private companion object {
        const val REQUEST_SLOT_BASE = 100
        const val REQUEST_SNOOZE = 200
    }
}
