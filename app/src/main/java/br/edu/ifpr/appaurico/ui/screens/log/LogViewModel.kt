package br.edu.ifpr.appaurico.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpr.appaurico.data.repository.SymptomRepository
import br.edu.ifpr.appaurico.domain.model.SymptomLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogUiState(
    val nivelSelecionado: Int? = null,
    val nota: String = "",
    val salvando: Boolean = false,
    val salvo: Boolean = false,
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val repository: SymptomRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    fun onNivelSelecionado(nivel: Int) {
        _uiState.update { it.copy(nivelSelecionado = nivel, salvo = false) }
    }

    fun onNotaChange(nota: String) {
        _uiState.update { it.copy(nota = nota, salvo = false) }
    }

    fun salvar() {
        val estado = _uiState.value
        val nivel = estado.nivelSelecionado
        if (nivel == null || estado.salvando) return

        _uiState.update { it.copy(salvando = true) }
        viewModelScope.launch {
            repository.registrar(
                SymptomLog(
                    dataHora = System.currentTimeMillis(),
                    nivel = nivel,
                    nota = estado.nota.trim().ifBlank { null },
                ),
            )
            // Limpa o formulario e sinaliza a confirmacao.
            _uiState.value = LogUiState(salvo = true)
        }
    }
}
