package edu.utcj.acceso.ui.guard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import edu.utcj.acceso.data.security.AuthOutcome
import edu.utcj.acceso.ui.components.AccesoHeader
import edu.utcj.acceso.ui.components.PrimaryBigButton

@Composable
fun GuardLoginScreen(
    onSuccess: () -> Unit,
    onNeedSetup: () -> Unit,
    onBack: () -> Unit,
    vm: GuardAuthViewModel = hiltViewModel()
) {
    if (!vm.isPasswordSet()) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onNeedSetup() }
        return
    }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Acceso guardia", onBack = onBack)
        Column(Modifier.padding(24.dp)) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            PrimaryBigButton(text = "Entrar", onClick = {
                when (val r = vm.login(password.toCharArray())) {
                    is AuthOutcome.Success -> onSuccess()
                    is AuthOutcome.Error -> error = r.message
                    is AuthOutcome.Locked -> error = "Bloqueado ${(r.remainingMs / 1000)} s. Intenta más tarde."
                }
            })
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun FirstPasswordSetupScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: GuardAuthViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("Guardia UTCJ") }
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Configurar contraseña", subtitle = "Primera vez", onBack = onBack)
        Column(Modifier.padding(24.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nombre del guardia") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(p1, { p1 = it }, label = { Text("Nueva contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(p2, { p2 = it }, label = { Text("Confirmar contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            PrimaryBigButton(text = "Guardar", onClick = {
                if (p1 != p2) { error = "Las contraseñas no coinciden"; return@PrimaryBigButton }
                when (val r = vm.setup(p1.toCharArray(), name)) {
                    is AuthOutcome.Success -> onDone()
                    is AuthOutcome.Error -> error = r.message
                    is AuthOutcome.Locked -> error = "Bloqueado"
                }
            })
            error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun ChangePasswordScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: GuardAuthViewModel = hiltViewModel()
) {
    var cur by remember { mutableStateOf("") }
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Cambiar contraseña", onBack = onBack)
        Column(Modifier.padding(24.dp)) {
            OutlinedTextField(cur, { cur = it }, label = { Text("Contraseña actual") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(p1, { p1 = it }, label = { Text("Nueva") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(p2, { p2 = it }, label = { Text("Confirmar") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            PrimaryBigButton(text = "Actualizar", onClick = {
                if (p1 != p2) { error = "No coinciden"; return@PrimaryBigButton }
                when (val r = vm.change(cur.toCharArray(), p1.toCharArray())) {
                    is AuthOutcome.Success -> onDone()
                    is AuthOutcome.Error -> error = r.message
                    is AuthOutcome.Locked -> error = "Bloqueado ${(r.remainingMs / 1000)} s"
                }
            })
            error?.let { Text(it) }
        }
    }
}

@Composable
fun ReauthScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    vm: GuardAuthViewModel = hiltViewModel()
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        AccesoHeader(title = "Confirmar identidad", subtitle = "Se requiere contraseña para salir del kiosco", onBack = onBack)
        Column(Modifier.padding(24.dp)) {
            OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            PrimaryBigButton(text = "Confirmar", onClick = {
                when (val r = vm.reauth(password.toCharArray())) {
                    is AuthOutcome.Success -> onSuccess()
                    is AuthOutcome.Error -> error = r.message
                    is AuthOutcome.Locked -> error = "Bloqueado"
                }
            })
            error?.let { Text(it) }
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class GuardAuthViewModel @javax.inject.Inject constructor(
    private val auth: edu.utcj.acceso.data.repository.AuthRepository
) : androidx.lifecycle.ViewModel() {
    fun isPasswordSet() = auth.isPasswordSet()
    fun login(p: CharArray) = auth.login(p)
    fun setup(p: CharArray, name: String) = auth.setupPassword(p, name)
    fun change(c: CharArray, n: CharArray) = auth.changePassword(c, n)
    fun reauth(p: CharArray) = auth.requireReauth(p)
}
