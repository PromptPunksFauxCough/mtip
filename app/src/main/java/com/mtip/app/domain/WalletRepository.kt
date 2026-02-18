package com.mtip.app.domain

import com.mtip.app.data.db.GiftWallet
import com.mtip.app.data.db.GiftWalletDao
import kotlinx.coroutines.flow.Flow

class WalletRepository(
    private val dao: GiftWalletDao,
    private val monero: MoneroWallet,
    private val nodeUri: () -> String,
    private val filesDir: String,
    private val networkType: () -> Int
) {
    val wallets: Flow<List<GiftWallet>> = dao.observeAll()

    suspend fun create(label: String = ""): GiftWallet {
        val net = networkType()
        val w = monero.create(filesDir, net)
        val entity = GiftWallet(
            primaryAddress = w.primaryAddress,
            seed = w.seed,
            restoreHeight = w.restoreHeight,
            createdAt = System.currentTimeMillis(),
            label = label,
            networkType = net
        )
        val id = dao.insert(entity)
        return entity.copy(id = id)
    }

    suspend fun checkBalance(wallet: GiftWallet): Long {
        val scanFrom = if (wallet.lastSyncedHeight > 0) {
            wallet.lastSyncedHeight
        } else {
            wallet.restoreHeight
        }

        val result = monero.getBalanceWithHeight(
            wallet.seed, scanFrom, nodeUri(), filesDir,
            wallet.networkType
        )

        dao.updateBalance(
            wallet.id, result.balance,
            System.currentTimeMillis(),
            result.syncedHeight
        )
        return result.balance
    }

    suspend fun sweep(
        wallet: GiftWallet, destination: String
    ): String =
        monero.sweepAll(
            wallet.seed, wallet.restoreHeight,
            destination, nodeUri(), filesDir,
            wallet.networkType
        )

    suspend fun sweepFromQr(
        seed: String, restoreHeight: Long,
        destination: String, networkType: Int
    ): String =
        monero.sweepAll(
            seed, restoreHeight, destination,
            nodeUri(), filesDir, networkType
        )

    suspend fun delete(wallet: GiftWallet) =
        dao.delete(wallet)
}