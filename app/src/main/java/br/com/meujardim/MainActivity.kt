package br.com.meujardim

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import br.com.meujardim.ui.navigation.MeuJardimNavHost
import br.com.meujardim.ui.theme.MeuJardimTheme

class MainActivity : ComponentActivity() {

    /** Planta a abrir quando o app é aberto por "Ver planta" na notificação. */
    private val pendingPlantId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) readPlantId(intent)
        setContent {
            MeuJardimTheme {
                MeuJardimNavHost(
                    openPlantId = pendingPlantId.value,
                    onPlantOpened = { pendingPlantId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readPlantId(intent)
    }

    private fun readPlantId(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_PLANT_ID, -1L) ?: -1L
        if (id > 0) pendingPlantId.value = id
    }

    companion object {
        const val EXTRA_PLANT_ID = "open_plant_id"
    }
}
