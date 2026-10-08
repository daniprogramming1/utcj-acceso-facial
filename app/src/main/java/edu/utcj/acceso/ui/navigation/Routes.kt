package edu.utcj.acceso.ui.navigation

import java.net.URLEncoder

object Routes {
    const val ONBOARDING = "onboarding"
    const val ROLE_SELECT = "role_select"

    // Alumno
    const val STUDENT_REGISTER = "student_register"
    const val STUDENT_HOME = "student_home"
    const val STUDENT_ACCESS = "student_access"
    const val DELETE_DATA = "delete_data?matricula={matricula}"

    // Personal de seguridad
    const val GUARD_LOGIN = "guard_login"
    const val FIRST_PASSWORD = "first_password"
    const val CHANGE_PASSWORD = "change_password"
    const val ADMIN = "admin"
    const val KIOSK = "kiosk"
    const val REAUTH = "reauth/{target}"
    const val MANUAL_ENTRY = "manual_entry?matricula={matricula}"
    const val EVAL_MODE = "eval_mode"

    fun deleteData(matricula: String? = null) =
        "delete_data?matricula=${URLEncoder.encode(matricula.orEmpty(), "UTF-8")}"

    fun manualEntry(matricula: String? = null) =
        "manual_entry?matricula=${URLEncoder.encode(matricula.orEmpty(), "UTF-8")}"

    fun reauth(target: String) = "reauth/$target"
}
