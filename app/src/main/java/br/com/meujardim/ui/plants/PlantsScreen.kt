package br.com.meujardim.ui.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.meujardim.AppContainer
import br.com.meujardim.data.db.Plant
import br.com.meujardim.ui.components.EmptyState
import br.com.meujardim.ui.components.PlantRow
import br.com.meujardim.ui.components.appViewModel
import br.com.meujardim.ui.theme.Garden
import br.com.meujardim.util.normalizedForSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class PlantsState(
    val all: List<Plant> = emptyList(),
    val filtered: List<Plant> = emptyList(),
    val query: String = "",
    val rooms: List<String> = emptyList(),
    val selectedRoom: String? = null
)

class PlantsViewModel(app: AppContainer) : ViewModel() {
    private val query = MutableStateFlow("")
    private val room = MutableStateFlow<String?>(null)

    val state: StateFlow<PlantsState> = combine(app.repository.plants, query, room) { plants, q, r ->
        val nq = q.normalizedForSearch()
        PlantsState(
            all = plants,
            filtered = plants.filter { p ->
                (r == null || p.room == r) &&
                    (nq.isBlank() || p.name.normalizedForSearch().contains(nq) ||
                        p.species.normalizedForSearch().contains(nq) ||
                        p.room.normalizedForSearch().contains(nq))
            },
            query = q,
            rooms = plants.map { it.room }.filter { it.isNotBlank() }.distinct().sorted(),
            selectedRoom = r
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantsState())

    fun setQuery(value: String) { query.value = value }
    fun toggleRoom(value: String) { room.value = if (room.value == value) null else value }
}

@Composable
fun PlantsScreen(onAddPlant: () -> Unit, onOpenPlant: (Long) -> Unit) {
    val vm = appViewModel { PlantsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlant, containerColor = Garden.Green, contentColor = Color.White) {
                Icon(Icons.Rounded.Add, "Adicionar planta")
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                Text("Minhas plantas", style = MaterialTheme.typography.headlineMedium)
                Text(
                    when (state.all.size) {
                        0 -> "Nenhuma planta cadastrada"
                        1 -> "1 planta no seu jardim"
                        else -> "${state.all.size} plantas no seu jardim"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por nome, espécie ou ambiente") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Rounded.Close, "Limpar busca")
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
            }
            if (state.rooms.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.rooms) { room ->
                        FilterChip(
                            selected = state.selectedRoom == room,
                            onClick = { vm.toggleRoom(room) },
                            label = { Text(room) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Garden.GreenLight,
                                selectedLabelColor = Garden.GreenDark
                            )
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
            }

            when {
                state.all.isEmpty() -> EmptyState(
                    title = "Nenhuma planta por aqui",
                    message = "Toque em + para cadastrar a primeira. Você pode tirar uma foto e escolher a espécie para receber sugestões de rega."
                )
                state.filtered.isEmpty() -> EmptyState(
                    title = "Nada encontrado",
                    message = "Nenhuma planta corresponde à busca ou ao filtro escolhido."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filtered, key = { it.id }) { plant ->
                        PlantRow(plant = plant, today = today, onClick = { onOpenPlant(plant.id) })
                    }
                }
            }
        }
    }
}
