package edu.utcj.acceso.data.remote

import android.util.Log
import edu.utcj.acceso.data.local.AccessEventEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional Firestore sync layer.
 *
 * Firebase dependencies are commented out in Gradle by default.
 * When enabled, replace the stub body with FirebaseFirestore uploads.
 * Until then, sync is a no-op that reports success so WorkManager can clear the queue
 * only when [isEnabled] is false; when a future build enables Firebase, wire real writes.
 */
@Singleton
class FirestoreDataSource @Inject constructor() {

    companion object {
        private const val TAG = "FirestoreDataSource"
        /** Set true only when google-services + Firestore deps are active. */
        const val FIRESTORE_COMPILED_IN = false
    }

    val isEnabled: Boolean get() = FIRESTORE_COMPILED_IN

    suspend fun uploadAccessEvent(event: AccessEventEntity): Boolean {
        if (!isEnabled) {
            Log.d(TAG, "Firestore disabled — skip upload for syncKey=${event.syncKey}")
            // Offline-first: treat as "synced locally only" when Firebase is off
            return true
        }
        // Placeholder for:
        // FirebaseFirestore.getInstance().collection("access_events")
        //   .document(event.syncKey).set(mapOf(...)).await()
        return false
    }

    suspend fun ping(): Boolean = isEnabled
}
