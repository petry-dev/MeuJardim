package br.com.meujardim.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.meujardim.MainActivity
import br.com.meujardim.R
import br.com.meujardim.data.db.Plant
import br.com.meujardim.util.formatDayMonth
import br.com.meujardim.util.relativeLabel

/** Em que momento do ciclo de lembretes a notificação está sendo enviada. */
enum class ReminderStage { FIRST, MIDDLE, LAST, OVERDUE, SNOOZED }

object NotificationHelper {

    const val CHANNEL_REMINDERS = "rega_lembretes"
    const val CHANNEL_OVERDUE = "rega_atrasos"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.channel_reminders_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_OVERDUE,
                context.getString(R.string.channel_overdue_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.channel_overdue_desc) }
        )
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun notificationId(plantId: Long): Int = (plantId % 1_000_000L).toInt() + 1_000

    fun showReminder(context: Context, plant: Plant, stage: ReminderStage) {
        if (!hasPermission(context)) return

        val (title, text) = texts(plant, stage)
        val channel = if (stage == ReminderStage.OVERDUE) CHANNEL_OVERDUE else CHANNEL_REMINDERS

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF2E7D4F.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPlantIntent(context, plant.id))
            .addAction(0, "Já reguei!", actionIntent(context, plant.id, NotificationActionReceiver.ACTION_WATERED))
            .addAction(0, "Lembrar mais tarde", actionIntent(context, plant.id, NotificationActionReceiver.ACTION_SNOOZE))
            .addAction(0, "Ver planta", openPlantIntent(context, plant.id))

        plant.photoPath?.let { loadThumbnail(it) }?.let { bitmap -> builder.setLargeIcon(bitmap) }

        try {
            NotificationManagerCompat.from(context).notify(notificationId(plant.id), builder.build())
        } catch (_: SecurityException) {
            // Permissão revogada entre a checagem e o envio.
        }
    }

    fun cancel(context: Context, plantId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(plantId))
    }

    private fun texts(plant: Plant, stage: ReminderStage): Pair<String, String> {
        val amount = "cerca de ${plant.waterAmountMl} ml"
        val species = plant.species.takeIf { it.isNotBlank() }?.let { "$it · " } ?: ""
        return when (stage) {
            ReminderStage.FIRST -> "Hora de regar: ${plant.name}" to
                "$species$amount. Antes de regar, confira se o solo está seco ao toque."
            ReminderStage.MIDDLE -> "${plant.name} ainda está na lista de hoje" to
                "A rega de hoje continua pendente ($amount). Se já regou, toque em \"Já reguei!\"."
            ReminderStage.LAST -> "Último lembrete de hoje: ${plant.name}" to
                "A rega prevista para hoje ainda não foi registrada ($amount)."
            ReminderStage.SNOOZED -> "Lembrete: regar ${plant.name}" to
                "Você pediu para lembrar mais tarde. $species$amount."
            ReminderStage.OVERDUE -> {
                val days = plant.daysOverdue()
                val whenLabel = if (days <= 1) "ontem" else
                    "${plant.nextWateringDate.relativeLabel()} (${plant.nextWateringDate.formatDayMonth()})"
                "Rega atrasada: ${plant.name}" to
                    "A rega estava prevista para $whenLabel. Verifique a umidade do solo e as " +
                    "necessidades da espécie antes de regar; nem todo atraso significa que a planta precisa de água agora."
            }
        }
    }

    private fun openPlantIntent(context: Context, plantId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse("meujardim://plant/$plantId"))
            .putExtra(MainActivity.EXTRA_PLANT_ID, plantId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(context: Context, plantId: Long, action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("meujardim://action/$plantId"))
            .putExtra(NotificationActionReceiver.EXTRA_PLANT_ID, plantId)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun loadThumbnail(path: String): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
}
