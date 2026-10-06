package edu.utcj.acceso

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import edu.utcj.acceso.ui.navigation.AccesoNavGraph
import edu.utcj.acceso.ui.theme.AccesoUtcjTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccesoUtcjTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AccesoNavGraph()
                }
            }
        }
    }
}
