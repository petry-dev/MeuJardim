package br.com.meujardim.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.meujardim.AppContainer
import br.com.meujardim.data.db.LightNeed
import br.com.meujardim.data.db.Plant
import br.com.meujardim.data.prefs.AppSettings
import br.com.meujardim.data.prefs.ReminderSlot
import br.com.meujardim.data.weather.CityResult
import br.com.meujardim.notifications.NotificationHelper
import br.com.meujardim.notifications.ReminderStage
import br.com.meujardim.ui.components.PermissionBanners
import br.com.meujardim.ui.components.TimePickerModal
import br.com.meujardim.ui.components.appViewModel
import br.com.meujardim.ui.components.rememberPermissionStatus
import br.com.meujardim.ui.theme.Garden
import br.com.meujardim.util.formatHourMinute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SettingsViewModel(private val app: AppContainer) : ViewModel() {
    val settings: StateFlow<AppSettings> = app.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    var cityResults by mutableStateOf<List<CityResult>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    var searchError by mutableStateOf(false)
        private set

    fun setName(name: String) = viewModelScope.launch { app.settings.setUserName(name) }

    fun setSnooze(minutes: Int) = viewModelScope.launch { app.settings.setSnoozeMinutes(minutes) }

    /** Salva o horário e reagenda imediatamente os alarmes do sistema. */
    fun setSlot(slot: ReminderSlot) = viewModelScope.launch {
        app.settings.setSlot(slot)
        app.scheduler.scheduleAll(app.settings.current())
    }

    fun searchCity(query: String) = viewModelScope.launch {
        searching = true
        searchError = false
        cityResults = runCatching { app.weather.searchCity(query) }
            .onFailure { searchError = true }
            .getOrDefault(emptyList())
        searching = false
    }

    fun chooseCity(city: CityResult) = viewModelScope.launch {
        app.settings.setCity(city.label, city.latitude, city.longitude)
        cityResults = emptyList()
    }

    fun clearCityResults() { cityResults = emptyList(); searchError = false }

    /** Mostra uma notificação de exemplo, com as mesmas ações das reais. */
    fun sendTest(context: android.content.Context) {
        val sample = Plant(
            id = 0,
            name = "Planta de teste",
            species = "Jiboia",
            room = "Sala",
            frequencyDays = 7,
            frequencyIsAuto = true,
            waterAmountMl = 300,
            nextWateringDate = LocalDate.now(),
            light = LightNeed.LUZ_INDIRETA
        )
        NotificationHelper.showReminder(context, sample, ReminderStage.FIRST)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val vm = appViewModel { SettingsViewModel(it) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val permissions = rememberPermissionStatus()
    val context = LocalContext.current

    var name by remember { mutableStateOf<String?>(null) }
    var editingSlot by remember { mutableStateOf<ReminderSlot?>(null) }
    var citySearchOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)

        PermissionBanners(permissions)

        SettingsCard("Seu nome") {
            val current = name ?: settings.userName
            OutlinedTextField(
                value = current,
                onValueChange = { name = it },
                placeholder = { Text("Como podemos te chamar?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { vm.setName(current) }),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                trailingIcon = {
                    if (name != null && name != settings.userName) {
                        TextButton(onClick = { vm.setName(current); name = null }) { Text("Salvar") }
                    }
                }
            )
            Text("Usado na saudação da tela inicial.", style = MaterialTheme.typography.bodySmall, color = Garden.InkSoft)
        }

        SettingsCard("Horários dos lembretes") {
            Text(
                "Enquanto a rega do dia estiver pendente, você recebe um aviso em cada horário ativo. " +
                    "No dia seguinte, o primeiro horário traz o aviso de atraso.",
                style = MaterialTheme.typography.bodySmall,
                color = Garden.InkSoft
            )
            Spacer(Modifier.height(8.dp))
            val labels = listOf("Primeiro lembrete", "Segundo lembrete", "Último lembrete")
            settings.slots.forEachIndexed { i, slot ->
                if (i > 0) HorizontalDivider(color = Garden.Outline)
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(labels.getOrElse(slot.index) { "Lembrete" }, style = MaterialTheme.typography.bodyLarge)
                        TextButton(
                            onClick = { editingSlot = slot },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            enabled = slot.enabled
                        ) {
                            Text(formatHourMinute(slot.hour, slot.minute), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Switch(
                        checked = slot.enabled,
                        onCheckedChange = { vm.setSlot(slot.copy(enabled = it)) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Garden.Green)
                    )
                }
            }
        }

        SettingsCard("Lembrar mais tarde") {
            Text("Ao tocar em \"Lembrar mais tarde\", o aviso volta depois de:", style = MaterialTheme.typography.bodySmall, color = Garden.InkSoft)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 60, 120, 180).forEach { minutes ->
                    FilterChip(
                        selected = settings.snoozeMinutes == minutes,
                        onClick = { vm.setSnooze(minutes) },
                        label = { Text(if (minutes < 60) "$minutes min" else "${minutes / 60} h") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Garden.GreenLight,
                            selectedLabelColor = Garden.GreenDark
                        )
                    )
                }
            }
        }

        SettingsCard("Cidade para o clima") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Garden.SkyLight),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFF2B7BB5), modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Text(settings.cityName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { citySearchOpen = true }) { Text("Alterar") }
            }
            Text("Previsão fornecida pela Open-Meteo.", style = MaterialTheme.typography.bodySmall, color = Garden.InkSoft)
        }

        SettingsCard("Testar notificações") {
            Text(
                "Envia um aviso de exemplo para conferir som, vibração e as ações rápidas.",
                style = MaterialTheme.typography.bodySmall,
                color = Garden.InkSoft
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.sendTest(context) }, enabled = permissions.notifications) {
                Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Enviar notificação de teste")
            }
        }

        Text(
            "Meu Jardim 1.0 · Os lembretes são uma ajuda para a rotina. Cada planta, vaso e ambiente é diferente: confira sempre a umidade do solo.",
            style = MaterialTheme.typography.bodySmall,
            color = Garden.InkSoft
        )
        Spacer(Modifier.height(24.dp))
    }

    editingSlot?.let { slot ->
        TimePickerModal(
            initialHour = slot.hour,
            initialMinute = slot.minute,
            onDismiss = { editingSlot = null },
            onConfirm = { h, m -> vm.setSlot(slot.copy(hour = h, minute = m)) }
        )
    }

    if (citySearchOpen) {
        CitySearchDialog(
            vm = vm,
            onDismiss = { citySearchOpen = false; vm.clearCityResults() }
        )
    }
}

@Composable
private fun CitySearchDialog(vm: SettingsViewModel, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.clearCityResults() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buscar cidade") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Ex.: Caxias do Sul") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.searchCity(query) }),
                    trailingIcon = { IconButton(onClick = { vm.searchCity(query) }) { Icon(Icons.Rounded.Search, "Buscar") } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors()
                )
                Spacer(Modifier.height(8.dp))
                when {
                    vm.searching -> CircularProgressIndicator(Modifier.padding(8.dp).size(24.dp), strokeWidth = 2.dp)
                    vm.searchError -> Text("Não foi possível buscar agora. Verifique a conexão.", color = Garden.CoralDark)
                    else -> vm.cityResults.forEach { city ->
                        TextButton(
                            onClick = { vm.chooseCity(city); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(city.name, color = Garden.Ink)
                                if (city.region.isNotBlank()) {
                                    Text(city.region, style = MaterialTheme.typography.bodySmall, color = Garden.InkSoft)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
