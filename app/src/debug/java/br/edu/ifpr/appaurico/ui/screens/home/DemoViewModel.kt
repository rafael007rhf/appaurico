package br.edu.ifpr.appaurico.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpr.appaurico.core.demo.DemoSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel dos controles de feira (somente debug). */
@HiltViewModel
class DemoViewModel @Inject constructor(
    private val seeder: DemoSeeder,
) : ViewModel() {

    private val _mensagem = MutableStateFlow<String?>(null)
    val mensagem: StateFlow<String?> = _mensagem.asStateFlow()

    fun carregar() {
        viewModelScope.launch {
            seeder.carregar()
            _mensagem.value = "Dados de demonstração carregados"
        }
    }

    fun limpar() {
        viewModelScope.launch {
            seeder.limpar()
            _mensagem.value = "Dados limpos"
        }
    }
}
