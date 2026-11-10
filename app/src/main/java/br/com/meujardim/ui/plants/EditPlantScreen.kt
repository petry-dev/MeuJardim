package br.com.meujardim.ui.plants

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import br.com.meujardim.data.catalog.DEFAULT_ROOMS
import br.com.meujardim.data.db.LightNeed
import br.com.meujardim.ui.components.DatePickerModal
import br.com.meujardim.ui.components.PlantAvatar
import br.com.meujardim.ui.components.appViewModel
import br.com.meujardim.ui.theme.Garden
import br.com.meujardim.util.PhotoStorage
import br.com.meujardim.util.formatShort
import br.com.meujardim.util.relativeLabel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditPlantScreen(plantId: Long?, onDone: (savedId: Long?) -> Unit) {
    val vm = appViewModel { EditPlantViewModel(it, plantId) }
    val form = vm.form
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cameraTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var showLastPicker by remember { mutableStateOf(false) }
    var showNextPicker by remember { mutableStateOf(false) }
    var customRoom by rememberSaveable { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch { vm.onPhoto(PhotoStorage.importFromUri(context, uri)) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = cameraTarget
        if (ok && path != null) vm.onPhoto(path) else path?.let(PhotoStorage::delete)
        cameraTarget = null
    }

    val leave = {
        vm.discardUnsavedPhoto()
        onDone(null)
    }
    BackHandler(onBack = leave)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (vm.isNew) "Nova planta" else "Editar planta") },
                navigationIcon = {
                    IconButton(onClick = leave) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar") }
                },
                actions = {
                    TextButton(onClick = { vm.save { onDone(it) } }) { Text("Salvar") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { inner ->
        if (!vm.loaded) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Foto
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlantAvatar(form.photoPath, form.name.ifBlank { "Planta" }, size = 96.dp, corner = 28.dp)
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val (file, uri) = PhotoStorage.newCameraTarget(context)
                        cameraTarget = file.absolutePath
                        cameraLauncher.launch(uri)
                    }) {
                        Icon(Icons.Rounded.PhotoCamera, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tirar foto")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Galeria")
                        }
                        if (form.photoPath != null) {
                            IconButton(onClick = { vm.onPhoto(null) }) { Icon(Icons.Rounded.Delete, "Remover foto") }
                        }
                    }
                }
            }

            FormField(
                value = form.name,
                onChange = vm::onName,
                label = "Nome da planta",
                placeholder = "Ex.: Jiboia da sala",
                isError = vm.nameError,
                supporting = if (vm.nameError) "Dê um nome para a planta." else null
            )

            Column {
                FormField(
                    value = form.species,
                    onChange = vm::onSpecies,
                    label = "Espécie",
                    placeholder = "Ex.: Jiboia, Costela-de-adão, Suculenta"
                )
                val suggestions = vm.suggestions()
                if (suggestions.isNotEmpty()) {
                    Card(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        suggestions.forEach { info ->
                            TextButton(onClick = { vm.pickSpecies(info) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(info.popularName, style = MaterialTheme.typography.bodyLarge, color = Garden.Ink)
                                    Text(info.scientificName, style = MaterialTheme.typography.bodySmall, color = Garden.InkSoft)
                                }
                            }
                        }
                    }
                }
                form.matchedSpecies?.let { info ->
                    Text(
                        "Dica: ${info.tip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Garden.GreenDark,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
                }
            }

            // Ambiente
            Column {
                Label("Ambiente")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rooms = (DEFAULT_ROOMS + form.room).filter { it.isNotBlank() }.distinct()
                    rooms.forEach { room ->
                        FilterChip(
                            selected = !customRoom && form.room == room,
                            onClick = { customRoom = false; vm.onRoom(room) },
                            label = { Text(room) },
                            colors = chipColors()
                        )
                    }
                    FilterChip(
                        selected = customRoom,
                        onClick = { customRoom = true; vm.onRoom("") },
                        label = { Text("Outro") },
                        colors = chipColors()
                    )
                }
                if (customRoom) {
                    Spacer(Modifier.height(8.dp))
                    FormField(form.room, vm::onRoom, "Nome do ambiente", "Ex.: Lavanderia")
                }
            }

            // Rega
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(Garden.YellowLight),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF8A6500), modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Sugestão automática", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (form.matchedSpecies != null) "Baseada na espécie ${form.matchedSpecies!!.popularName}"
                                else "Escolha uma espécie do catálogo para sugestões específicas",
                                style = MaterialTheme.typography.bodySmall,
                                color = Garden.InkSoft
                            )
                        }
                        Switch(
                            checked = form.autoFrequency,
                            onCheckedChange = vm::onAutoFrequency,
                            colors = SwitchDefaults.colors(checkedTrackColor = Garden.Green)
                        )
                    }
                    Stepper(
                        label = "Regar a cada",
                        value = if (form.frequencyDays == 1) "1 dia" else "${form.frequencyDays} dias",
                        onMinus = { vm.onFrequency(form.frequencyDays - 1) },
                        onPlus = { vm.onFrequency(form.frequencyDays + 1) }
                    )
                    Stepper(
                        label = "Quantidade aproximada",
                        value = "${form.waterAmountMl} ml",
                        onMinus = { vm.onWaterAmount(form.waterAmountMl - if (form.waterAmountMl > 100) 50 else 25) },
                        onPlus = { vm.onWaterAmount(form.waterAmountMl + if (form.waterAmountMl >= 100) 50 else 25) }
                    )
                    DateRow(
                        label = "Última rega",
                        value = form.lastWatered?.let { "${it.formatShort()} (${it.relativeLabel()})" } ?: "Não informada",
                        onClick = { showLastPicker = true },
                        actionLabel = if (form.lastWatered != null) "Limpar" else null,
                        onAction = { vm.onLastWatered(null) }
                    )
                    DateRow(
                        label = "Próxima rega",
                        value = "${form.nextWatering.formatShort()} (${form.nextWatering.relativeLabel()})",
                        onClick = { showNextPicker = true },
                        actionLabel = if (form.nextOverridden) "Recalcular" else null,
                        onAction = vm::resetNextWatering
                    )
                }
            }

            // Luz
            Column {
                Label("Necessidade de iluminação")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LightNeed.entries.forEach { light ->
                        FilterChip(
                            selected = form.light == light,
                            onClick = { vm.onLight(light) },
                            label = { Text(light.label) },
                            colors = chipColors()
                        )
                    }
                }
                Text(form.light.description, style = MaterialTheme.typography.bodySmall, color = Garden.InkSoft)
            }

            FormField(
                value = form.notes,
                onChange = vm::onNotes,
                label = "Observações",
                placeholder = "Adubação, vaso, cuidados especiais...",
                singleLine = false,
                minLines = 3
            )

            Button(
                onClick = { vm.save { onDone(it) } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Garden.Green)
            ) { Text(if (vm.isNew) "Adicionar ao jardim" else "Salvar alterações") }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showLastPicker) {
        DatePickerModal(
            initial = form.lastWatered ?: LocalDate.now(),
            maxDate = LocalDate.now(),
            title = "Quando foi a última rega?",
            onDismiss = { showLastPicker = false },
            onConfirm = vm::onLastWatered
        )
    }
    if (showNextPicker) {
        DatePickerModal(
            initial = form.nextWatering,
            title = "Próxima rega",
            onDismiss = { showNextPicker = false },
            onConfirm = vm::onNextWatering
        )
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Garden.GreenLight,
    selectedLabelColor = Garden.GreenDark
)

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun FormField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean = false,
    supporting: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    val supportingSlot: (@Composable () -> Unit)? = if (supporting != null) {
        { Text(supporting) }
    } else {
        null
    }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = isError,
        supportingText = supportingSlot,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
    )
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = onMinus) { Icon(Icons.Rounded.Remove, "Diminuir") }
        Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(84.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        FilledTonalIconButton(onClick = onPlus) { Icon(Icons.Rounded.Add, "Aumentar") }
    }
}

@Composable
private fun DateRow(label: String, value: String, onClick: () -> Unit, actionLabel: String?, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(16.dp), tint = Garden.Green)
                Spacer(Modifier.width(6.dp))
                Text(value, color = Garden.GreenDark)
            }
        }
        if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}
