package br.edu.ifpr.appaurico.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.edu.ifpr.appaurico.core.notification.ReminderScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PatientWorkerDependencies {
    fun repository(): PatientRepository
    fun scheduler(): ReminderScheduler
}
class PatientSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, PatientWorkerDependencies::class.java)
        val repository = deps.repository()
        return repository.session.mutex.withLock {
            try {
                repository.synchronize(repository.session.account.first())
                deps.scheduler().reagendarTodos(emptyList())
                Result.success()
            } catch (e: CancellationException) { throw e }
            catch (e: ApiFailure) { if (e.status in listOf(401, 403)) Result.failure() else Result.retry() }
            catch (_: Exception) { Result.retry() }
        }
    }
}
