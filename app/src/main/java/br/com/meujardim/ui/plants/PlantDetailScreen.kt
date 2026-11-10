package br.com.meujardim.ui.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.meujardim.AppContainer
import br.com.meujardim.data.catalog.SpeciesCatalog
import br.com.meujardim.data.db.Plant
import br.com.meujardim.data.db.WateringLog
import br.com.meujardim.data.db.daysLate
import br.com.meujardim.data.db.wasLate
import br.com.meujardim.ui.components.ConfirmDialog
import br.com.meujardim.ui.components.Pill
import br.com.meujardim.ui.components.PlantAvatar
import br.com.meujardim.ui.components.WateringStatusPill
import br.com.meujardim.ui.components.appViewModel
import br.com.meujardim.ui.history.LogEditDialog
import br.com.meujardim.ui.theme.Garden
import br.com.meujardim.util.formatDateTime
import br.com.meujardim.util.formatShort
import br.com.meujardim.util.relativeLabel
import br.com.meujardim.util.toLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class PlantDetailViewModel(private val app: AppContainer, val plantId: Long) : ViewModel() {
    val plant: StateFlow<Plant?> = app.repository.observePlant(plantId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val logs: StateFlow<List<WateringLog>> = app.repository.observeLogs(plantId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun water() = viewModelScope.launch { app.repository.markWatered(plantId) }

    fun rename(name: String) = viewModelScope.launch {
        val p = app.repository.getPlant(plantId) ?: return@launch
        if (name.isNotBlank()) app.repository.savePlant(p.copy(name = name.trim()))
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        app.repository.getPlant(plantId)?.let { app.repository.deletePlant(it) }
        onDeleted()
    }

    fun updateLog(log: WateringLog) = viewModelScope.launch { app.repository.updateLog(log) }
    fun deleteLog(log: WateringLog) = viewModelScope.launch { app.repository.deleteLog(log) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlantDetailScreen(plantId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val vm = appViewModel { PlantDetailViewModel(it, plantId) }
    val plant by vm.plant.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<WateringLog?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar") } },
                actions = {
                    IconButton(onClick = { onEdit(plantId) }) { Icon(Icons.Rounded.Edit, "Editar") }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "Mais opções") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Renomear") },
                                leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) },
                                onClick = { menuOpen = false; renaming = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Excluir planta", color = Garden.CoralDark) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Garden.CoralDark) },
                                onClick = { menuOpen = false; confirmDelete = true }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { inner ->
        val p = plant ?: return@Scaffold
        val today = LocalDate.now()
        val info = SpeciesCatalog.find(p.species)

        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    PlantAvatar(p.photoPath, p.name, size = 168.dp, corner = 40.dp)
                    Spacer(Modifier.height(14.dp))
                    Text(p.name, style = MaterialTheme.typography.headlineMedium)
                    if (p.species.isNotBlank()) {
                        Text(
                            info?.scientificName?.let { "${p.species} · $it" } ?: p.species,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Garden.InkSoft
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        WateringStatusPill(p, today)
                        Pill(p.room, Garden.LilacLight, Garden.LilacDark, Icons.Rounded.Home)
                    }
                }
            }

            if (p.isOverdue(today)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Garden.CoralLight), shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.padding(16.dp)) {
                            Icon(Icons.Rounded.Info, null, tint = Garden.CoralDark)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "A rega estava prevista para ${p.nextWateringDate.formatShort()} (${p.nextWateringDate.relativeLabel(today)}). " +
                                    "Antes de regar, verifique a umidade do solo e as necessidades da espécie.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Garden.Ink
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        vm.water()
                        scope.launch { snackbar.showSnackbar("Rega registrada para ${p.name}.") }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Garden.Green)
                ) {
                    Icon(Icons.Rounded.WaterDrop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Já reguei!")
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoTile(Icons.Rounded.Repeat, "Frequência",
                        (if (p.frequencyDays == 1) "Todo dia" else "A cada ${p.frequencyDays} dias") +
                            if (p.frequencyIsAuto) "\n(sugestão automática)" else "",
                        Garden.GreenLight, Garden.GreenDark, Modifier.weight(1f))
                    InfoTile(Icons.Rounded.WaterDrop, "Quantidade", "Cerca de ${p.waterAmountMl} ml",
                        Garden.SkyLight, Color(0xFF2B7BB5), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoTile(Icons.Rounded.History, "Última rega",
                        p.lastWateredAt?.formatDateTime() ?: "Não registrada",
                        Garden.YellowLight, Color(0xFF8A6500), Modifier.weight(1f))
                    InfoTile(Icons.Rounded.Event, "Próxima rega",
                        "${p.nextWateringDate.formatShort()}\n(${p.nextWateringDate.relativeLabel(today)})",
                        Garden.LilacLight, Garden.LilacDark, Modifier.weight(1f))
                }
            }
            item {
                InfoTile(Icons.Rounded.LightMode, "Iluminação", "${p.light.label} · ${p.light.description}",
                    Garden.YellowLight, Color(0xFF8A6500), Modifier.fillMaxWidth())
            }

            if (info != null || p.notes.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            if (info != null) {
                                Text("Dica para ${info.popularName}", style = MaterialTheme.typography.titleSmall, color = Garden.GreenDark)
                                Text(info.tip, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (info != null && p.notes.isNotBlank()) Spacer(Modifier.height(12.dp))
                            if (p.notes.isNotBlank()) {
                                Text("Observações", style = MaterialTheme.typography.titleSmall)
                                Text(p.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text("Histórico de regas", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Toque em um registro para corrigir a data ou o horário.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Garden.InkSoft
                )
            }
            if (logs.isEmpty()) {
                item {
                    Text("Nenhuma rega registrada ainda.", style = MaterialTheme.typography.bodyMedium, color = Garden.InkSoft)
                }
            } else {
                items(logs, key = { it.id }) { log ->
                    LogRow(log, onClick = { editingLog = log })
                }
            }
        }
    }

    if (renaming) {
        var newName by remember { mutableStateOf(plant?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Renomear planta") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, label = { Text("Nome") })
            },
            confirmButton = {
                TextButton(onClick = { vm.rename(newName); renaming = false }, enabled = newName.isNotBlank()) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancelar") } }
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Excluir ${plant?.name.orEmpty()}?",
            message = "A planta, a foto e todo o histórico de regas serão apagados. Essa ação não pode ser desfeita.",
            confirmLabel = "Excluir",
            onDismiss = { confirmDelete = false },
            onConfirm = { vm.delete(onBack) }
        )
    }
    editingLog?.let { log ->
        LogEditDialog(
            log = log,
            plantName = plant?.name.orEmpty(),
            onDismiss = { editingLog = null },
            onSave = vm::updateLog,
            onDelete = vm::deleteLog
        )
    }
}

@Composable
private fun InfoTile(
    icon: ImageVector,
    label: String,
    value: String,
    background: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier, shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = background)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = accent)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Garden.Ink)
        }
    }
}

@Composable
fun LogRow(log: WateringLog, onClick: () -> Unit, title: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(if (log.wasLate) Garden.CoralLight else Garden.GreenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.WaterDrop, null, tint = if (log.wasLate) Garden.CoralDark else Garden.Green, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
            Text(log.wateredAt.formatDateTime(), style = MaterialTheme.typography.bodyMedium)
        }
        if (log.wasLate) {
            Pill(
                if (log.daysLate == 1L) "1 dia de atraso" else "${log.daysLate} dias de atraso",
                Garden.CoralLight, Garden.CoralDark
            )
        } else if (log.wateredAt.toLocalDate().isBefore(log.scheduledDate)) {
            Pill("Antecipada", Garden.SkyLight, Color(0xFF2B7BB5))
        } else {
            Pill("No dia", Garden.GreenLight, Garden.GreenDark)
        }
    }
}
