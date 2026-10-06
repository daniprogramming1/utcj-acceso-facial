package edu.utcj.acceso.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.utcj.acceso.data.sync.SyncQueue
import edu.utcj.acceso.data.sync.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatusLabel { ONLINE, OFFLINE, SYNCING, PENDING }

data class SyncUiState(
    val label: SyncStatusLabel,
    val pendingCount: Int,
    val displayEs: String
)

@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncQueue: SyncQueue
) {
    private val syncing = MutableStateFlow(false)

    fun observeStatus(): Flow<SyncUiState> = combine(
        syncQueue.observePendingCount(),
        syncing
    ) { pending, isSyncing ->
        val online = isOnline()
        val label = when {
            isSyncing -> SyncStatusLabel.SYNCING
            !online -> SyncStatusLabel.OFFLINE
            pending > 0 -> SyncStatusLabel.PENDING
            else -> SyncStatusLabel.ONLINE
        }
        val text = when (label) {
            SyncStatusLabel.ONLINE -> "En línea"
            SyncStatusLabel.OFFLINE -> "Sin conexión"
            SyncStatusLabel.SYNCING -> "Sincronizando"
            SyncStatusLabel.PENDING -> "Pendientes: $pending"
        }
        SyncUiState(label, pending, text)
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun triggerSync() {
        syncing.value = true
        SyncWorker.enqueue(context)
        syncing.value = false
    }
}
