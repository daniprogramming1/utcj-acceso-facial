package edu.utcj.acceso.flow

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import edu.utcj.acceso.data.local.AccessEventDao
import edu.utcj.acceso.data.local.AppDatabase
import edu.utcj.acceso.data.local.EvalResultDao
import edu.utcj.acceso.data.local.IncidentDao
import edu.utcj.acceso.data.local.StudentDao
import edu.utcj.acceso.data.local.SyncQueueDao
import edu.utcj.acceso.data.local.UsedNonceDao
import edu.utcj.acceso.data.local.VisitorDao
import edu.utcj.acceso.data.security.KeyValueStore
import edu.utcj.acceso.di.DatabaseModule
import edu.utcj.acceso.di.QrModule
import edu.utcj.acceso.domain.qr.InMemoryQrKeyStore
import edu.utcj.acceso.domain.qr.QrKeyStore
import edu.utcj.acceso.di.SecurityModule
import edu.utcj.acceso.testutil.MemoryKeyValueStore
import javax.inject.Singleton

/** Preferencias en memoria: Robolectric no tiene Android Keystore. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SecurityModule::class])
object TestSecurityModule {
    @Provides @Singleton
    fun store(): KeyValueStore = MemoryKeyValueStore()
}

/**
 * Room en memoria. Sin `allowMainThreadQueries()` a propósito: igual que en un teléfono,
 * cualquier consulta en el hilo principal hace fallar la prueba.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides @Singleton
    fun db(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Provides fun studentDao(db: AppDatabase): StudentDao = db.studentDao()
    @Provides fun nonceDao(db: AppDatabase): UsedNonceDao = db.usedNonceDao()
    @Provides fun accessDao(db: AppDatabase): AccessEventDao = db.accessEventDao()
    @Provides fun visitorDao(db: AppDatabase): VisitorDao = db.visitorDao()
    @Provides fun incidentDao(db: AppDatabase): IncidentDao = db.incidentDao()
    @Provides fun syncDao(db: AppDatabase): SyncQueueDao = db.syncQueueDao()
    @Provides fun evalDao(db: AppDatabase): EvalResultDao = db.evalResultDao()
}

/** Llaves EC en software: Robolectric no tiene Android Keystore. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [QrModule::class])
object TestQrModule {
    @Provides @Singleton
    fun keys(): QrKeyStore = InMemoryQrKeyStore()
}
