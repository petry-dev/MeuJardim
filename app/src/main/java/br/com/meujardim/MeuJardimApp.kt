package br.com.meujardim

import android.app.Application
import android.content.Context
import br.com.meujardim.data.GardenRepository
import br.com.meujardim.data.db.GardenDatabase
import br.com.meujardim.data.prefs.SettingsRepository
import br.com.meujardim.data.weather.WeatherRepository
import br.com.meujardim.notifications.AlarmScheduler
import br.com.meujardim.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Injeção de dependências manual: simples e suficiente para o tamanho do app. */
class AppContainer(context: Context) {
    val database = GardenDatabase.create(context)
    val settings = SettingsRepository(context)
    val weather = WeatherRepository()
    val scheduler = AlarmScheduler(context)
    val repository = GardenRepository(context, database, scheduler)
}

class MeuJardimApp : Application() {

    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
        appScope.launch { container.scheduler.scheduleAll(container.settings.current()) }
    }
}

val Context.container: AppContainer
    get() = (applicationContext as MeuJardimApp).container
