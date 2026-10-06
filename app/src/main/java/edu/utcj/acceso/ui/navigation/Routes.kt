package edu.utcj.acceso.ui.navigation

object Routes {
    const val ROLE_SELECT = "role_select"
    const val PRIVACY_CONSENT = "privacy_consent"
    const val STUDENT_REGISTER = "student_register"
    const val STUDENT_VERIFY = "student_verify"
    const val DELETE_DATA = "delete_data"
    const val STUDENT_QR = "student_qr"
    const val GUARD_LOGIN = "guard_login"
    const val FIRST_PASSWORD = "first_password"
    const val CHANGE_PASSWORD = "change_password"
    const val GUARD_HOME = "guard_home"
    const val KIOSK = "kiosk"
    const val KIOSK_RESULT = "kiosk_result/{allowed}/{name}/{matricula}"
    const val ACCESS_LOG = "access_log"
    const val STUDENT_SEARCH = "student_search"
    const val PENDING = "pending_approvals"
    const val MANUAL_ENTRY = "manual_entry"
    const val VISITORS = "visitors"
    const val INCIDENTS = "incidents"
    const val DASHBOARD = "dashboard"
    const val ALERTS = "alerts"
    const val SETTINGS = "settings"
    const val EVAL_MODE = "eval_mode"
    const val REAUTH = "reauth/{target}"

    fun kioskResult(allowed: Boolean, name: String, matricula: String) =
        "kiosk_result/$allowed/${java.net.URLEncoder.encode(name, "UTF-8")}/${java.net.URLEncoder.encode(matricula, "UTF-8")}"

    fun reauth(target: String) = "reauth/$target"
}
