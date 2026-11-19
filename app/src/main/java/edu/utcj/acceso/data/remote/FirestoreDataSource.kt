package edu.utcj.acceso.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import edu.utcj.acceso.BuildConfig
import edu.utcj.acceso.data.local.AccessEventEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Capa de sincronización con Firestore.
 *
 * Se activa sola cuando existe `app/google-services.json` (ver `BuildConfig.FIREBASE_ENABLED`).
 * Sin ese archivo la app es 100 % offline: el worker trata los eventos como "sincronizados
 * localmente" para vaciar la cola.
 *
 * Cada evento se escribe en `access_events/{syncKey}`: usar `syncKey` como ID hace la escritura
 * idempotente (reintentos de WorkManager nunca crean duplicados).
 */
@Singleton
class FirestoreDataSource @Inject constructor() {

    companion object {
        private const val TAG = "FirestoreDataSource"
        const val COLLECTION_ACCESS_EVENTS = "access_events"
    }

    val isEnabled: Boolean get() = BuildConfig.FIREBASE_ENABLED

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun uploadAccessEvent(event: AccessEventEntity): Boolean {
        if (!isEnabled) {
            Log.d(TAG, "Firestore desactivado — se omite syncKey=${event.syncKey}")
            return true
        }
        if (event.syncKey.isBlank()) {
            Log.w(TAG, "Evento ${event.id} sin syncKey; no se sube")
            return false
        }
        return try {
            db.collection(COLLECTION_ACCESS_EVENTS)
                .document(event.syncKey)
                .set(event.toFirestoreMap(), SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al subir syncKey=${event.syncKey}", e)
            false
        }
    }

    suspend fun ping(): Boolean = isEnabled

    private fun AccessEventEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "syncKey" to syncKey,
        "datetimeMs" to datetimeMs,
        "matricula" to matricula,
        "nombre" to nombre,
        "result" to result.name,
        "method" to method.name,
        "authorizingGuard" to authorizingGuard,
        "reason" to reason,
        "similarity" to similarity,
        "verifyDurationMs" to verifyDurationMs
    )
}
