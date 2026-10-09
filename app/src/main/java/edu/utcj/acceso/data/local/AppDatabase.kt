package edu.utcj.acceso.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StudentEntity::class,
        UsedNonceEntity::class,
        AccessEventEntity::class,
        VisitorEntity::class,
        IncidentEntity::class,
        SyncQueueEntity::class,
        EvalResultEntity::class
    ],
    version = AppDatabase.VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun usedNonceDao(): UsedNonceDao
    abstract fun accessEventDao(): AccessEventDao
    abstract fun visitorDao(): VisitorDao
    abstract fun incidentDao(): IncidentDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun evalResultDao(): EvalResultDao

    companion object {
        const val VERSION = 2
        const val NAME = "acceso_utcj.db"

        /**
         * v1 → v2 (app 1.2.0): el acceso pasa de reconocimiento facial a QR firmado.
         * - students: + correo, + publicKey (los alumnos, la bitácora y todo lo demás se conservan).
         * - used_nonces: nueva tabla anti-repetición.
         * - face_embeddings: se elimina (ya no se usa ningún dato biométrico).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `students` ADD COLUMN `correo` TEXT")
                db.execSQL("ALTER TABLE `students` ADD COLUMN `publicKey` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `used_nonces` (`nonce` TEXT NOT NULL, `matricula` TEXT NOT NULL, " +
                        "`expiresAtMs` INTEGER NOT NULL, PRIMARY KEY(`nonce`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_used_nonces_expiresAtMs` ON `used_nonces` (`expiresAtMs`)")
                db.execSQL("DROP INDEX IF EXISTS `index_face_embeddings_matricula`")
                db.execSQL("DROP TABLE IF EXISTS `face_embeddings`")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
    }
}
