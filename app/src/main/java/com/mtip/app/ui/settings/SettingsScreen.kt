package com.mtip.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mtip.app.MTipApp
import com.mtip.app.data.nodes.DefaultNodes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as MTipApp
    var custom by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(app.currentNodeUri) }
    val nodes = DefaultNodes.forNetwork(app.network.id)

    Scaffold(topBar = { TopAppBar(
        title = { Text("Settings") },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
    )}) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Text("Network: ${app.network.name}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Current node: $selected", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(custom, { custom = it }, label = { Text("Custom node (host:port)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = {
                if (custom.isNotBlank()) { app.currentNodeUri = custom; selected = custom }
            }, Modifier.padding(top = 8.dp)) { Text("Use Custom") }
            Spacer(Modifier.height(16.dp))
            Text("${app.network.name} Nodes", style = MaterialTheme.typography.titleSmall)
            if (nodes.isEmpty()) {
                Text("No default nodes. Enter custom above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                nodes.forEach { node ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        RadioButton(selected == node.uri, { selected = node.uri; app.currentNodeUri = node.uri })
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(node.uri, style = MaterialTheme.typography.bodySmall)
                            if (node.isOnion) Text("Tor · requires Orbot",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}