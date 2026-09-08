package br.edu.ifpr.appaurico.ui.screens.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpr.appaurico.core.cycle.Ciclo
import br.edu.ifpr.appaurico.data.repository.StimulationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class ReminderUiState(
    val salvando: Boolean = false,
    val concluido: Boolean = false,
    val realizadasHoje: Int = 0,
    val previstasHoje: Int = Ciclo.HORARIOS.size,
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val repository: StimulationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observarRealizadas().collect { estimulacoes ->
                val zona = ZoneId.systemDefault()
                val inicioHoje = LocalDate.now()
                    .atStartOfDay(zona)
                    .toInstant()
                    .toEpochMilli()
                val inicioAmanha = LocalDate.now()
                    .plusDays(1)
                    .atStartOfDay(zona)
                    .toInstant()
                    .toEpochMilli()

                val realizadasHoje = estimulacoes.count {
                    it.dataHora in inicioHoje until inicioAmanha
                }
                _uiState.update {
                    it.copy(
                        realizadasHoje = realizadasHoje,
                        previstasHoje = Ciclo.HORARIOS.size,
                    )
                }
            }
        }
    }

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
