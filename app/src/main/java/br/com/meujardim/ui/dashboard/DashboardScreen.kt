package br.com.meujardim.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.meujardim.data.weather.WeatherKind
import br.com.meujardim.data.weather.recommendations
import br.com.meujardim.ui.components.EmptyState
import br.com.meujardim.ui.components.LeafDecoration
import br.com.meujardim.ui.components.PermissionBanners
import br.com.meujardim.ui.components.PlantRow
import br.com.meujardim.ui.components.SectionTitle
import br.com.meujardim.ui.components.StatCard
import br.com.meujardim.ui.components.appViewModel
import br.com.meujardim.ui.components.rememberPermissionStatus
import br.com.meujardim.ui.theme.Garden
import br.com.meujardim.util.formatFull
import br.com.meujardim.util.greetingFor

@Composable
fun DashboardScreen(
    onAddPlant: () -> Unit,
    onOpenPlant: (Long) -> Unit
) {
    val vm = appViewModel { DashboardViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val permissions = rememberPermissionStatus()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPlant,
                containerColor = Garden.Green,
                contentColor = Color.White,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Adicionar planta") }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = inner.calculateBottomPadding() + 96.dp)
        ) {
            item { Header(state.userName, state) }

            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    PermissionBanners(permissions)
                    StatsGrid(state)
                    Spacer(Modifier.height(16.dp))
                    WeatherCard(state, onRefresh = vm::refreshWeather)
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (!state.loading && state.total == 0) {
                item {
                    EmptyState(
                        title = "Seu jardim ainda está vazio",
                        message = "Cadastre sua primeira planta para receber lembretes de rega no horário certo."
                    )
                }
            } else {
                item {
                    SectionTitle(
                        if (state.toWater.isEmpty()) "Tudo em dia hoje" else "Para regar agora",
                        Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (state.toWater.isEmpty()) {
                    item {
                        AllGoodCard(Modifier.padding(horizontal = 20.dp))
                    }
                } else {
                    items(state.toWater, key = { "w${it.id}" }) { plant ->
                        PlantRow(
                            plant = plant,
                            today = state.today,
                            onClick = { onOpenPlant(plant.id) },
                            onWater = { vm.water(plant.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                    if (state.overdue > 0) {
                        item {
                            Text(
                                "Rega atrasada não significa necessariamente um problema: confira a umidade do solo e as necessidades de cada espécie antes de regar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (state.upcoming.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(20.dp))
                        SectionTitle("Próximas regas", Modifier.padding(horizontal = 20.dp))
                        Spacer(Modifier.height(8.dp))
                    }
                    items(state.upcoming, key = { "u${it.id}" }) { plant ->
                        PlantRow(
                            plant = plant,
                            today = state.today,
                            onClick = { onOpenPlant(plant.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(name: String, state: DashboardState) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Garden.GreenLight, MaterialTheme.colorScheme.background)))
    ) {
        LeafDecoration(Modifier.matchParentSize())
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)
        ) {
            Text(
                state.today.formatFull(),
                style = MaterialTheme.typography.labelLarge,
                color = Garden.GreenDark.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (name.isBlank()) "${greetingFor()}!" else "${greetingFor()}, $name!",
                style = MaterialTheme.typography.headlineMedium,
                color = Garden.Ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    state.total == 0 -> "Vamos começar o seu jardim?"
                    state.toWater.isEmpty() -> "Suas plantas estão em dia."
                    state.toWater.size == 1 -> "1 planta espera por você hoje."
                    else -> "${state.toWater.size} plantas esperam por você hoje."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Garden.InkSoft
            )
        }
    }
}

@Composable
private fun StatsGrid(state: DashboardState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Plantas", state.total, Icons.Rounded.LocalFlorist, Garden.GreenLight, Garden.GreenDark, Modifier.weight(1f))
            StatCard("Regar hoje", state.dueToday, Icons.Rounded.WaterDrop, Garden.YellowLight, Color(0xFF8A6500), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Atrasadas", state.overdue, Icons.Rounded.Schedule, Garden.CoralLight, Garden.CoralDark, Modifier.weight(1f))
            StatCard("Em dia", state.upToDate, Icons.Rounded.CheckCircle, Garden.LilacLight, Garden.LilacDark, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WeatherCard(state: DashboardState, onRefresh: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Garden.SkyLight)
    ) {
        Column(Modifier.padding(16.dp)) {
            when (val w = state.weather) {
                WeatherState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Garden.Sky)
                    Spacer(Modifier.width(12.dp))
                    Text("Consultando a previsão...", style = MaterialTheme.typography.bodyMedium)
                }

                is WeatherState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Clima indisponível", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Não foi possível consultar a previsão para ${w.city}. Verifique a conexão.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Garden.InkSoft
                        )
                    }
                    IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Tentar novamente") }
                }

                is WeatherState.Ready -> {
                    val s = w.summary
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(weatherIcon(s.kind), null, tint = weatherTint(s.kind), modifier = Modifier.size(30.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${s.temperature} °C · ${s.description}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${w.city} · máx. ${s.maxTemperature}° / mín. ${s.minTemperature}°",
                                style = MaterialTheme.typography.bodySmall,
                                color = Garden.InkSoft
                            )
                        }
                        IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Atualizar clima", tint = Garden.InkSoft) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WeatherFact(Icons.Rounded.Umbrella, "Chuva", "${s.rainChance}%", Modifier.weight(1f))
                        WeatherFact(Icons.Rounded.WaterDrop, "Umidade", "${s.humidity}%", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(10.dp))
                    s.recommendations(state.hasOutdoorPlants).forEach { tip ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Box(
                                Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(Garden.Green)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tip, style = MaterialTheme.typography.bodySmall, color = Garden.Ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherFact(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.65f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF2B7BB5), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Garden.InkSoft)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun AllGoodCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Garden.GreenLight)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Garden.Green, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Nenhuma rega pendente para hoje. Aproveite para observar folhas e solo.",
                style = MaterialTheme.typography.bodyMedium,
                color = Garden.GreenDark
            )
        }
    }
}

private fun weatherIcon(kind: WeatherKind): ImageVector = when (kind) {
    WeatherKind.SUN -> Icons.Rounded.WbSunny
    WeatherKind.PARTLY_CLOUDY -> Icons.Rounded.WbCloudy
    WeatherKind.CLOUDY -> Icons.Rounded.Cloud
    WeatherKind.FOG -> Icons.Rounded.Dehaze
    WeatherKind.RAIN -> Icons.Rounded.Grain
    WeatherKind.STORM -> Icons.Rounded.Thunderstorm
    WeatherKind.SNOW -> Icons.Rounded.AcUnit
}

private fun weatherTint(kind: WeatherKind): Color = when (kind) {
    WeatherKind.SUN -> Color(0xFFE0A400)
    WeatherKind.PARTLY_CLOUDY -> Color(0xFFE0A400)
    WeatherKind.STORM -> Garden.LilacDark
    else -> Color(0xFF2B7BB5)
}
