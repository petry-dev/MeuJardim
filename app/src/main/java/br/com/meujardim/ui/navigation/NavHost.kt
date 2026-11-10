package br.com.meujardim.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.meujardim.notifications.NotificationHelper
import br.com.meujardim.ui.dashboard.DashboardScreen
import br.com.meujardim.ui.history.HistoryScreen
import br.com.meujardim.ui.plants.EditPlantScreen
import br.com.meujardim.ui.plants.PlantDetailScreen
import br.com.meujardim.ui.plants.PlantsScreen
import br.com.meujardim.ui.settings.SettingsScreen
import br.com.meujardim.ui.theme.Garden

private object Routes {
    const val HOME = "inicio"
    const val PLANTS = "plantas"
    const val HISTORY = "historico"
    const val SETTINGS = "ajustes"
    const val DETAIL = "planta/{id}"
    const val EDIT = "editar-planta?id={id}"
    fun detail(id: Long) = "planta/$id"
    fun edit(id: Long? = null) = if (id == null) "editar-planta" else "editar-planta?id=$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Início", Icons.Rounded.Home),
    Tab(Routes.PLANTS, "Plantas", Icons.Rounded.LocalFlorist),
    Tab(Routes.HISTORY, "Histórico", Icons.Rounded.CalendarMonth),
    Tab(Routes.SETTINGS, "Ajustes", Icons.Rounded.Settings)
)

@Composable
fun MeuJardimNavHost(openPlantId: Long?, onPlantOpened: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = tabs.any { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }

    // Pede a permissão de notificações uma única vez, logo na primeira abertura.
    val context = LocalContext.current
    var askedPermission by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!askedPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasPermission(context)
        ) {
            askedPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Abertura vinda de "Ver planta" na notificação.
    LaunchedEffect(openPlantId) {
        if (openPlantId != null) {
            nav.navigate(Routes.detail(openPlantId)) { launchSingleTop = true }
            onPlantOpened()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = Color.White) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Garden.GreenDark,
                                selectedTextColor = Garden.GreenDark,
                                indicatorColor = Garden.GreenLight
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Routes.HOME) {
                DashboardScreen(
                    onAddPlant = { nav.navigate(Routes.edit()) },
                    onOpenPlant = { nav.navigate(Routes.detail(it)) }
                )
            }
            composable(Routes.PLANTS) {
                PlantsScreen(
                    onAddPlant = { nav.navigate(Routes.edit()) },
                    onOpenPlant = { nav.navigate(Routes.detail(it)) }
                )
            }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                PlantDetailScreen(
                    plantId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.edit(it)) }
                )
            }
            composable(
                Routes.EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val id = entry.arguments?.getLong("id")?.takeIf { it > 0 }
                EditPlantScreen(plantId = id) { savedId ->
                    nav.popBackStack()
                    // Nova planta salva: abre o detalhe dela.
                    if (id == null && savedId != null) nav.navigate(Routes.detail(savedId))
                }
            }
        }
    }
}
