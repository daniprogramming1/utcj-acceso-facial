package edu.utcj.acceso.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.utcj.acceso.R
import edu.utcj.acceso.ui.theme.UtcjBlue
import edu.utcj.acceso.ui.theme.UtcjTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccesoHeader(
    title: String,
    subtitle: String? = null,
    syncLabel: String? = null,
    onBack: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_utcj_logo),
                    contentDescription = "Logo UTCJ",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                OutlinedButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp)) {
                    Text("Atrás")
                }
            }
        },
        actions = {
            if (syncLabel != null) {
                Text(
                    syncLabel,
                    modifier = Modifier.padding(end = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = UtcjTeal
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
fun BigRoleCard(
    title: String,
    description: String,
    accentTeal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (accentTeal) UtcjTeal.copy(alpha = 0.12f) else UtcjBlue.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = if (accentTeal) UtcjTeal else UtcjBlue)
            Spacer(Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun PrimaryBigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(vertical = 8.dp),
        fontWeight = FontWeight.Bold
    )
}
