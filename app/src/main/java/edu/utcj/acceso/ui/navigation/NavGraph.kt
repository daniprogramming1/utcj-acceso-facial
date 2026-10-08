package edu.utcj.acceso.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.utcj.acceso.ui.admin.AdminShell
import edu.utcj.acceso.ui.admin.EvalModeScreen
import edu.utcj.acceso.ui.admin.ManualEntryScreen
import edu.utcj.acceso.ui.guard.ChangePasswordScreen
import edu.utcj.acceso.ui.guard.FirstPasswordSetupScreen
import edu.utcj.acceso.ui.guard.GuardLoginScreen
import edu.utcj.acceso.ui.guard.ReauthScreen
import edu.utcj.acceso.ui.kiosk.KioskScreen
import edu.utcj.acceso.ui.onboarding.OnboardingScreen
import edu.utcj.acceso.ui.role.RoleSelectScreen
import edu.utcj.acceso.ui.student.DeleteDataScreen
import edu.utcj.acceso.ui.student.RegistrationScreen
import edu.utcj.acceso.ui.student.StudentAccessScreen
import edu.utcj.acceso.ui.student.StudentHomeScreen
import edu.utcj.acceso.ui.theme.AppTheme
import edu.utcj.acceso.ui.theme.Motion
import java.net.URLDecoder

private typealias Enter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias Exit = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

@Composable
fun AccesoNavGraph(
    startDestination: String,
    onOnboardingFinished: () -> Unit,
    isStudentRemembered: () -> Boolean,
    nav: NavHostController = rememberNavController()
) {
    val reduced = AppTheme.reducedMotion
    val enter: Enter = {
        if (reduced) fadeIn(tween(Motion.SHORT))
        else fadeIn(tween(Motion.MEDIUM)) + slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.emphasized)) { it / 8 }
    }
    val exit: Exit = { fadeOut(tween(Motion.SHORT)) }
    val popEnter: Enter = {
        if (reduced) fadeIn(tween(Motion.SHORT))
        else fadeIn(tween(Motion.MEDIUM)) + slideInHorizontally(tween(Motion.MEDIUM, easing = Motion.emphasized)) { -it / 8 }
    }
    val popExit: Exit = {
        if (reduced) fadeOut(tween(Motion.SHORT))
        else fadeOut(tween(Motion.SHORT)) + slideOutHorizontally(tween(Motion.MEDIUM)) { it / 8 }
    }

    fun goRoleSelect() = nav.navigate(Routes.ROLE_SELECT) { popUpTo(0) { inclusive = true } }

    NavHost(
        navController = nav,
        startDestination = startDestination,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = popEnter,
        popExitTransition = popExit
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinish = {
                onOnboardingFinished()
                goRoleSelect()
            })
        }
        composable(Routes.ROLE_SELECT) {
            RoleSelectScreen(
                onStudent = {
                    nav.navigate(if (isStudentRemembered()) Routes.STUDENT_HOME else Routes.STUDENT_REGISTER)
                },
                onStudentAccess = { nav.navigate(Routes.STUDENT_ACCESS) },
                onGuard = { nav.navigate(Routes.GUARD_LOGIN) }
            )
        }

        // ---------- Alumno ----------
        composable(Routes.STUDENT_REGISTER) {
            RegistrationScreen(
                onGoHome = {
                    nav.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.ROLE_SELECT) }
                },
                onFinish = { goRoleSelect() },
                onBack = { nav.popBackStack() },
                onDeleteData = { nav.navigate(Routes.deleteData()) }
            )
        }
        composable(Routes.STUDENT_ACCESS) {
            StudentAccessScreen(
                onOpenHome = { nav.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.ROLE_SELECT) } },
                onRegister = { nav.navigate(Routes.STUDENT_REGISTER) { popUpTo(Routes.ROLE_SELECT) } },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.STUDENT_HOME) {
            StudentHomeScreen(
                onBack = { nav.popBackStack() },
                onReRegister = { nav.navigate(Routes.STUDENT_REGISTER) },
                onDeleteData = { mat -> nav.navigate(Routes.deleteData(mat)) },
                onSignedOut = { goRoleSelect() },
                onNeedAccess = { nav.navigate(Routes.STUDENT_ACCESS) { popUpTo(Routes.ROLE_SELECT) } }
            )
        }
        composable(
            Routes.DELETE_DATA,
            arguments = listOf(navArgument("matricula") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            DeleteDataScreen(
                initialMatricula = URLDecoder.decode(entry.arguments?.getString("matricula").orEmpty(), "UTF-8"),
                onDone = { goRoleSelect() },
                onBack = { nav.popBackStack() }
            )
        }

        // ---------- Personal de seguridad ----------
        composable(Routes.GUARD_LOGIN) {
            GuardLoginScreen(
                onSuccess = { nav.navigate(Routes.ADMIN) { popUpTo(Routes.ROLE_SELECT) } },
                onNeedSetup = { nav.navigate(Routes.FIRST_PASSWORD) { popUpTo(Routes.GUARD_LOGIN) { inclusive = true } } },
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.FIRST_PASSWORD) {
            FirstPasswordSetupScreen(
                onDone = { nav.navigate(Routes.ADMIN) { popUpTo(Routes.ROLE_SELECT) } },
                onBack = { goRoleSelect() }
            )
        }
        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }
        composable(Routes.ADMIN) {
            AdminShell(
                onKiosk = { nav.navigate(Routes.KIOSK) },
                onManualEntry = { mat -> nav.navigate(Routes.manualEntry(mat)) },
                onChangePassword = { nav.navigate(Routes.CHANGE_PASSWORD) },
                onEvalMode = { nav.navigate(Routes.EVAL_MODE) },
                onLogout = { goRoleSelect() }
            )
        }
        composable(
            Routes.KIOSK,
            enterTransition = { fadeIn(tween(Motion.MEDIUM)) },
            exitTransition = { fadeOut(tween(Motion.SHORT)) }
        ) {
            KioskScreen(onExitRequest = { nav.navigate(Routes.reauth(Routes.ADMIN)) })
        }
        composable(
            Routes.REAUTH,
            arguments = listOf(navArgument("target") { type = NavType.StringType })
        ) { entry ->
            val target = entry.arguments?.getString("target") ?: Routes.ADMIN
            ReauthScreen(
                onSuccess = { nav.navigate(target) { popUpTo(Routes.KIOSK) { inclusive = true } } },
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Routes.MANUAL_ENTRY,
            arguments = listOf(navArgument("matricula") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            ManualEntryScreen(
                initialMatricula = URLDecoder.decode(entry.arguments?.getString("matricula").orEmpty(), "UTF-8"),
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.EVAL_MODE) { EvalModeScreen(onBack = { nav.popBackStack() }) }
    }
}
