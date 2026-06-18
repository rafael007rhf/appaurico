package br.edu.ifpr.appaurico.ui.screens.evolution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpr.appaurico.data.repository.SymptomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Um ponto do grafico: o momento do registro e o nivel informado. */
data class EvolutionPoint(
    val dataHora: Long,
    val nivel: Int,
)

data class EvolutionUiState(
    val pontos: List<EvolutionPoint> = emptyList(),
    val nivelInicial: Int? = null,
    val nivelAtual: Int? = null,
    val variacao: Int? = null,
)

@HiltViewModel
class EvolutionViewModel @Inject constructor(
    repository: SymptomRepository,
) : ViewModel() {

    val uiState: StateFlow<EvolutionUiState> =
        repository.observarRegistros()
            .map { registros ->
                val pontos = registros.map { EvolutionPoint(it.dataHora, it.nivel) }
                val inicial = pontos.firstOrNull()?.nivel
                val atual = pontos.lastOrNull()?.nivel
                EvolutionUiState(
                    pontos = pontos,
                    nivelInicial = inicial,
                    nivelAtual = atual,
                    variacao = if (inicial != null && atual != null) atual - inicial else null,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = EvolutionUiState(),
            )
}
