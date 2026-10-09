package edu.utcj.acceso.data.repository

import edu.utcj.acceso.data.local.StudentDao
import edu.utcj.acceso.data.local.StudentEntity
import edu.utcj.acceso.data.remote.StudentStatusCsvDataSource
import edu.utcj.acceso.domain.model.ConsentRecord
import edu.utcj.acceso.domain.model.Student
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.qr.RegistrationPayload
import edu.utcj.acceso.domain.qr.StudentRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepository @Inject constructor(
    private val studentDao: StudentDao,
    private val csvStatus: StudentStatusCsvDataSource
) {
    fun observeAll(): Flow<List<Student>> = studentDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observePending(): Flow<List<Student>> =
        studentDao.observeByStatus(StudentStatus.PENDING).map { list -> list.map { it.toDomain() } }

    suspend fun get(matricula: String): Student? = studentDao.getByMatricula(matricula)?.toDomain()

    fun observe(matricula: String): Flow<Student?> = studentDao.observeByMatricula(matricula).map { it?.toDomain() }

    suspend fun search(q: String): List<Student> = studentDao.search(q).map { it.toDomain() }

    /** Datos que usa el guardia para decidir un QR (incluye la llave pública). */
    suspend fun record(matricula: String): StudentRecord? = studentDao.getByMatricula(matricula)?.let {
        StudentRecord(it.matricula, it.nombre, it.carrera, it.status, it.publicKey?.let(::decodeKey))
    }

    /**
     * Registro en el teléfono del alumno: guarda sus datos y su llave pública como PENDIENTE.
     * Solo datos personales; ningún dato biométrico. Un estatus BAJA / SUSPENDIDO no se «limpia».
     */
    suspend fun registerWithConsent(
        matricula: String,
        nombre: String,
        carrera: String,
        correo: String?,
        publicKey: ByteArray
    ): Student {
        val now = System.currentTimeMillis()
        val previous = studentDao.getByMatricula(matricula)
        val status = when (previous?.status) {
            StudentStatus.BAJA, StudentStatus.SUSPENDIDO -> previous.status
            else -> StudentStatus.PENDING
        }
        val entity = StudentEntity(
            matricula = matricula,
            nombre = nombre,
            carrera = carrera,
            correo = correo,
            status = status,
            consentVersion = ConsentRecord.CURRENT_VERSION,
            consentTimestampMs = now,
            createdAtMs = now,
            publicKey = encodeKey(publicKey)
        )
        studentDao.upsert(entity)
        return entity.toDomain()
    }

    /**
     * Aprobación en el teléfono del guardia a partir del QR de registro del alumno.
     * Conserva fecha de alta previa (p. ej. del CSV institucional) y reemplaza la llave.
     */
    suspend fun approveFromRegistration(p: RegistrationPayload, guardName: String, approve: Boolean): Student {
        val now = System.currentTimeMillis()
        val previous = studentDao.getByMatricula(p.matricula)
        val entity = StudentEntity(
            matricula = p.matricula,
            nombre = p.nombre,
            carrera = p.carrera.ifBlank { previous?.carrera.orEmpty() },
            correo = p.correo ?: previous?.correo,
            status = if (approve) StudentStatus.APPROVED else StudentStatus.REJECTED,
            consentVersion = p.consentVersion,
            consentTimestampMs = p.issuedAtMs,
            createdAtMs = previous?.createdAtMs ?: now,
            approvedAtMs = now,
            approvedByGuard = guardName,
            publicKey = encodeKey(p.publicKey)
        )
        studentDao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun approve(matricula: String, guardName: String) {
        val s = studentDao.getByMatricula(matricula) ?: return
        studentDao.update(
            s.copy(
                status = StudentStatus.APPROVED,
                approvedAtMs = System.currentTimeMillis(),
                approvedByGuard = guardName
            )
        )
    }

    suspend fun reject(matricula: String, guardName: String) {
        val s = studentDao.getByMatricula(matricula) ?: return
        studentDao.update(
            s.copy(
                status = StudentStatus.REJECTED,
                approvedAtMs = System.currentTimeMillis(),
                approvedByGuard = guardName
            )
        )
    }

    suspend fun deleteStudentData(matricula: String) {
        studentDao.delete(matricula)
    }

    suspend fun importStatusFromAssets() {
        csvStatus.loadFromAssets().forEach { row ->
            val existing = studentDao.getByMatricula(row.matricula)
            if (existing != null) {
                // Institutional status overrides: BAJA / SUSPENDIDO block access
                val newStatus = when (row.status) {
                    StudentStatus.BAJA -> StudentStatus.BAJA
                    StudentStatus.SUSPENDIDO -> StudentStatus.SUSPENDIDO
                    StudentStatus.ACTIVO -> if (existing.status == StudentStatus.APPROVED) StudentStatus.APPROVED else existing.status
                    else -> existing.status
                }
                studentDao.update(existing.copy(status = newStatus, nombre = row.nombre.ifBlank { existing.nombre }, carrera = row.carrera.ifBlank { existing.carrera }))
            } else {
                // Alta desde el directorio institucional (sin llave: debe presentar su QR de registro)
                studentDao.upsert(
                    StudentEntity(
                        matricula = row.matricula,
                        nombre = row.nombre,
                        carrera = row.carrera,
                        status = row.status
                    )
                )
            }
        }
    }

    suspend fun importStatusFromStream(input: InputStream) {
        csvStatus.parse(input).forEach { row ->
            val existing = studentDao.getByMatricula(row.matricula)
            if (existing != null) {
                val newStatus = when (row.status) {
                    StudentStatus.BAJA -> StudentStatus.BAJA
                    StudentStatus.SUSPENDIDO -> StudentStatus.SUSPENDIDO
                    else -> existing.status
                }
                studentDao.update(existing.copy(status = newStatus, nombre = row.nombre.ifBlank { existing.nombre }))
            }
        }
    }

    private fun StudentEntity.toDomain() = Student(
        matricula = matricula,
        nombre = nombre,
        carrera = carrera,
        status = status,
        consentVersion = consentVersion,
        consentTimestampMs = consentTimestampMs,
        createdAtMs = createdAtMs,
        approvedAtMs = approvedAtMs,
        approvedByGuard = approvedByGuard,
        correo = correo,
        hasQrKey = !publicKey.isNullOrBlank()
    )

    private companion object {
        fun encodeKey(k: ByteArray): String = Base64.getEncoder().encodeToString(k)
        fun decodeKey(s: String): ByteArray? = runCatching { Base64.getDecoder().decode(s) }.getOrNull()
    }
}

/**
 * Abstract student-status facade for future institutional API.
 * Current implementation reads CSV via [StudentRepository.importStatusFromAssets].
 */
interface StudentStatusRepository {
    suspend fun refreshFromInstitutionalSource()
    suspend fun statusOf(matricula: String): StudentStatus?
}

@Singleton
class CsvBackedStudentStatusRepository @Inject constructor(
    private val students: StudentRepository
) : StudentStatusRepository {
    override suspend fun refreshFromInstitutionalSource() {
        students.importStatusFromAssets()
    }

    override suspend fun statusOf(matricula: String): StudentStatus? =
        students.get(matricula)?.status
}
