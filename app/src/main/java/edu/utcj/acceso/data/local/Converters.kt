package edu.utcj.acceso.data.local

import androidx.room.TypeConverter
import edu.utcj.acceso.domain.model.AccessMethod
import edu.utcj.acceso.domain.model.AccessResult
import edu.utcj.acceso.domain.model.IncidentCategory
import edu.utcj.acceso.domain.model.StudentStatus

class Converters {
    @TypeConverter fun fromStudentStatus(v: StudentStatus): String = v.name
    @TypeConverter fun toStudentStatus(v: String): StudentStatus = StudentStatus.valueOf(v)

    @TypeConverter fun fromAccessResult(v: AccessResult): String = v.name
    @TypeConverter fun toAccessResult(v: String): AccessResult = AccessResult.valueOf(v)

    @TypeConverter fun fromAccessMethod(v: AccessMethod): String = v.name
    @TypeConverter fun toAccessMethod(v: String): AccessMethod = AccessMethod.valueOf(v)

    @TypeConverter fun fromIncidentCategory(v: IncidentCategory): String = v.name
    @TypeConverter fun toIncidentCategory(v: String): IncidentCategory = IncidentCategory.valueOf(v)
}
