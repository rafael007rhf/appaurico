package br.edu.ifpr.appaurico.di

import android.content.Context
import androidx.room.Room
import br.edu.ifpr.appaurico.data.local.AuricoDatabase
import br.edu.ifpr.appaurico.data.local.dao.ReminderDao
import br.edu.ifpr.appaurico.data.local.dao.StimulationDao
import br.edu.ifpr.appaurico.data.local.dao.SymptomLogDao
import br.edu.ifpr.appaurico.data.repository.ReminderRepository
import br.edu.ifpr.appaurico.data.repository.StimulationRepository
import br.edu.ifpr.appaurico.data.repository.SymptomRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AuricoDatabase =
        Room.databaseBuilder(
            context,
            AuricoDatabase::class.java,
            "semente.db",
        )
            // App de demonstracao, sem dados a preservar: recria o banco em mudancas de schema.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideSymptomLogDao(database: AuricoDatabase): SymptomLogDao =
        database.symptomLogDao()

    @Provides
    fun provideReminderDao(database: AuricoDatabase): ReminderDao =
        database.reminderDao()

    @Provides
    fun provideStimulationDao(database: AuricoDatabase): StimulationDao =
        database.stimulationDao()

    @Provides
    @Singleton
    fun provideSymptomRepository(dao: SymptomLogDao): SymptomRepository =
        SymptomRepository(dao)

    @Provides
    @Singleton
    fun provideReminderRepository(dao: ReminderDao): ReminderRepository =
        ReminderRepository(dao)

    @Provides
    @Singleton
    fun provideStimulationRepository(dao: StimulationDao): StimulationRepository =
        StimulationRepository(dao)
}
