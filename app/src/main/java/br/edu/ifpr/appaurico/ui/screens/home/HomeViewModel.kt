package br.edu.ifpr.appaurico.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpr.appaurico.core.cycle.Ciclo
import br.edu.ifpr.appaurico.data.repository.StimulationRepository
import br.edu.ifpr.appaurico.data.repository.SymptomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val diaCiclo: Int = 1,
    val duracaoCiclo: Int = Ciclo.DURACAO_DIAS,
    val proximaEstimulacao: String = "",
    val estimulacoesFeitas: Int = 0,
    val estimulacoesPrevistas: Int = 0,
    val adesaoPercentual: Int = 0,
    val ultimoRegistroNivel: Int? = null,
    val ultimoRegistroQuando: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    stimulationRepository: StimulationRepository,
    symptomRepository: SymptomRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        stimulationRepository.observarRealizadas(),
        symptomRepository.observarRegistros(),
    ) { estimulacoes, registros ->
        val agora = LocalDateTime.now()
        val inicioMillis = Ciclo.inicioMillis()
        val feitas = estimulacoes.count { it.dataHora >= inicioMillis }
        val previstas = Ciclo.previstasAte(agora)
        val adesao = if (previstas == 0) 0 else (feitas * 100 / previstas).coerceIn(0, 100)
        val ultimo = registros.lastOrNull()

        HomeUiState(
            diaCiclo = Ciclo.diaAtual(agora),
            duracaoCiclo = Ciclo.DURACAO_DIAS,
            proximaEstimulacao = formatarMomento(Ciclo.proximaEstimulacao(agora), agora),
            estimulacoesFeitas = feitas,
            estimulacoesPrevistas = previstas,
            adesaoPercentual = adesao,
            ultimoRegistroNivel = ultimo?.nivel,
            ultimoRegistroQuando = ultimo?.let { formatarMillis(it.dataHora, agora) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}

private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")
private val FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm")

/** Texto amigavel para um momento futuro/recente: "Hoje às 16:00", "Amanhã às 08:00" ou data. */
private fun formatarMomento(momento: LocalDateTime, agora: LocalDateTime): String {
    val hora = momento.format(FORMATO_HORA)
    return when (momento.toLocalDate()) {
        agora.toLocalDate() -> "Hoje às $hora"
        agora.toLocalDate().plusDays(1) -> "Amanhã às $hora"
        else -> momento.format(FORMATO_DATA_HORA)
    }
}

private fun formatarMillis(millis: Long, agora: LocalDateTime): String {
    val momento = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    val hora = momento.format(FORMATO_HORA)
    return when (momento.toLocalDate()) {
        agora.toLocalDate() -> "Hoje às $hora"
        agora.toLocalDate().minusDays(1) -> "Ontem às $hora"
        else -> momento.format(FORMATO_DATA_HORA)
    }
}
