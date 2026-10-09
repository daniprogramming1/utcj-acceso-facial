package edu.utcj.acceso.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import edu.utcj.acceso.data.local.AccessEventDao
import edu.utcj.acceso.data.local.AppDatabase
import edu.utcj.acceso.data.local.EvalResultDao
import edu.utcj.acceso.data.local.IncidentDao
import edu.utcj.acceso.data.local.StudentDao
import edu.utcj.acceso.data.local.SyncQueueDao
import edu.utcj.acceso.data.local.UsedNonceDao
import edu.utcj.acceso.data.local.VisitorDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // Migraciones reales: actualizar la app conserva alumnos, bitácora e incidencias.
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            // Solo si alguien instala una versión ANTERIOR encima de una nueva se recrea la base.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun studentDao(db: AppDatabase): StudentDao = db.studentDao()
    @Provides fun nonceDao(db: AppDatabase): UsedNonceDao = db.usedNonceDao()
    @Provides fun accessDao(db: AppDatabase): AccessEventDao = db.accessEventDao()
    @Provides fun visitorDao(db: AppDatabase): VisitorDao = db.visitorDao()
    @Provides fun incidentDao(db: AppDatabase): IncidentDao = db.incidentDao()
    @Provides fun syncDao(db: AppDatabase): SyncQueueDao = db.syncQueueDao()
    @Provides fun evalDao(db: AppDatabase): EvalResultDao = db.evalResultDao()
}
