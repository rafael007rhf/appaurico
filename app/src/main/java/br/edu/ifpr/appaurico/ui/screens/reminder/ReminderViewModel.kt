package br.edu.ifpr.appaurico.ui.screens.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpr.appaurico.data.repository.StimulationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderUiState(
    val salvando: Boolean = false,
    val concluido: Boolean = false,
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: StimulationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    fun marcarComoFeito() {
        val estado = _uiState.value
        if (estado.salvando || estado.concluido) return

        _uiState.update { it.copy(salvando = true) }
        viewModelScope.launch {
            repository.registrar(System.currentTimeMillis())
            _uiState.update { it.copy(salvando = false, concluido = true) }
        }
    }
}
