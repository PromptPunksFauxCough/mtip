package com.mtip.app.ui.create

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtip.app.data.db.GiftWallet
import com.mtip.app.domain.WalletRepository
import com.mtip.app.ui.components.generateQrBitmap
import com.mtip.app.util.Formatters
import com.mtip.app.util.PdfGenerator
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletListScreen(repo: WalletRepository, onBack: () -> Unit) {
    val vm: WalletListViewModel = viewModel(factory = WalletListViewModel.Factory(repo))
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var deleteTarget by remember { mutableStateOf<GiftWallet?>(null) }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text("Gift Wallets") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
        )},
        floatingActionButton = { FloatingActionButton(onClick = { vm.create() }) {
            if (state.busy) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Icon(Icons.Default.Add, "Create")
        }}
    ) { pad ->
        if (state.wallets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Tap + to create a gift wallet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.wallets, key = { it.id }) { w ->
                    WalletCard(w, state.expandedId == w.id, w.id in state.checking,
                        onTap = { vm.toggle(w.id) },
                        onCheck = { vm.check(w) },
                        onDelete = { deleteTarget = w },
                        onSavePdf = {
                            val payload = vm.qrPayload(w)
                            val file = PdfGenerator.generate(ctx, w, payload)
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }, "Save gift wallet PDF"))
                        },
                        onCopy = { clip.setText(AnnotatedString(w.primaryAddress)) },
                        onShare = { shareQr(ctx, vm.qrPayload(w), w.id) }
                    )
                }
            }
        }

        state.error?.let { err ->
            AlertDialog(
                onDismissRequest = { vm.clearError() },
                title = { Text("Result") },
                text = {
                    SelectionContainer {
                        Text(err, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = { clip.setText(AnnotatedString(err)) }) { Text("Copy") }
                        TextButton(onClick = { vm.clearError() }) { Text("OK") }
                    }
                }
            )
        }
    }

    deleteTarget?.let { w ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete wallet?") },
            text = { Text("Unclaimed funds will be lost permanently.") },
            confirmButton = { TextButton(onClick = { vm.delete(w); deleteTarget = null }) {
                Text("Delete", color = MaterialTheme.colorScheme.error) }},
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun WalletCard(
    w: GiftWallet, expanded: Boolean, checking: Boolean,
    onTap: () -> Unit, onCheck: () -> Unit, onDelete: () -> Unit,
    onSavePdf: () -> Unit, onCopy: () -> Unit, onShare: () -> Unit
) {
    Card(Modifier.fillMaxWidth().clickable { onTap() }) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(Formatters.truncAddr(w.primaryAddress), fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, "Copy", Modifier.size(16.dp))
                        }
                    }
                    Text(Formatters.xmr(w.cachedBalance), style = MaterialTheme.typography.titleMedium)
                    if (w.lastCheckedAt > 0) {
                        Text("Checked: ${Formatters.date(w.lastCheckedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Height: ${w.restoreHeight}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (checking) CircularProgressIndicator(Modifier.size(20.dp))
            }

            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 12.dp)) {
                    val qr = remember(w.primaryAddress) { generateQrBitmap(w.primaryAddress, 200) }
                    qr?.let {
                        Image(it.asImageBitmap(), "Fund QR",
                            Modifier.size(140.dp).align(Alignment.CenterHorizontally))
                        Text("Scan to fund", Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        AssistChip(onClick = onCheck, label = { Text("Check") },
                            leadingIcon = { Icon(Icons.Default.Refresh, null, Modifier.size(16.dp)) })
                        AssistChip(onClick = onShare, label = { Text("Share") },
                            leadingIcon = { Icon(Icons.Default.Share, null, Modifier.size(16.dp)) })
                        AssistChip(onClick = onSavePdf, label = { Text("PDF") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, Modifier.size(16.dp)) })
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Delete", Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun shareQr(ctx: android.content.Context, payload: String, id: Long) {
    val bmp = generateQrBitmap(payload, 512) ?: return
    val f = File(ctx.cacheDir, "mtip_$id.png")
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "ɱTip — scan to claim your Monero!")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "Share gift wallet"))
}