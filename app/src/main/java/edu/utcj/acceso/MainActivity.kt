package edu.utcj.acceso

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import edu.utcj.acceso.data.repository.SettingsRepository
import edu.utcj.acceso.domain.model.ThemeMode
import edu.utcj.acceso.ui.navigation.AccesoNavGraph
import edu.utcj.acceso.ui.navigation.Routes
import edu.utcj.acceso.ui.theme.AccesoUtcjTheme
import edu.utcj.acceso.util.LocalWindowSizeClass
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settings: SettingsRepository

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val start = if (settings.isOnboardingDone()) Routes.ROLE_SELECT else Routes.ONBOARDING
        setContent {
            val themeMode by settings.themeModeFlow.collectAsStateWithLifecycle()
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                )
                onDispose { }
            }
            CompositionLocalProvider(LocalWindowSizeClass provides calculateWindowSizeClass(this)) {
                AccesoUtcjTheme(themeMode = themeMode) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AccesoNavGraph(
                            startDestination = start,
                            onOnboardingFinished = { settings.setOnboardingDone() },
                            isStudentRemembered = { settings.getRememberedStudent() != null }
                        )
                    }
                }
            }
        }
    }
}
