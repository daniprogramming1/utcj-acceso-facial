package edu.utcj.acceso.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.qr.QrKeyStore
import edu.utcj.acceso.domain.validation.RegistrationValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class RegStep(val label: String) {
    DATA("Datos"),
    CONSENT("Consentimiento"),
    DONE("Listo")
}

data class RegistrationUi(
    val step: RegStep = RegStep.DATA,
    val matricula: String = "",
    val nombre: String = "",
    /** Carrera elegida de la lista (o escrita en «Otra carrera…»), sin el grupo. */
    val carrera: String = "",
    val grupo: String = "",
    val correo: String = "",
    val showErrors: Boolean = false,
    val consentAccepted: Boolean = false,
    /** Estatus previo de la matrícula en este teléfono (re-registro o BAJA / SUSPENDIDO). */
    val existingStatus: StudentStatus? = null,
    val saving: Boolean = false,
    val checking: Boolean = false,
    val error: String? = null,
    val savedStatus: StudentStatus? = null
) {
    /**
     * Validación en vivo: el error aparece mientras escribe, salvo «demasiado corta», que solo se
     * muestra al intentar continuar (no se regaña a quien va a la mitad).
     */
    val matriculaError: String? get() = when {
        showErrors -> RegistrationValidator.matriculaError(matricula)
        matricula.isBlank() -> null
        else -> RegistrationValidator.matriculaError(matricula)
            .takeUnless { it == RegistrationValidator.MATRICULA_TOO_SHORT }
    }
    val nombreError: String? get() = if (showErrors) RegistrationValidator.nombreError(nombre) else null
    val grupoError: String? get() =
        if (showErrors || grupo.length >= 2) RegistrationValidator.grupoError(grupo) else null
    val correoError: String? get() = if (showErrors) RegistrationValidator.correoError(correo) else null

    /** Lo que se guarda en `Student.carrera`. */
    val carreraCompleta: String get() = RegistrationValidator.joinCarrera(carrera, grupo)
}

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val students: StudentRepository,
    private val settings: SettingsRepository,
    private val keys: QrKeyStore
) : ViewModel() {

    private val _ui = MutableStateFlow(RegistrationUi())
    val ui: StateFlow<RegistrationUi> = _ui.asStateFlow()

    init {
        // «Actualizar mis datos»: precarga lo que ya está guardado en este teléfono.
        viewModelScope.launch {
            val s = settings.getRememberedStudent()?.let { students.get(it) } ?: return@launch
            _ui.update {
                if (it.matricula.isNotEmpty()) it
                else {
                    val (carrera, grupo) = RegistrationValidator.splitCarrera(s.carrera)
                    it.copy(matricula = s.matricula, nombre = s.nombre, carrera = carrera, grupo = grupo, correo = s.correo.orEmpty())
                }
            }
        }
    }

    fun onMatricula(v: String) = _ui.update { it.copy(matricula = v.take(RegistrationValidator.MATRICULA_MAX + 5)) }
    fun onNombre(v: String) = _ui.update { it.copy(nombre = v.take(80)) }
    fun onCarrera(v: String) = _ui.update { it.copy(carrera = v.take(60)) }
    fun onGrupo(v: String) = _ui.update { it.copy(grupo = v.take(4)) }
    fun onCorreo(v: String) = _ui.update { it.copy(correo = v.take(80).trim()) }
    fun onConsent(v: Boolean) = _ui.update { it.copy(consentAccepted = v) }

    /** Avanza al siguiente paso si el actual es válido. */
    fun next() {
        val s = _ui.value
        when (s.step) {
            RegStep.DATA -> {
                if (!RegistrationValidator.isValid(s.matricula, s.nombre) ||
                    RegistrationValidator.correoError(s.correo) != null ||
                    RegistrationValidator.grupoError(s.grupo) != null
                ) {
                    _ui.update { it.copy(showErrors = true) }
                    return
                }
                val mat = RegistrationValidator.normalizeMatricula(s.matricula)
                _ui.update { it.copy(checking = true, matricula = mat) }
                viewModelScope.launch {
                    val existing = students.get(mat)
                    _ui.update { it.copy(checking = false, existingStatus = existing?.status, step = RegStep.CONSENT) }
                }
            }
            RegStep.CONSENT -> if (s.consentAccepted) save()
            RegStep.DONE -> Unit
        }
    }

    /** @return false si ya está en el primer paso (la pantalla debe cerrar). */
    fun back(): Boolean = when (_ui.value.step) {
        RegStep.DATA, RegStep.DONE -> false
        RegStep.CONSENT -> { _ui.update { it.copy(step = RegStep.DATA) }; true }
    }

    private fun save() {
        val s = _ui.value
        if (s.saving) return
        _ui.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                // Re-registro: llave nueva (el guardia debe aprobar el nuevo QR de registro).
                val publicKey = withContext(Dispatchers.Default) {
                    keys.delete(s.matricula)
                    keys.getOrCreate(s.matricula).publicKey
                }
                val saved = students.registerWithConsent(
                    s.matricula, s.nombre.trim(), s.carreraCompleta, s.correo.ifBlank { null }, publicKey
                )
                settings.setStudentMarkedApproved(false)
                settings.setRememberedStudent(s.matricula)
                _ui.update { it.copy(saving = false, step = RegStep.DONE, savedStatus = saved.status) }
            } catch (e: Exception) {
                _ui.update { it.copy(saving = false, error = "No se pudo crear tu llave segura. Intenta de nuevo.") }
            }
        }
    }
}
