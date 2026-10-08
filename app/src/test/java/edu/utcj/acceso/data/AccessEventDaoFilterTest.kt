package edu.utcj.acceso.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import edu.utcj.acceso.data.local.AccessEventEntity
import edu.utcj.acceso.data.local.AppDatabase
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regresión: filtros opcionales en `null` no deben lanzar NullPointerException (cierre del panel). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessEventDaoFilterTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun event(t: Long, r: AccessResult) = AccessEventEntity(
        datetimeMs = t, matricula = "2023$t", nombre = "Alumno $t", result = r, method = AccessMethod.FACE, syncKey = "k$t"
    )

    @Test
    fun observeFiltered_allNull_returnsEverything() = runBlocking {
        val dao = db.accessEventDao()
        assertEquals(0, dao.observeFiltered(null, null, null, null).first().size)
        dao.insert(event(1_000, AccessResult.ALLOWED))
        dao.insert(event(2_000, AccessResult.DENIED))
        dao.insert(event(3_000, AccessResult.QR))
        assertEquals(3, dao.observeFiltered(null, null, null, null).first().size)
        assertEquals(2, dao.observeFiltered(2_000, null, null, null).first().size)
    }

    @Test
    fun observeFiltered_byResult() = runBlocking {
        val dao = db.accessEventDao()
        dao.insert(event(1_000, AccessResult.ALLOWED))
        dao.insert(event(2_000, AccessResult.DENIED))
        val denied = dao.observeFiltered(null, null, null, AccessResult.DENIED).first()
        assertEquals(listOf(AccessResult.DENIED), denied.map { it.result })
    }
}
