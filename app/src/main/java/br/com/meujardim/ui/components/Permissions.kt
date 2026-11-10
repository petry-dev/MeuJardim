package br.com.meujardim.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.com.meujardim.container
import br.com.meujardim.notifications.NotificationHelper
import br.com.meujardim.ui.theme.Garden

data class PermissionStatus(val notifications: Boolean, val exactAlarms: Boolean) {
    val allGranted get() = notifications && exactAlarms
}

/** Estado das permissões, reavaliado sempre que a tela volta ao primeiro plano. */
@Composable
fun rememberPermissionStatus(): PermissionStatus {
    val context = LocalContext.current
    var status by remember {
        mutableStateOf(
            PermissionStatus(NotificationHelper.hasPermission(context), context.container.scheduler.canScheduleExact())
        )
    }
    LifecycleResumeEffect(Unit) {
        status = PermissionStatus(NotificationHelper.hasPermission(context), context.container.scheduler.canScheduleExact())
        onPauseOrDispose { }
    }
    return status
}

/** Cartões que pedem as permissões necessárias para os lembretes funcionarem com o app fechado. */
@Composable
fun PermissionBanners(status: PermissionStatus, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(modifier) {
        if (!status.notifications) {
            BannerCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "Ative as notificações",
                message = "Sem elas, o Meu Jardim não consegue avisar quando suas plantas precisam de água.",
                action = "Permitir"
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }
            }
        }
        if (!status.exactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BannerCard(
                icon = Icons.Rounded.Alarm,
                title = "Lembretes no horário certo",
                message = "Permita alarmes exatos para que os avisos cheguem pontualmente. Sem isso, o Android pode atrasá-los alguns minutos.",
                action = "Abrir ajustes"
            ) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                )
            }
        }
    }
}

@Composable
private fun BannerCard(icon: ImageVector, title: String, message: String, action: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Garden.LilacLight)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = Garden.LilacDark, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Garden.LilacDark)
                Text(message, style = MaterialTheme.typography.bodySmall, color = Garden.Ink)
                TextButton(onClick = onAction, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text(action, color = Garden.LilacDark)
                }
            }
        }
    }
}
