package br.edu.ifpr.appaurico.ui.screens.professional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpr.appaurico.core.cycle.Ciclo
import br.edu.ifpr.appaurico.data.repository.StimulationRepository
import br.edu.ifpr.appaurico.data.repository.SymptomRepository
import br.edu.ifpr.appaurico.ui.screens.evolution.EvolutionPoint
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

/** Nome ficticio: o app de demonstracao nao guarda dados reais de paciente. */
private const val NOME_FICTICIO = "Maria (exemplo)"

/** Quantos registros recentes mostrar na visao do profissional. */
private const val MAX_REGISTROS_RECENTES = 5

data class RegistroResumo(
    val quando: String,
    val nivel: Int,
    val nota: String?,
)

data class ProfessionalUiState(
    val nomePaciente: String = NOME_FICTICIO,
    val diaCiclo: Int = 1,
    val duracaoCiclo: Int = Ciclo.DURACAO_DIAS,
    val adesaoPercentual: Int = 0,
    val estimulacoesFeitas: Int = 0,
    val estimulacoesPrevistas: Int = 0,
    val pontos: List<EvolutionPoint> = emptyList(),
    val nivelInicial: Int? = null,
    val nivelAtual: Int? = null,
    val variacao: Int? = null,
    val registrosRecentes: List<RegistroResumo> = emptyList(),
    val relatorio: String = "",
)

@HiltViewModel
class ProfessionalViewModel @Inject constructor(
    stimulationRepository: StimulationRepository,
    symptomRepository: SymptomRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfessionalUiState> = combine(
        stimulationRepository.observarRealizadas(),
        symptomRepository.observarRegistros(),
    ) { estimulacoes, registros ->
        val agora = LocalDateTime.now()
        val inicioMillis = Ciclo.inicioMillis()
        val feitas = estimulacoes.count { it.dataHora >= inicioMillis }
        val previstas = Ciclo.previstasAte(agora)
        val adesao = if (previstas == 0) 0 else (feitas * 100 / previstas).coerceIn(0, 100)

        val pontos = registros.map { EvolutionPoint(it.dataHora, it.nivel) }
        val inicial = pontos.firstOrNull()?.nivel
        val atual = pontos.lastOrNull()?.nivel
        val variacao = if (inicial != null && atual != null) atual - inicial else null
        val diaCiclo = Ciclo.diaAtual(agora)

        val recentes = registros
            .takeLast(MAX_REGISTROS_RECENTES)
            .reversed()
            .map { RegistroResumo(formatarMillis(it.dataHora), it.nivel, it.nota) }

        ProfessionalUiState(
            diaCiclo = diaCiclo,
            duracaoCiclo = Ciclo.DURACAO_DIAS,
            adesaoPercentual = adesao,
            estimulacoesFeitas = feitas,
            estimulacoesPrevistas = previstas,
            pontos = pontos,
            nivelInicial = inicial,
            nivelAtual = atual,
            variacao = variacao,
            registrosRecentes = recentes,
            relatorio = montarRelatorio(
                diaCiclo = diaCiclo,
                duracaoCiclo = Ciclo.DURACAO_DIAS,
                adesao = adesao,
                feitas = feitas,
                previstas = previstas,
                inicial = inicial,
                atual = atual,
                variacao = variacao,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfessionalUiState(),
    )
}

private val FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm")

private fun formatarMillis(millis: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        .format(FORMATO_DATA_HORA)

private fun formatarVariacao(variacao: Int): String =
    if (variacao > 0) "+$variacao" else variacao.toString()

/** Resumo em texto do acompanhamento, para o botao "Compartilhar relatório". */
private fun montarRelatorio(
    diaCiclo: Int,
    duracaoCiclo: Int,
    adesao: Int,
    feitas: Int,
    previstas: Int,
    inicial: Int?,
    atual: Int?,
    variacao: Int?,
): String = buildString {
    appendLine("Relatório de acompanhamento")
    appendLine("Paciente: $NOME_FICTICIO")
    appendLine("Ciclo: dia $diaCiclo de $duracaoCiclo")
    appendLine("Adesão: $adesao% ($feitas de $previstas estimulações)")
    appendLine("Sintoma inicial: ${inicial?.toString() ?: "-"}")
    append("Sintoma atual: ${atual?.toString() ?: "-"}")
    if (variacao != null) append(" (variação ${formatarVariacao(variacao)})")
    appendLine()
    appendLine()
    append("Este resumo é de apoio e não substitui a avaliação do profissional.")
}
