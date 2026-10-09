package edu.utcj.acceso.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import edu.utcj.acceso.data.local.AppDatabase
import edu.utcj.acceso.data.local.UsedNonceEntity
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.StudentStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migración 1 → 2 (v1.1.x → v1.2.0) sobre una base creada con el esquema exacto de la versión 1:
 * conserva alumnos y bitácora, agrega correo/llave pública y la tabla de nonces y elimina las
 * plantillas faciales. Room valida el esquema resultante al abrir.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-test.db"

    @Before fun clean() { ctx.deleteDatabase(name) }
    @After fun cleanUp() { ctx.deleteDatabase(name) }

    private fun createV1() {
        val cfg = SupportSQLiteOpenHelper.Configuration.builder(ctx).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) = V1_SQL.forEach(db::execSQL)
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(cfg)
        helper.writableDatabase.apply {
            execSQL("INSERT INTO students VALUES ('20230001','Ana López','TI','APPROVED','1.0.0',1000,1000,2000,'Guardia')")
            execSQL("INSERT INTO face_embeddings (matricula, encryptedEmbedding, iv, sampleIndex, createdAtMs) VALUES ('20230001', x'00', x'00', 0, 1000)")
            execSQL("INSERT INTO access_events (datetimeMs, matricula, nombre, result, method, authorizingGuard, reason, similarity, verifyDurationMs, synced, syncKey) VALUES (3000,'20230001','Ana López','ALLOWED','FACE',NULL,NULL,0.8,900,0,'k1')")
        }
        helper.close()
    }

    @Test
    fun migrate1To2_keepsData_dropsFaceTable() = runBlocking {
        createV1()
        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS).allowMainThreadQueries().build()
        try {
            val s = db.studentDao().getByMatricula("20230001")!!
            assertEquals("Ana López", s.nombre)
            assertEquals(StudentStatus.APPROVED, s.status)
            assertNull(s.correo)
            assertNull(s.publicKey)
            val ev = db.accessEventDao().getSince(0).single()
            assertEquals(AccessMethod.FACE, ev.method) // la historia se conserva
            assertEquals(0, db.query("SELECT COUNT(*) FROM sqlite_master WHERE name='face_embeddings'", null).use { it.moveToFirst(); it.getInt(0) })
            assertEquals(1L, db.usedNonceDao().tryInsert(UsedNonceEntity("n1", "20230001", 10_000)))
            assertEquals(-1L, db.usedNonceDao().tryInsert(UsedNonceEntity("n1", "20230001", 10_000)))
            assertEquals(2, db.openHelper.readableDatabase.version)
        } finally {
            db.close()
        }
    }

    private companion object {
        val V1_SQL = listOf(
            "CREATE TABLE IF NOT EXISTS `students` (`matricula` TEXT NOT NULL, `nombre` TEXT NOT NULL, `carrera` TEXT NOT NULL, `status` TEXT NOT NULL, `consentVersion` TEXT NOT NULL, `consentTimestampMs` INTEGER NOT NULL, `createdAtMs` INTEGER NOT NULL, `approvedAtMs` INTEGER, `approvedByGuard` TEXT, PRIMARY KEY(`matricula`))",
            "CREATE TABLE IF NOT EXISTS `face_embeddings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `matricula` TEXT NOT NULL, `encryptedEmbedding` BLOB NOT NULL, `iv` BLOB NOT NULL, `sampleIndex` INTEGER NOT NULL, `createdAtMs` INTEGER NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_face_embeddings_matricula` ON `face_embeddings` (`matricula`)",
            "CREATE TABLE IF NOT EXISTS `access_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `datetimeMs` INTEGER NOT NULL, `matricula` TEXT NOT NULL, `nombre` TEXT NOT NULL, `result` TEXT NOT NULL, `method` TEXT NOT NULL, `authorizingGuard` TEXT, `reason` TEXT, `similarity` REAL, `verifyDurationMs` INTEGER, `synced` INTEGER NOT NULL, `syncKey` TEXT NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_access_events_datetimeMs` ON `access_events` (`datetimeMs`)",
            "CREATE INDEX IF NOT EXISTS `index_access_events_matricula` ON `access_events` (`matricula`)",
            "CREATE INDEX IF NOT EXISTS `index_access_events_synced` ON `access_events` (`synced`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_access_events_syncKey` ON `access_events` (`syncKey`)",
            "CREATE TABLE IF NOT EXISTS `visitors` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `motivo` TEXT NOT NULL, `visitaA` TEXT NOT NULL, `entradaMs` INTEGER NOT NULL, `salidaMs` INTEGER, `registeredByGuard` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `incidents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `datetimeMs` INTEGER NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `photoUri` TEXT, `reportedByGuard` TEXT NOT NULL, `relatedMatricula` TEXT)",
            "CREATE TABLE IF NOT EXISTS `sync_queue` (`syncKey` TEXT NOT NULL, `entityType` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `createdAtMs` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, PRIMARY KEY(`syncKey`))",
            "CREATE TABLE IF NOT EXISTS `eval_results` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scenario` TEXT NOT NULL, `passed` INTEGER NOT NULL, `notes` TEXT NOT NULL, `similarity` REAL, `durationMs` INTEGER, `recordedAtMs` INTEGER NOT NULL, `recordedBy` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '5a7f5d57a7cf4104797f56e8cd71051b')"
        )
    }
}
