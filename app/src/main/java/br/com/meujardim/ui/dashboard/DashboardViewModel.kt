package br.com.meujardim.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.meujardim.AppContainer
import br.com.meujardim.data.db.Plant
import br.com.meujardim.data.weather.WeatherSummary
import br.com.meujardim.util.normalizedForSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface WeatherState {
    data object Loading : WeatherState
    data class Ready(val summary: WeatherSummary, val city: String) : WeatherState
    data class Failed(val city: String) : WeatherState
}

data class DashboardState(
    val loading: Boolean = true,
    val userName: String = "",
    val today: LocalDate = LocalDate.now(),
    val total: Int = 0,
    val dueToday: Int = 0,
    val overdue: Int = 0,
    val upToDate: Int = 0,
    /** Plantas para regar agora: atrasadas primeiro, depois as de hoje. */
    val toWater: List<Plant> = emptyList(),
    /** Próximas regas futuras, em ordem de data. */
    val upcoming: List<Plant> = emptyList(),
    val hasOutdoorPlants: Boolean = false,
    val weather: WeatherState = WeatherState.Loading
)

class DashboardViewModel(private val app: AppContainer) : ViewModel() {

    private val weather = MutableStateFlow<WeatherState>(WeatherState.Loading)
    private var lastLocation: Pair<Double, Double>? = null

    val state: StateFlow<DashboardState> = combine(
        app.repository.plants,
        app.settings.settings,
        weather
    ) { plants, settings, weatherState ->
        val today = LocalDate.now()
        val overdue = plants.filter { it.isOverdue(today) }.sortedBy { it.nextWateringDate }
        val dueToday = plants.filter { it.nextWateringDate == today }
        DashboardState(
            loading = false,
            userName = settings.userName,
            today = today,
            total = plants.size,
            dueToday = dueToday.size,
            overdue = overdue.size,
            upToDate = plants.count { it.nextWateringDate.isAfter(today) },
            toWater = overdue + dueToday,
            upcoming = plants.filter { it.nextWateringDate.isAfter(today) }
                .sortedWith(compareBy<Plant>({ it.nextWateringDate }, { it.name.lowercase() }))
                .take(6),
            hasOutdoorPlants = plants.any { p ->
                val room = p.room.normalizedForSearch()
                room.contains("varanda") || room.contains("jardim") || room.contains("quintal") || room.contains("sacada")
            },
            weather = weatherState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    init {
        viewModelScope.launch {
            app.settings.settings
                .map { Triple(it.latitude, it.longitude, it.cityName) }
                .distinctUntilChanged()
                .collect { (lat, lon, city) -> loadWeather(lat, lon, city, force = false) }
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            val s = app.settings.current()
            loadWeather(s.latitude, s.longitude, s.cityName, force = true)
        }
    }

    private suspend fun loadWeather(lat: Double, lon: Double, city: String, force: Boolean) {
        if (!force && lastLocation == lat to lon && weather.value is WeatherState.Ready) return
        lastLocation = lat to lon
        weather.value = WeatherState.Loading
        weather.value = runCatching { app.weather.forecast(lat, lon, forceRefresh = force) }
            .fold(
                onSuccess = { WeatherState.Ready(it, city) },
                onFailure = { WeatherState.Failed(city) }
            )
    }

    fun water(plantId: Long) {
        viewModelScope.launch { app.repository.markWatered(plantId) }
    }
}
