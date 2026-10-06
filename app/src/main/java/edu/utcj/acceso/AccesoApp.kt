package edu.utcj.acceso

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import edu.utcj.acceso.data.repository.StudentStatusRepository
import edu.utcj.acceso.data.security.SecurePrefs
import edu.utcj.acceso.data.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AccesoApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var statusRepository: StudentStatusRepository
    @Inject lateinit var securePrefs: SecurePrefs

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Primera ejecución: cargar estatus institucional de muestra (assets/students_status.csv)
        appScope.launch {
            if (!securePrefs.getBoolean(KEY_CSV_SEEDED, false)) {
                try {
                    statusRepository.refreshFromInstitutionalSource()
                    securePrefs.putBoolean(KEY_CSV_SEEDED, true)
                } catch (e: Exception) {
                    Log.w("AccesoApp", "No se pudo cargar CSV de estatus", e)
                }
            }
        }
        SyncWorker.schedulePeriodic(this)
        SyncWorker.enqueue(this)
    }

    private companion object {
        const val KEY_CSV_SEEDED = "students_csv_seeded"
    }
}
