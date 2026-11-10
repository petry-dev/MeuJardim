package br.com.meujardim.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.meujardim.AppContainer
import br.com.meujardim.container

/** Cria um ViewModel com acesso ao [AppContainer], escopado ao destino de navegação atual. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = LocalContext.current.container
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
