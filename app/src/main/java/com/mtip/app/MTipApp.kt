package com.mtip.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.mtip.app.data.db.AppDatabase
import com.mtip.app.data.nodes.DefaultNodes
import com.mtip.app.domain.MoneroNetwork
import com.mtip.app.domain.WalletRepository
import com.mtip.app.monero.MoneroWalletImpl
import java.util.UUID

class MTipApp : Application() {

    companion object {
        lateinit var instance: MTipApp private set
    }

    lateinit var repository: WalletRepository private set

    val network: MoneroNetwork =
        MoneroNetwork.entries.first { it.id == BuildConfig.NETWORK_TYPE }

    var currentNodeUri: String =
        DefaultNodes.defaultForNetwork(BuildConfig.NETWORK_TYPE)

    var pendingClaimUri: String? by mutableStateOf(null)

    override fun onCreate() {
        super.onCreate()
        instance = this
        val db = AppDatabase.get(this, getOrCreateDbKey())
        repository = WalletRepository(
            dao = db.giftWalletDao(),
            monero = MoneroWalletImpl(),
            nodeUri = { currentNodeUri },
            filesDir = filesDir.absolutePath,
            networkType = { network.id }
        )
    }

    private fun getOrCreateDbKey(): ByteArray {
        val master = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(
            this, "mtip_keys", master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val key = prefs.getString("db_key", null)
            ?: UUID.randomUUID().toString().also {
                prefs.edit().putString("db_key", it).apply()
            }
        return key.toByteArray()
    }
}