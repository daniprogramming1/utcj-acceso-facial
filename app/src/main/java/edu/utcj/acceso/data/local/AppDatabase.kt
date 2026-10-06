package edu.utcj.acceso.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        StudentEntity::class,
        FaceEmbeddingEntity::class,
        AccessEventEntity::class,
        VisitorEntity::class,
        IncidentEntity::class,
        SyncQueueEntity::class,
        EvalResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun faceEmbeddingDao(): FaceEmbeddingDao
    abstract fun accessEventDao(): AccessEventDao
    abstract fun visitorDao(): VisitorDao
    abstract fun incidentDao(): IncidentDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun evalResultDao(): EvalResultDao
}
