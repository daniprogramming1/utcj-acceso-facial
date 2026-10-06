package edu.utcj.acceso.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.repository.AccessLogRepository
import edu.utcj.acceso.domain.model.AccessEvent
import edu.utcj.acceso.domain.model.AccessResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccessLogViewModel @Inject constructor(
    private val repo: AccessLogRepository
) : ViewModel() {
    private val fromMs = MutableStateFlow<Long?>(null)
    private val toMs = MutableStateFlow<Long?>(null)
    private val matricula = MutableStateFlow<String?>(null)
    private val result = MutableStateFlow<AccessResult?>(null)

    val events: StateFlow<List<AccessEvent>> = combine(fromMs, toMs, matricula, result) { f, t, m, r ->
        Filter(f, t, m, r)
    }.flatMapLatest { q ->
        repo.observeFiltered(q.from, q.to, q.mat, q.res)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilters(matricula: String?, result: AccessResult?) {
        this.matricula.value = matricula
        this.result.value = result
    }

    suspend fun exportCsv(): File = repo.exportCsv(events.value)
    suspend fun exportPdf(): File = repo.exportPdf(events.value)

    private data class Filter(val from: Long?, val to: Long?, val mat: String?, val res: AccessResult?)
}
