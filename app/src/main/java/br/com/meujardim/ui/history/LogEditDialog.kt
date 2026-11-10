package br.com.meujardim.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import br.com.meujardim.data.db.WateringLog
import br.com.meujardim.ui.components.DatePickerModal
import br.com.meujardim.ui.components.TimePickerModal
import br.com.meujardim.ui.theme.Garden
import br.com.meujardim.util.formatShort
import br.com.meujardim.util.toEpochMillis
import br.com.meujardim.util.toLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime

/** Correção de um registro de rega: altera data/horário ou exclui o registro. */
@Composable
fun LogEditDialog(
    log: WateringLog,
    plantName: String,
    onDismiss: () -> Unit,
    onSave: (WateringLog) -> Unit,
    onDelete: (WateringLog) -> Unit
) {
    var dateTime by remember(log.id) { mutableStateOf(log.wateredAt.toLocalDateTime()) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Corrigir registro") },
        text = {
            Column {
                Text(plantName, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Rega prevista para ${log.scheduledDate.formatShort()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Garden.InkSoft
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Data: ${dateTime.toLocalDate().formatShort()}")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Horário: %02d:%02d".format(dateTime.hour, dateTime.minute))
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Excluir registro", color = Garden.CoralDark)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val millis = dateTime.toEpochMillis().coerceAtMost(System.currentTimeMillis())
                onSave(log.copy(wateredAt = millis))
                onDismiss()
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (pickDate) {
        DatePickerModal(
            initial = dateTime.toLocalDate(),
            maxDate = LocalDate.now(),
            title = "Data da rega",
            onDismiss = { pickDate = false },
            onConfirm = { d -> dateTime = LocalDateTime.of(d, dateTime.toLocalTime()) }
        )
    }
    if (pickTime) {
        TimePickerModal(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute,
            title = "Horário da rega",
            onDismiss = { pickTime = false },
            onConfirm = { h, m -> dateTime = dateTime.withHour(h).withMinute(m) }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir registro?") },
            text = { Text("A última e a próxima rega da planta serão recalculadas a partir dos registros restantes.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(log)
                    onDismiss()
                }) { Text("Excluir", color = Garden.CoralDark) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}
