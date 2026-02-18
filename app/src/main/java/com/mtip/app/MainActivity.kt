package com.mtip.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.mtip.app.domain.QrCodec
import com.mtip.app.ui.nav.MTipNavHost
import com.mtip.app.ui.nav.Screen
import com.mtip.app.ui.theme.MTipTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)

        setContent {
            val app = applicationContext as MTipApp
            MTipTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    MTipNavHost(nav, app.repository)

                    LaunchedEffect(app.pendingClaimUri) {
                        if (app.pendingClaimUri != null) {
                            nav.navigate(Screen.Claim.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data?.toString() ?: return
        if (QrCodec.decode(uri) != null) {
            (applicationContext as MTipApp).pendingClaimUri = uri
        }
    }
}