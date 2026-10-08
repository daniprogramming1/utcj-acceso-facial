package edu.utcj.acceso.data.repository

import edu.utcj.acceso.data.biometric.EmbeddingCrypto
import edu.utcj.acceso.data.local.FaceEmbeddingDao
import edu.utcj.acceso.data.local.FaceEmbeddingEntity
import edu.utcj.acceso.data.local.StudentDao
import edu.utcj.acceso.data.local.StudentEntity
import edu.utcj.acceso.data.remote.StudentStatusCsvDataSource
import edu.utcj.acceso.domain.model.ConsentRecord
import edu.utcj.acceso.domain.model.Student
import edu.utcj.acceso.domain.model.StudentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentRepository @Inject constructor(
    private val studentDao: StudentDao,
    private val embeddingDao: FaceEmbeddingDao,
    private val crypto: EmbeddingCrypto,
    private val csvStatus: StudentStatusCsvDataSource
) {
    fun observeAll(): Flow<List<Student>> = studentDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observePending(): Flow<List<Student>> =
        studentDao.observeByStatus(StudentStatus.PENDING).map { list -> list.map { it.toDomain() } }

    suspend fun get(matricula: String): Student? = studentDao.getByMatricula(matricula)?.toDomain()

    fun observe(matricula: String): Flow<Student?> = studentDao.observeByMatricula(matricula).map { it?.toDomain() }

    /** Matrículas con muestras faciales registradas (para mostrar «Rostro registrado» en el panel). */
    fun observeEnrolled(): Flow<Set<String>> = embeddingDao.observeEnrolledMatriculas().map { it.toSet() }

    suspend fun search(q: String): List<Student> = studentDao.search(q).map { it.toDomain() }

    suspend fun registerWithConsent(
        matricula: String,
        nombre: String,
        carrera: String,
        embeddings: List<FloatArray>
    ): Student {
        val now = System.currentTimeMillis()
        // Un estatus institucional BAJA / SUSPENDIDO no se «limpia» al volver a registrarse.
        val previous = studentDao.getByMatricula(matricula)?.status
        val status = if (previous == StudentStatus.BAJA || previous == StudentStatus.SUSPENDIDO) previous
        else StudentStatus.PENDING
        val entity = StudentEntity(
            matricula = matricula,
            nombre = nombre,
            carrera = carrera,
            status = status,
            consentVersion = ConsentRecord.CURRENT_VERSION,
            consentTimestampMs = now,
            createdAtMs = now
        )
        studentDao.upsert(entity)
        embeddingDao.deleteForStudent(matricula)
        embeddings.forEachIndexed { idx, emb ->
            val bytes = crypto.floatArrayToBytes(emb)
            val sealed = crypto.encrypt(bytes)
            embeddingDao.insert(
                FaceEmbeddingEntity(
                    matricula = matricula,
                    encryptedEmbedding = sealed.ciphertext,
                    iv = sealed.iv,
                    sampleIndex = idx
                )
            )
        }
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
        embeddingDao.deleteForStudent(matricula)
        studentDao.delete(matricula)
    }

    /**
     * Galería descifrada para comparación (solo embeddings, nunca fotos).
     * Incluye a todos los alumnos con muestras; el kiosco decide por estatus
     * (BAJA / SUSPENDIDO / PENDING ⇒ denegado con motivo en bitácora).
     */
    suspend fun loadGalleryEmbeddings(): Map<String, List<FloatArray>> {
        val all = embeddingDao.getAll()
        return all.groupBy { it.matricula }.mapValues { (_, list) ->
            list.map { e ->
                val plain = crypto.decrypt(e.encryptedEmbedding, e.iv)
                crypto.bytesToFloatArray(plain)
            }
        }
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
                // Seed directory entry without face samples
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

    suspend fun sampleCount(matricula: String): Int = embeddingDao.countForStudent(matricula)

    private fun StudentEntity.toDomain() = Student(
        matricula = matricula,
        nombre = nombre,
        carrera = carrera,
        status = status,
        consentVersion = consentVersion,
        consentTimestampMs = consentTimestampMs,
        createdAtMs = createdAtMs,
        approvedAtMs = approvedAtMs,
        approvedByGuard = approvedByGuard
    )
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
