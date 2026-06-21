package br.edu.ifpr.appaurico.ui.screens.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpr.appaurico.core.cycle.Ciclo
import br.edu.ifpr.appaurico.core.notification.ReminderScheduler
import br.edu.ifpr.appaurico.data.repository.ReminderRepository
import br.edu.ifpr.appaurico.domain.model.ReminderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AgendaUiState(
    val proximoRetorno: String = "",
    val estimulacaoAtiva: Boolean = false,
    val retornoAtivo: Boolean = false,
)

@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    val uiState: StateFlow<AgendaUiState> =
        reminderRepository.observarLembretes()
            .map { lembretes ->
                AgendaUiState(
                    proximoRetorno = formatarRetorno(Ciclo.retorno()),
                    estimulacaoAtiva = lembretes.any { it.tipo == ReminderType.ESTIMULACAO && it.ativo },
                    retornoAtivo = lembretes.any { it.tipo == ReminderType.RETORNO && it.ativo },
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AgendaUiState(proximoRetorno = formatarRetorno(Ciclo.retorno())),
            )

    fun onEstimulacaoChange(ativo: Boolean) =
        definir(ReminderType.ESTIMULACAO, ativo, Ciclo.proximaEstimulacaoMillis())

    fun onRetornoChange(ativo: Boolean) =
        definir(ReminderType.RETORNO, ativo, Ciclo.retornoMillis())

    private fun definir(tipo: ReminderType, ativo: Boolean, horario: Long) {
        viewModelScope.launch {
            reminderRepository.definir(tipo, ativo, horario)
            // Reagenda a partir do estado atualizado, ligando ou desligando os alarmes.
            reminderScheduler.reagendarTodos(reminderRepository.observarLembretes().first())
        }
    }
}

private val FORMATO_RETORNO = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm")

private fun formatarRetorno(momento: LocalDateTime): String = momento.format(FORMATO_RETORNO)
