package com.mtip.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtip.app.BuildConfig
import com.mtip.app.MTipApp

@Composable
fun OnboardingScreen(onCreate: () -> Unit, onClaim: () -> Unit, onSettings: () -> Unit) {
    val app = LocalContext.current.applicationContext as MTipApp

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ɱTip", fontSize = 48.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Monero gift wallets, simplified",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(4.dp))
            Text("⚠ ${app.network.name}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.height(64.dp))
        Button(onClick = onCreate, Modifier.fillMaxWidth().height(56.dp)) {
            Text("Create Gift Wallet", fontSize = 18.sp)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onClaim, Modifier.fillMaxWidth().height(56.dp)) {
            Text("Claim Gift Wallet", fontSize = 18.sp)
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSettings) { Text("Settings") }
    }
}