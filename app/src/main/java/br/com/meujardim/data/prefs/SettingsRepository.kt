package br.com.meujardim.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Um dos três horários diários de lembrete. index 0 = primeiro, 2 = último. */
data class ReminderSlot(
    val index: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean
)

data class AppSettings(
    val userName: String = "",
    val slots: List<ReminderSlot> = DEFAULT_SLOTS,
    val snoozeMinutes: Int = 60,
    val cityName: String = "Caxias do Sul, RS",
    val latitude: Double = -29.1678,
    val longitude: Double = -51.1794
) {
    val enabledSlots: List<ReminderSlot> get() = slots.filter { it.enabled }.sortedBy { it.hour * 60 + it.minute }

    companion object {
        val DEFAULT_SLOTS = listOf(
            ReminderSlot(0, 9, 0, true),
            ReminderSlot(1, 14, 0, true),
            ReminderSlot(2, 19, 0, true)
        )
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val userName = stringPreferencesKey("user_name")
        val snooze = intPreferencesKey("snooze_minutes")
        val city = stringPreferencesKey("city_name")
        val lat = doublePreferencesKey("latitude")
        val lon = doublePreferencesKey("longitude")
        fun hour(i: Int) = intPreferencesKey("slot_${i}_hour")
        fun minute(i: Int) = intPreferencesKey("slot_${i}_minute")
        fun enabled(i: Int) = booleanPreferencesKey("slot_${i}_enabled")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            userName = p[Keys.userName] ?: defaults.userName,
            slots = AppSettings.DEFAULT_SLOTS.map { d ->
                ReminderSlot(
                    index = d.index,
                    hour = p[Keys.hour(d.index)] ?: d.hour,
                    minute = p[Keys.minute(d.index)] ?: d.minute,
                    enabled = p[Keys.enabled(d.index)] ?: d.enabled
                )
            },
            snoozeMinutes = p[Keys.snooze] ?: defaults.snoozeMinutes,
            cityName = p[Keys.city] ?: defaults.cityName,
            latitude = p[Keys.lat] ?: defaults.latitude,
            longitude = p[Keys.lon] ?: defaults.longitude
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setUserName(name: String) = context.dataStore.edit { it[Keys.userName] = name.trim() }

    suspend fun setSnoozeMinutes(minutes: Int) = context.dataStore.edit { it[Keys.snooze] = minutes }

    suspend fun setSlot(slot: ReminderSlot) = context.dataStore.edit {
        it[Keys.hour(slot.index)] = slot.hour
        it[Keys.minute(slot.index)] = slot.minute
        it[Keys.enabled(slot.index)] = slot.enabled
    }

    suspend fun setCity(name: String, latitude: Double, longitude: Double) = context.dataStore.edit {
        it[Keys.city] = name
        it[Keys.lat] = latitude
        it[Keys.lon] = longitude
    }
}
