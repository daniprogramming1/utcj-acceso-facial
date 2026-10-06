package edu.utcj.acceso.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.utcj.acceso.ui.guard.ChangePasswordScreen
import edu.utcj.acceso.ui.guard.FirstPasswordSetupScreen
import edu.utcj.acceso.ui.guard.GuardHomeScreen
import edu.utcj.acceso.ui.guard.GuardLoginScreen
import edu.utcj.acceso.ui.guard.ReauthScreen
import edu.utcj.acceso.ui.kiosk.KioskResultScreen
import edu.utcj.acceso.ui.kiosk.KioskScreen
import edu.utcj.acceso.ui.log.AccessLogScreen
import edu.utcj.acceso.ui.panel.AlertsScreen
import edu.utcj.acceso.ui.panel.DashboardScreen
import edu.utcj.acceso.ui.panel.EvalModeScreen
import edu.utcj.acceso.ui.panel.IncidentsScreen
import edu.utcj.acceso.ui.panel.ManualEntryScreen
import edu.utcj.acceso.ui.panel.PendingApprovalsScreen
import edu.utcj.acceso.ui.panel.SettingsScreen
import edu.utcj.acceso.ui.panel.StudentSearchScreen
import edu.utcj.acceso.ui.panel.VisitorsScreen
import edu.utcj.acceso.ui.role.RoleSelectScreen
import edu.utcj.acceso.ui.student.DeleteDataScreen
import edu.utcj.acceso.ui.student.PrivacyConsentScreen
import edu.utcj.acceso.ui.student.RegistrationScreen
import edu.utcj.acceso.ui.student.StudentQrScreen
import edu.utcj.acceso.ui.student.VerificationScreen
import java.net.URLDecoder

@Composable
fun AccesoNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.ROLE_SELECT) {
        composable(Routes.ROLE_SELECT) {
            RoleSelectScreen(
                onStudent = { nav.navigate(Routes.PRIVACY_CONSENT) },
                onStudentQr = { nav.navigate(Routes.STUDENT_QR) },
                onGuard = { nav.navigate(Routes.GUARD_LOGIN) }
            )
        }
        composable(Routes.PRIVACY_CONSENT) {
            PrivacyConsentScreen(
                onAccept = { nav.navigate(Routes.STUDENT_REGISTER) },
                onBack = { nav.popBackStack() },
                onDeleteData = { nav.navigate(Routes.DELETE_DATA) }
            )
        }
        composable(Routes.STUDENT_REGISTER) {
            RegistrationScreen(
                onDone = { nav.navigate(Routes.STUDENT_VERIFY) { popUpTo(Routes.ROLE_SELECT) } },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.STUDENT_VERIFY) {
            VerificationScreen(
                onShowQr = { nav.navigate(Routes.STUDENT_QR) },
                onGoKioskHint = { nav.navigate(Routes.ROLE_SELECT) { popUpTo(Routes.ROLE_SELECT) { inclusive = true } } },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.STUDENT_QR) {
            StudentQrScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.DELETE_DATA) {
            DeleteDataScreen(
                onDone = { nav.navigate(Routes.ROLE_SELECT) { popUpTo(Routes.ROLE_SELECT) { inclusive = true } } },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.GUARD_LOGIN) {
            GuardLoginScreen(
                onSuccess = { nav.navigate(Routes.GUARD_HOME) { popUpTo(Routes.ROLE_SELECT) } },
                onNeedSetup = { nav.navigate(Routes.FIRST_PASSWORD) { popUpTo(Routes.GUARD_LOGIN) { inclusive = true } } },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.FIRST_PASSWORD) {
            FirstPasswordSetupScreen(
                onDone = { nav.navigate(Routes.GUARD_HOME) { popUpTo(Routes.ROLE_SELECT) } },
                onBack = { nav.navigate(Routes.ROLE_SELECT) { popUpTo(Routes.ROLE_SELECT) { inclusive = true } } }
            )
        }
        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }
        composable(Routes.GUARD_HOME) {
            GuardHomeScreen(
                onKiosk = { nav.navigate(Routes.KIOSK) },
                onLog = { nav.navigate(Routes.ACCESS_LOG) },
                onSearch = { nav.navigate(Routes.STUDENT_SEARCH) },
                onPending = { nav.navigate(Routes.PENDING) },
                onManual = { nav.navigate(Routes.MANUAL_ENTRY) },
                onVisitors = { nav.navigate(Routes.VISITORS) },
                onIncidents = { nav.navigate(Routes.INCIDENTS) },
                onDashboard = { nav.navigate(Routes.DASHBOARD) },
                onAlerts = { nav.navigate(Routes.ALERTS) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onChangePassword = { nav.navigate(Routes.CHANGE_PASSWORD) },
                onEval = { nav.navigate(Routes.EVAL_MODE) },
                onLogout = { nav.navigate(Routes.ROLE_SELECT) { popUpTo(Routes.ROLE_SELECT) { inclusive = true } } }
            )
        }
        composable(Routes.KIOSK) {
            KioskScreen(
                onResult = { allowed, name, mat ->
                    nav.navigate(Routes.kioskResult(allowed, name, mat))
                },
                onExitRequest = { nav.navigate(Routes.reauth(Routes.GUARD_HOME)) }
            )
        }
        composable(
            Routes.KIOSK_RESULT,
            arguments = listOf(
                navArgument("allowed") { type = NavType.BoolType },
                navArgument("name") { type = NavType.StringType },
                navArgument("matricula") { type = NavType.StringType }
            )
        ) { entry ->
            val allowed = entry.arguments?.getBoolean("allowed") == true
            val name = URLDecoder.decode(entry.arguments?.getString("name") ?: "", "UTF-8")
            val mat = URLDecoder.decode(entry.arguments?.getString("matricula") ?: "", "UTF-8")
            KioskResultScreen(
                allowed = allowed,
                name = name,
                matricula = mat,
                onReset = { nav.popBackStack(Routes.KIOSK, inclusive = false) }
            )
        }
        composable(
            Routes.REAUTH,
            arguments = listOf(navArgument("target") { type = NavType.StringType })
        ) { entry ->
            val target = entry.arguments?.getString("target") ?: Routes.GUARD_HOME
            ReauthScreen(
                onSuccess = {
                    nav.navigate(target) {
                        popUpTo(Routes.KIOSK) { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.ACCESS_LOG) { AccessLogScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.STUDENT_SEARCH) { StudentSearchScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PENDING) { PendingApprovalsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.MANUAL_ENTRY) { ManualEntryScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.VISITORS) { VisitorsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.INCIDENTS) { IncidentsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.DASHBOARD) { DashboardScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ALERTS) { AlertsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.EVAL_MODE) { EvalModeScreen(onBack = { nav.popBackStack() }) }
    }
}
