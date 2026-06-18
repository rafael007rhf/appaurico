package br.edu.ifpr.appaurico.di

import android.content.Context
import androidx.room.Room
import br.edu.ifpr.appaurico.data.local.SementeDatabase
import br.edu.ifpr.appaurico.data.local.dao.ReminderDao
import br.edu.ifpr.appaurico.data.local.dao.SymptomLogDao
import br.edu.ifpr.appaurico.data.repository.ReminderRepository
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
    fun provideDatabase(@ApplicationContext context: Context): SementeDatabase =
        Room.databaseBuilder(
            context,
            SementeDatabase::class.java,
            "semente.db",
        ).build()

    @Provides
    fun provideSymptomLogDao(database: SementeDatabase): SymptomLogDao =
        database.symptomLogDao()

    @Provides
    fun provideReminderDao(database: SementeDatabase): ReminderDao =
        database.reminderDao()

    @Provides
    @Singleton
    fun provideSymptomRepository(dao: SymptomLogDao): SymptomRepository =
        SymptomRepository(dao)

    @Provides
    @Singleton
    fun provideReminderRepository(dao: ReminderDao): ReminderRepository =
        ReminderRepository(dao)
}
