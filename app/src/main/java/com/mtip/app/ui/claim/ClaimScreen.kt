package com.mtip.app.ui.claim

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.mtip.app.MTipApp
import com.mtip.app.domain.WalletRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimScreen(
    repo: WalletRepository,
    onBack: () -> Unit
) {
    val vm: ClaimViewModel = viewModel(
        factory = ClaimViewModel.Factory(repo)
    )
    val state by vm.state.collectAsState()

    val pendingUri = MTipApp.instance.pendingClaimUri
    LaunchedEffect(pendingUri) {
        if (pendingUri != null) {
            MTipApp.instance.pendingClaimUri = null
            vm.onScanned(pendingUri)
        }
    }

    val scanner = rememberLauncherForActivityResult(
        ScanContract()
    ) { r ->
        r.contents?.let { vm.onScanned(it) }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Claim Gift") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            }
        )
    }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state.step) {
                Step.SCAN -> {
                    Spacer(Modifier.height(48.dp))
                    Icon(
                        Icons.Default.QrCodeScanner, null,
                        Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Scan the ɱTip QR code",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan the QR code on your gift card",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = {
                            scanner.launch(
                                ScanOptions().apply {
                                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    setPrompt("Scan ɱTip gift wallet")
                                    setBeepEnabled(false)
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open Scanner") }
                }

                Step.ADDRESS -> {
                    Text(
                        "Gift card scanned ✓",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Enter your Monero wallet address below.\nFunds will be swept to this address.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        state.address,
                        { vm.setAddress(it) },
                        label = { Text("Your XMR address") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { vm.sweep() },
                        enabled = state.address.length >= 95,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sweep Funds") }
                }

                Step.SWEEPING -> {
                    Spacer(Modifier.height(48.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Syncing & sweeping…")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This may take a few minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Step.DONE -> {
                    val clip = LocalClipboardManager.current
                    val txHash = state.txHash

                    Icon(
                        Icons.Default.CheckCircle, null,
                        Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Funds swept!",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(16.dp))

                    if (txHash.isNotBlank() && txHash != "submitted") {
                        val qrBitmap = remember(txHash) { generateQr(txHash) }
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "TX QR",
                                modifier = Modifier.size(200.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    Text(
                        "TX ID",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    SelectionContainer {
                        Text(
                            txHash,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        clip.setText(AnnotatedString(txHash))
                    }) { Text("Copy TX ID") }
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = onBack) {
                        Text("Done")
                    }
                }

                Step.ERROR -> {
                    val clip = LocalClipboardManager.current
                    Spacer(Modifier.height(48.dp))
                    SelectionContainer {
                        Text(
                            state.error,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Button(onClick = {
                            clip.setText(AnnotatedString(state.error))
                        }) { Text("Copy") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { vm.reset() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

private fun generateQr(data: String): Bitmap? {
    return try {
        val size = 512
        val writer = QRCodeWriter()
        val matrix = writer.encode(
            data, BarcodeFormat.QR_CODE, size, size
        )
        val bmp = Bitmap.createBitmap(
            size, size, Bitmap.Config.ARGB_8888
        )
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(
                    x, y,
                    if (matrix.get(x, y)) Color.BLACK
                    else Color.WHITE
                )
            }
        }
        bmp
    } catch (_: Exception) {
        null
    }
}