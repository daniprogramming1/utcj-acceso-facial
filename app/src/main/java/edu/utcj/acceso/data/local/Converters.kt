package edu.utcj.acceso.data.local

import androidx.room.TypeConverter
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.IncidentCategory
import edu.utcj.acceso.domain.model.StudentStatus

/**
 * Convertidores de enums ⇄ texto.
 *
 * Aceptan y devuelven `null` porque Room también los usa para enlazar parámetros opcionales de
 * consultas, p. ej. `AccessEventDao.observeFiltered(result = null)` («todos los resultados»).
 * Con parámetros no nulos, Room lanzaba `NullPointerException` al crear la consulta y la app se
 * cerraba al abrir el panel (tablero y bitácora).
 */
class Converters {
    @TypeConverter fun fromStudentStatus(v: StudentStatus?): String? = v?.name
    @TypeConverter fun toStudentStatus(v: String?): StudentStatus? = v?.let { StudentStatus.valueOf(it) }

    @TypeConverter fun fromAccessResult(v: AccessResult?): String? = v?.name
    @TypeConverter fun toAccessResult(v: String?): AccessResult? = v?.let { AccessResult.valueOf(it) }

    @TypeConverter fun fromAccessMethod(v: AccessMethod?): String? = v?.name
    @TypeConverter fun toAccessMethod(v: String?): AccessMethod? = v?.let { AccessMethod.valueOf(it) }

    @TypeConverter fun fromIncidentCategory(v: IncidentCategory?): String? = v?.name
    @TypeConverter fun toIncidentCategory(v: String?): IncidentCategory? = v?.let { IncidentCategory.valueOf(it) }
}
