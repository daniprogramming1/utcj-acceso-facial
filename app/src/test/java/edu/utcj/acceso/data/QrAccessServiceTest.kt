package edu.utcj.acceso.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import edu.utcj.acceso.data.local.AppDatabase
import edu.utcj.acceso.data.local.StudentEntity
import edu.utcj.acceso.data.qr.QrAccessService
import edu.utcj.acceso.data.qr.ScanDebouncer
import edu.utcj.acceso.data.qr.ScanOutcome
import edu.utcj.acceso.data.remote.StudentStatusCsvDataSource
import edu.utcj.acceso.data.repository.AccessLogRepository
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.data.repository.StudentRepository
import edu.utcj.acceso.data.sync.SyncQueue
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.StudentStatus
import edu.utcj.acceso.domain.qr.DenialReason
import edu.utcj.acceso.domain.qr.QrCrypto
import edu.utcj.acceso.domain.qr.RegistrationCheck
import edu.utcj.acceso.domain.qr.SoftwareQrSigner
import edu.utcj.acceso.testutil.MemoryKeyValueStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/** Verificación del guardia contra una base Room real: registro → aprobación → acceso y cada rechazo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QrAccessServiceTest {
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var students: StudentRepository
    private lateinit var service: QrAccessService
    private val phone = SoftwareQrSigner.generate()

    /** Hoy a las 12:00 locales (dentro del horario por defecto). */
    private val noon = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(ctx)
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        settings = SettingsRepository(MemoryKeyValueStore())
        settings.setHours(6, 22)
        students = StudentRepository(db.studentDao(), StudentStatusCsvDataSource(ctx))
        service = QrAccessService(students, db.usedNonceDao(), settings, AccessLogRepository(db.accessEventDao(), SyncQueue(db.syncQueueDao()), ctx))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun approve(mat: String = "20240001") {
        val reg = service.scan(QrCrypto.registrationQr(phone, mat, "Ana López", "TI", null, "2.0.0", noon), noon)
        val ok = (reg as ScanOutcome.Registration).check as RegistrationCheck.Ok
        students.approveFromRegistration(ok.payload, "Guardia", approve = true)
    }

    private fun access(mat: String = "20240001", validity: Int = 60, at: Long = noon) = QrCrypto.accessQr(phone, mat, validity, at)

    private suspend fun reasonOf(raw: String, now: Long = noon): DenialReason? =
        (service.scan(raw, now) as ScanOutcome.Access).reason

    @Test
    fun approvedStudent_isAllowed_andNonceIsSingleUse() = runBlocking {
        approve()
        val qr = access()
        val first = service.scan(qr, noon + 1_000) as ScanOutcome.Access
        assertTrue(first.allowed)
        assertEquals("Ana López", first.student?.nombre)
        assertEquals(DenialReason.REUSED, reasonOf(qr, noon + 2_000))
    }

    @Test
    fun deniedReasons() = runBlocking {
        assertEquals(DenialReason.NOT_REGISTERED, reasonOf(access("20249999")))
        approve()
        assertEquals(DenialReason.EXPIRED, reasonOf(access(at = noon - 10 * 60_000)))
        assertEquals(DenialReason.NOT_YET_VALID, reasonOf(access(at = noon + 5 * 60_000)))
        settings.setGuardMaxQrValiditySec(60)
        assertEquals(DenialReason.LIFETIME_TOO_LONG, reasonOf(access(validity = 300)))
        val other = SoftwareQrSigner.generate()
        assertEquals(DenialReason.BAD_SIGNATURE, reasonOf(QrCrypto.accessQr(other, "20240001", 60, noon)))
        settings.setHours(13, 14)
        assertEquals(DenialReason.OUT_OF_HOURS, reasonOf(access()))
        settings.setHours(6, 22)
        db.studentDao().getByMatricula("20240001")!!.let { db.studentDao().upsert(it.copy(status = StudentStatus.BAJA)) }
        assertEquals(DenialReason.BAJA, reasonOf(access()))
        db.studentDao().getByMatricula("20240001")!!.let { db.studentDao().upsert(it.copy(status = StudentStatus.SUSPENDIDO)) }
        assertEquals(DenialReason.SUSPENDIDO, reasonOf(access()))
        assertTrue(service.scan("https://example.com", noon) is ScanOutcome.Invalid)
    }

    @Test
    fun registration_ofBajaStudent_isBlocked() = runBlocking {
        db.studentDao().upsert(StudentEntity(matricula = "20240002", nombre = "Luis", status = StudentStatus.BAJA))
        val reg = service.scan(QrCrypto.registrationQr(phone, "20240002", "Luis", "TI", null, "2.0.0", noon), noon)
        assertTrue((reg as ScanOutcome.Registration).check is RegistrationCheck.Blocked)
    }

    @Test
    fun expiredNonces_arePurged() = runBlocking {
        approve()
        assertTrue((service.scan(access(), noon) as ScanOutcome.Access).allowed)
        assertEquals(1, db.usedNonceDao().exists(db.query("SELECT nonce FROM used_nonces", null).use { it.moveToFirst(); it.getString(0) }))
        assertEquals(DenialReason.EXPIRED, reasonOf(access(), noon + 10 * 60_000))
        assertEquals(0, db.query("SELECT COUNT(*) FROM used_nonces", null).use { it.moveToFirst(); it.getInt(0) })
    }

    @Test
    fun log_writesQrEvent() = runBlocking {
        approve()
        val outcome = service.scan(access(), noon) as ScanOutcome.Access
        service.log(outcome, "Guardia", AccessResult.ALLOWED)
        val ev = db.accessEventDao().getSince(0).single()
        assertEquals(AccessMethod.QR, ev.method)
        assertEquals("20240001", ev.matricula)
        assertEquals("Guardia", ev.authorizingGuard)
    }

    @Test
    fun debouncer_ignoresSameCodeWhileVisible() {
        val d = ScanDebouncer(quietMs = 4_000)
        assertTrue(d.accept("a", 0))
        assertFalse(d.accept("a", 1_000))
        assertFalse(d.accept("a", 4_500)) // sigue viéndose (último visto a 1 000 ms + 3 500)
        assertTrue(d.accept("a", 9_000))
        assertTrue(d.accept("b", 9_100))
    }
}
