package br.edu.ifpr.appaurico.platform

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith
import br.edu.ifpr.appaurico.data.local.AuricoDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class PlatformMigrationTest {
    @Test fun preservesVersionTwoRecordsWithoutAssigningRemoteOwnership() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "migration-preservation-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name); file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("CREATE TABLE symptom_log (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dataHora INTEGER NOT NULL, nivel INTEGER NOT NULL, nota TEXT)")
            old.execSQL("CREATE TABLE reminder (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tipo TEXT NOT NULL, horario INTEGER NOT NULL, ativo INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE stimulation (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dataHora INTEGER NOT NULL)")
            old.execSQL("INSERT INTO symptom_log VALUES (42, 1700000000000, 6, 'Registro preservado')")
            old.execSQL("INSERT INTO stimulation VALUES (9, 1700000000000)")
            old.execSQL("INSERT INTO reminder VALUES (3, 'ESTIMULACAO', 1700000000000, 1)")
            old.version = 2
        }
        val db = Room.databaseBuilder(context, AuricoDatabase::class.java, name).addMigrations(PlatformMigration.FROM_2_TO_3).build()
        try {
            assertEquals(1, db.symptomLogDao().contar())
            assertEquals(1, db.stimulationDao().contar())
            db.openHelper.readableDatabase.query("SELECT id,nota FROM symptom_log").use { cursor ->
                assertTrue(cursor.moveToFirst()); assertEquals(42L, cursor.getLong(0)); assertEquals("Registro preservado", cursor.getString(1))
            }
            assertTrue(db.platformDao().allTreatments("any-account").isEmpty())
            assertTrue(db.platformDao().pending("any-account").isEmpty())
            assertEquals(3, db.openHelper.readableDatabase.version)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
