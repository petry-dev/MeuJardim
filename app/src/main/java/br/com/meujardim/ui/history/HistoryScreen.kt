package br.com.meujardim.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import br.com.meujardim.AppContainer
import br.com.meujardim.data.db.LogEntry
import br.com.meujardim.data.db.Plant
import br.com.meujardim.data.db.WateringLog
import br.com.meujardim.data.db.wasLate
import br.com.meujardim.ui.components.EmptyState
import br.com.meujardim.ui.components.appViewModel
import br.com.meujardim.ui.plants.LogRow
import br.com.meujardim.ui.theme.Garden
import br.com.meujardim.util.formatDayMonth
import br.com.meujardim.util.formatMonthYear
import br.com.meujardim.util.toLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class HistoryViewModel(private val app: AppContainer) : ViewModel() {
    val entries: StateFlow<List<LogEntry>> = app.repository.allLogEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val plants: StateFlow<List<Plant>> = app.repository.plants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateLog(log: WateringLog) = viewModelScope.launch { app.repository.updateLog(log) }
    fun deleteLog(log: WateringLog) = viewModelScope.launch { app.repository.deleteLog(log) }
}

private enum class DayMark { NONE, ON_TIME, LATE, PENDING_OVERDUE }

@Composable
fun HistoryScreen() {
    val vm = appViewModel { HistoryViewModel(it) }
    val allEntries by vm.entries.collectAsStateWithLifecycle()
    val plants by vm.plants.collectAsStateWithLifecycle()

    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var plantFilter by remember { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<LogEntry?>(null) }

    val today = LocalDate.now()
    val entries = allEntries.filter { plantFilter == null || it.log.plantId == plantFilter }
    val byDay = entries.groupBy { it.log.wateredAt.toLocalDate() }
    // Datas previstas que já passaram sem rega registrada (plantas atualmente atrasadas).
    val overdueDays = plants
        .filter { (plantFilter == null || it.id == plantFilter) && it.isOverdue(today) }
        .map { it.nextWateringDate }
        .toSet()

    val visible = when (val d = selectedDay) {
        null -> entries.filter { YearMonth.from(it.log.wateredAt.toLocalDate()) == month }
        else -> byDay[d].orEmpty()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
                Text("Histórico de regas", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${allEntries.size} registros no total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (plants.size > 1) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = plantFilter == null,
                            onClick = { plantFilter = null },
                            label = { Text("Todas") },
                            colors = historyChipColors()
                        )
                    }
                    items(plants, key = { it.id }) { p ->
                        FilterChip(
                            selected = plantFilter == p.id,
                            onClick = { plantFilter = if (plantFilter == p.id) null else p.id },
                            label = { Text(p.name) },
                            colors = historyChipColors()
                        )
                    }
                }
            }
        } else {
            item { Spacer(Modifier.height(12.dp)) }
        }

        item {
            CalendarCard(
                month = month,
                today = today,
                selected = selectedDay,
                markFor = { day ->
                    val logs = byDay[day].orEmpty()
                    when {
                        logs.any { it.log.wasLate } -> DayMark.LATE
                        logs.isNotEmpty() -> DayMark.ON_TIME
                        day in overdueDays -> DayMark.PENDING_OVERDUE
                        else -> DayMark.NONE
                    }
                },
                onPrev = { month = month.minusMonths(1); selectedDay = null },
                onNext = { month = month.plusMonths(1); selectedDay = null },
                onSelect = { day -> selectedDay = if (selectedDay == day) null else day },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedDay?.let { "Regas em ${it.formatDayMonth()}" } ?: "Regas em ${month.atDay(1).formatMonthYear()}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (selectedDay != null) TextButton(onClick = { selectedDay = null }) { Text("Ver mês") }
            }
        }

        if (visible.isEmpty()) {
            item {
                EmptyState(
                    title = "Nenhuma rega registrada",
                    message = if (allEntries.isEmpty())
                        "Quando você confirmar uma rega, ela aparece aqui com data e horário."
                    else "Não há registros para o período selecionado."
                )
            }
        } else {
            items(visible, key = { it.log.id }) { entry ->
                Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    LogRow(entry.log, onClick = { editing = entry }, title = entry.plantName)
                }
            }
        }
    }

    editing?.let { entry ->
        LogEditDialog(
            log = entry.log,
            plantName = entry.plantName,
            onDismiss = { editing = null },
            onSave = vm::updateLog,
            onDelete = vm::deleteLog
        )
    }
}

@Composable
private fun historyChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Garden.GreenLight,
    selectedLabelColor = Garden.GreenDark
)

@Composable
private fun CalendarCard(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate?,
    markFor: (LocalDate) -> DayMark,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Mês anterior") }
                Text(
                    month.atDay(1).formatMonthYear(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onNext, enabled = month < YearMonth.now()) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Próximo mês")
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                listOf("D", "S", "T", "Q", "Q", "S", "S").forEach {
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = Garden.InkSoft
                    )
                }
            }
            // Semana começando no domingo, como no calendário brasileiro.
            val first = month.atDay(1)
            val offset = first.dayOfWeek.value % 7
            val cells = offset + month.lengthOfMonth()
            val rows = (cells + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val dayNumber = r * 7 + c - offset + 1
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                            if (dayNumber in 1..month.lengthOfMonth()) {
                                val day = month.atDay(dayNumber)
                                DayCell(day, today, selected == day, markFor(day), onSelect)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Legend(Garden.Green, "No dia")
                Legend(Garden.CoralDark, "Com atraso")
                Legend(Garden.Yellow, "Pendente")
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, today: LocalDate, isSelected: Boolean, mark: DayMark, onSelect: (LocalDate) -> Unit) {
    val bg = when {
        isSelected -> Garden.Green
        mark == DayMark.ON_TIME -> Garden.GreenLight
        mark == DayMark.LATE -> Garden.CoralLight
        mark == DayMark.PENDING_OVERDUE -> Garden.YellowLight
        else -> Color.Transparent
    }
    val fg = if (isSelected) Color.White else Garden.Ink
    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(bg)
            .then(if (day == today && !isSelected) Modifier.border(1.5.dp, Garden.Green, CircleShape) else Modifier)
            .clickable { onSelect(day) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                color = fg
            )
            if (mark != DayMark.NONE) {
                val dot = when (mark) {
                    DayMark.LATE -> Garden.CoralDark
                    DayMark.PENDING_OVERDUE -> Color(0xFFB8860B)
                    else -> Garden.Green
                }
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSelected) Color.White else dot))
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Garden.InkSoft)
    }
}
