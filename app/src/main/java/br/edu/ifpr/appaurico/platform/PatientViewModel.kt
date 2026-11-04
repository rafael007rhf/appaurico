package br.edu.ifpr.appaurico.platform

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifpr.appaurico.core.notification.ReminderScheduler
import br.edu.ifpr.appaurico.data.repository.SymptomRepository
import br.edu.ifpr.appaurico.domain.model.SymptomLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PatientViewModel @Inject constructor(val repository: PatientRepository, private val scheduler: ReminderScheduler, private val legacy: SymptomRepository) : ViewModel() {
    val account = repository.session.account.stateIn(viewModelScope, SharingStarted.Eagerly, "local")
    val onboarding = repository.session.onboarding.map<Boolean, Boolean?> { it }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val reminders = repository.session.reminders.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val treatments = account.flatMapLatest(repository.dao::treatments).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val occurrences = account.flatMapLatest(repository.dao::occurrences).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val events = account.flatMapLatest(repository.dao::events).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val failures = account.flatMapLatest(repository.dao::failures).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val legacyRecords = legacy.observarRegistros().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val message = MutableStateFlow("")
    val busy = MutableStateFlow(false)
    init { repository.schedulePeriodicSync(); repository.scheduleSync() }
    private fun run(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true; message.value = ""
        viewModelScope.launch {
            try { action() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message.value = e.message ?: "Não foi possível concluir. Os dados locais foram preservados." }
            finally { busy.value = false }
        }
    }
    fun onboardingDone() = run { repository.session.completeOnboarding() }
    suspend fun loginIntent(): Intent = repository.session.loginIntent()
    fun finishLogin(intent: Intent) = run {
        repository.session.mutex.withLock { scheduler.cancelarTodos(); repository.session.finishLogin(intent) }
        repository.scheduleSync(); message.value = "Conta conectada. Aguardando sincronização."
    }
    fun logout() = run {
        repository.session.mutex.withLock { scheduler.cancelarTodos(); repository.session.logout() }
        message.value = "Sessão encerrada. Os registros de cada conta permanecem separados neste dispositivo."
    }
    fun sync() = run {
        repository.session.mutex.withLock { repository.synchronize(account.value); scheduler.reagendarTodos(emptyList()) }
        message.value = "Sincronização concluída."
    }
    fun setReminders(enabled: Boolean) = run { repository.session.setReminders(enabled); scheduler.reagendarTodos(emptyList()) }
    fun record(treatmentId: String, symptomId: String, level: Int, note: String, correction: String? = null) = run {
        repository.recordSymptom(account.value, treatmentId, symptomId, level, note, correction)
        message.value = "Salvo neste dispositivo. Aguardando sincronização."
    }
    fun stimulate(occurrence: Occurrence) = run { repository.recordStimulation(account.value, occurrence); message.value = "Estimulação salva neste dispositivo." }
    fun localRecord(level: Int, note: String) = run {
        require(level in 0..10 && note.length <= 2000)
        legacy.registrar(SymptomLog(dataHora = System.currentTimeMillis(), nivel = level, nota = note.trim().ifBlank { null }))
        message.value = "Salvo somente neste dispositivo, sem associação a tratamento ou envio automático."
    }
    fun accept(token: String, consent: Boolean) = run {
        require(consent)
        repository.session.mutex.withLock {
            repository.session.requestFor(account.value, "/patient-links/accept", "POST", JSONObject().put("token", token.trim()).put("consent", true).put("policyVersion", "2026-11-03"))
        }
        repository.scheduleSync(); message.value = "Convite aceito. O tratamento aparecerá depois da publicação pelo profissional."
    }
}
