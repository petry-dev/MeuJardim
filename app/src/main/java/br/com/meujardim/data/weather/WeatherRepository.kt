package br.com.meujardim.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

data class WeatherSummary(
    val temperature: Int,
    val maxTemperature: Int,
    val minTemperature: Int,
    val humidity: Int,
    val rainChance: Int,
    val weatherCode: Int,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    val description: String get() = describeWeatherCode(weatherCode)
    val kind: WeatherKind get() = kindOf(weatherCode)
}

enum class WeatherKind { SUN, PARTLY_CLOUDY, CLOUDY, FOG, RAIN, STORM, SNOW }

data class CityResult(val name: String, val region: String, val latitude: Double, val longitude: Double) {
    val label: String get() = if (region.isBlank()) name else "$name, $region"
}

/**
 * Previsão via Open-Meteo (gratuita, sem chave de API).
 * Usa HttpURLConnection + org.json para não depender de bibliotecas extras.
 */
class WeatherRepository {

    @Volatile
    private var cache: Pair<String, WeatherSummary>? = null

    suspend fun forecast(latitude: Double, longitude: Double, forceRefresh: Boolean = false): WeatherSummary {
        val key = "%.3f,%.3f".format(Locale.US, latitude, longitude)
        val cached = cache
        if (!forceRefresh && cached != null && cached.first == key &&
            System.currentTimeMillis() - cached.second.fetchedAt < CACHE_MS
        ) return cached.second

        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${"%.4f".format(Locale.US, latitude)}" +
            "&longitude=${"%.4f".format(Locale.US, longitude)}" +
            "&current=temperature_2m,relative_humidity_2m,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&forecast_days=1"
        val json = JSONObject(get(url))
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        val summary = WeatherSummary(
            temperature = current.getDouble("temperature_2m").roundToInt(),
            maxTemperature = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
            minTemperature = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
            humidity = current.getDouble("relative_humidity_2m").roundToInt(),
            rainChance = daily.getJSONArray("precipitation_probability_max").optInt(0, 0),
            weatherCode = current.getInt("weather_code")
        )
        cache = key to summary
        return summary
    }

    suspend fun searchCity(query: String): List<CityResult> {
        if (query.isBlank()) return emptyList()
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=pt&format=json"))
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            val region = listOfNotNull(
                r.optString("admin1").takeIf { it.isNotBlank() },
                r.optString("country").takeIf { it.isNotBlank() && it != "Brasil" }
            ).joinToString(", ")
            CityResult(
                name = r.getString("name"),
                region = region,
                latitude = r.getDouble("latitude"),
                longitude = r.getDouble("longitude")
            )
        }
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            val code = conn.responseCode
            if (code !in 200..299) error("Serviço de clima respondeu $code")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val CACHE_MS = 30 * 60 * 1000L
    }
}

private fun kindOf(code: Int): WeatherKind = when (code) {
    0, 1 -> WeatherKind.SUN
    2 -> WeatherKind.PARTLY_CLOUDY
    3 -> WeatherKind.CLOUDY
    45, 48 -> WeatherKind.FOG
    in 51..67, in 80..82 -> WeatherKind.RAIN
    in 71..77, 85, 86 -> WeatherKind.SNOW
    in 95..99 -> WeatherKind.STORM
    else -> WeatherKind.CLOUDY
}

private fun describeWeatherCode(code: Int): String = when (code) {
    0 -> "Céu limpo"
    1 -> "Predominantemente limpo"
    2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Neblina"
    51, 53, 55 -> "Garoa"
    56, 57 -> "Garoa congelante"
    61 -> "Chuva fraca"
    63 -> "Chuva moderada"
    65 -> "Chuva forte"
    66, 67 -> "Chuva congelante"
    71, 73, 75, 77 -> "Neve"
    80, 81, 82 -> "Pancadas de chuva"
    85, 86 -> "Pancadas de neve"
    95 -> "Trovoadas"
    96, 99 -> "Trovoadas com granizo"
    else -> "Tempo variável"
}

/**
 * Recomendações gerais a partir do clima. Nunca alteram a agenda de regas sozinhas:
 * apenas sugerem que o usuário verifique o solo.
 */
fun WeatherSummary.recommendations(hasOutdoorPlants: Boolean): List<String> = buildList {
    if (maxTemperature >= 30) add("Dia quente (máx. de $maxTemperature °C): o solo pode secar mais rápido. Vale conferir a umidade dos vasos, principalmente os que pegam sol.")
    if (rainChance >= 60 && hasOutdoorPlants) add("Chance de chuva de $rainChance%: plantas na varanda ou no jardim podem receber água da chuva. Verifique o solo antes de regar.")
    if (humidity < 40) add("Ar seco (umidade de $humidity%): espécies tropicais, como samambaias e marantas, costumam apreciar folhas borrifadas.")
    if (minTemperature <= 10) add("Noite fria (mín. de $minTemperature °C): em dias frios o solo demora mais para secar. Regue preferencialmente pela manhã.")
    if (isEmpty()) add("Clima ameno hoje. Siga a rotina e confira a umidade do solo com o dedo antes de regar.")
}
