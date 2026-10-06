package edu.utcj.acceso.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import edu.utcj.acceso.domain.model.StudentStatus
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class CsvStudentRow(
    val matricula: String,
    val nombre: String,
    val status: StudentStatus,
    val carrera: String
)

/**
 * Reads institutional student status from CSV (assets sample + Document picker import).
 * Door for a future institutional API: swap this data source behind StudentStatusRepository.
 */
@Singleton
class StudentStatusCsvDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun loadFromAssets(assetName: String = "students_status.csv"): List<CsvStudentRow> {
        return context.assets.open(assetName).use { parse(it) }
    }

    fun parse(input: InputStream): List<CsvStudentRow> {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        val rows = mutableListOf<CsvStudentRow>()
        var headerSkipped = false
        reader.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEachLine
            if (!headerSkipped) {
                headerSkipped = true
                if (trimmed.lowercase().startsWith("matricula")) return@forEachLine
            }
            val parts = trimmed.split(',')
            if (parts.size < 3) return@forEachLine
            rows += CsvStudentRow(
                matricula = parts[0].trim(),
                nombre = parts[1].trim(),
                status = StudentStatus.fromCsv(parts[2]),
                carrera = parts.getOrNull(3)?.trim().orEmpty()
            )
        }
        return rows
    }
}
