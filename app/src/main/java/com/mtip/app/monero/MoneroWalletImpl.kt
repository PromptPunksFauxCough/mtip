package com.mtip.app.monero

import com.mtip.app.MTipApp
import com.mtip.app.domain.BalanceResult
import com.mtip.app.domain.CreatedWallet
import com.mtip.app.domain.MoneroWallet
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class MoneroWalletImpl : MoneroWallet {

    private val lib: MoneroLibrary by lazy {
        Native.load(
            "wallet2_api_c", MoneroLibrary::class.java
        )
    }

    private val wm: Pointer by lazy {
        lib.MONERO_WalletManagerFactory_getWalletManager()
    }

    private val mutex = Mutex()

    override suspend fun create(
        filesDir: String, networkType: Int
    ): CreatedWallet = mutex.withLock {
        withContext(Dispatchers.IO) {
            val nodeUri = MTipApp.instance.currentNodeUri
            val path =
                "$filesDir/gift_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}"

            cleanup(path)

            val wallet =
                lib.MONERO_WalletManager_createWallet(
                    wm, path, "", "English", networkType
                )
            checkStatus(wallet, "create")

            val address =
                lib.MONERO_Wallet_address(wallet, 0L, 0L)
            val seed =
                lib.MONERO_Wallet_seed(wallet, "")

            if (seed.isBlank()) {
                lib.MONERO_WalletManager_closeWallet(
                    wm, wallet, 0
                )
                cleanup(path)
                throw IllegalStateException(
                    "Wallet created but seed is blank"
                )
            }

            lib.MONERO_Wallet_init(
                wallet, nodeUri, 0L, "", "", 0, 0, ""
            )
            lib.MONERO_Wallet_setTrustedDaemon(wallet, 1)

            val height =
                lib.MONERO_Wallet_daemonBlockChainHeight(
                    wallet
                )
            if (height == 0L) {
                lib.MONERO_WalletManager_closeWallet(
                    wm, wallet, 0
                )
                cleanup(path)
                throw IllegalStateException(
                    "Cannot reach daemon at $nodeUri"
                )
            }

            val restoreHeight =
                if (height > 10) height - 10 else 1L

            lib.MONERO_WalletManager_closeWallet(
                wm, wallet, 0
            )
            cleanup(path)

            CreatedWallet(address, seed, restoreHeight)
        }
    }

    private data class SyncResult(
        val wallet: Pointer,
        val path: String,
        val syncedHeight: Long
    )

    private fun syncWallet(
        seed: String, restoreHeight: Long,
        nodeUri: String, filesDir: String,
        networkType: Int
    ): SyncResult {
        val path =
            "$filesDir/tmp_${System.currentTimeMillis()}"
        cleanup(path)

        val tmpPath = "${path}_tmp"
        val tmpW =
            lib.MONERO_WalletManager_recoveryWallet(
                wm, tmpPath, "", seed, networkType,
                restoreHeight, 1L, ""
            )
        checkStatus(tmpW, "tmp-recovery")

        val addr =
            lib.MONERO_Wallet_address(tmpW, 0L, 0L)
        val viewKey =
            lib.MONERO_Wallet_secretViewKey(tmpW)
        val spendKey =
            lib.MONERO_Wallet_secretSpendKey(tmpW)

        lib.MONERO_WalletManager_closeWallet(
            wm, tmpW, 0
        )
        cleanup(tmpPath)

        val wallet =
            lib.MONERO_WalletManager_createWalletFromKeys(
                wm, path, "", "English",
                networkType, restoreHeight,
                addr, viewKey, spendKey, 1L
            )
        checkStatus(wallet, "fromKeys")

        val listener =
            lib.MONERO_cw_getWalletListener(wallet)

        lib.MONERO_Wallet_init(
            wallet, nodeUri, 0L, "", "", 0, 0, ""
        )
        lib.MONERO_Wallet_setTrustedDaemon(wallet, 1)
        lib.MONERO_Wallet_setRecoveringFromSeed(
            wallet, 1
        )
        lib.MONERO_Wallet_setRefreshFromBlockHeight(
            wallet, restoreHeight
        )

        val daemonH =
            lib.MONERO_Wallet_daemonBlockChainHeight(
                wallet
            )
        if (daemonH == 0L) {
            lib.MONERO_WalletManager_closeWallet(
                wm, wallet, 0
            )
            cleanup(path)
            throw IllegalStateException(
                "Daemon not reachable at $nodeUri"
            )
        }

        lib.MONERO_Wallet_startRefresh(wallet)

        for (round in 1..300) {
            Thread.sleep(1000)

            val needR =
                lib.MONERO_cw_WalletListener_isNeedToRefresh(
                    listener
                )
            if (needR != 0) {
                lib.MONERO_Wallet_store(wallet, path)
                lib.MONERO_cw_WalletListener_resetNeedToRefresh(
                    listener
                )
            }

            val bal =
                lib.MONERO_Wallet_balance(wallet, 0)
            if (bal > 0) break

            val h =
                lib.MONERO_Wallet_blockChainHeight(
                    wallet
                )
            if (h >= daemonH) {
                Thread.sleep(3000)
                break
            }
        }

        lib.MONERO_Wallet_pauseRefresh(wallet)

        val syncedHeight =
            lib.MONERO_Wallet_blockChainHeight(wallet)

        return SyncResult(wallet, path, syncedHeight)
    }

    override suspend fun getBalance(
        seed: String, restoreHeight: Long,
        nodeUri: String, filesDir: String,
        networkType: Int
    ): Long {
        return getBalanceWithHeight(
            seed, restoreHeight, nodeUri, filesDir,
            networkType
        ).balance
    }

    override suspend fun getBalanceWithHeight(
        seed: String, restoreHeight: Long,
        nodeUri: String, filesDir: String,
        networkType: Int
    ): BalanceResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            var path = ""
            try {
                val result = syncWallet(
                    seed, restoreHeight, nodeUri,
                    filesDir, networkType
                )
                path = result.path

                val bal =
                    lib.MONERO_Wallet_balance(
                        result.wallet, 0
                    )

                lib.MONERO_WalletManager_closeWallet(
                    wm, result.wallet, 0
                )

                BalanceResult(bal, result.syncedHeight)
            } finally {
                if (path.isNotEmpty()) cleanup(path)
            }
        }
    }

    override suspend fun sweepAll(
        seed: String, restoreHeight: Long,
        destination: String, nodeUri: String,
        filesDir: String, networkType: Int
    ): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            var path = ""
            try {
                val result = syncWallet(
                    seed, restoreHeight, nodeUri,
                    filesDir, networkType
                )
                path = result.path
                val wallet = result.wallet

                val unl =
                    lib.MONERO_Wallet_unlockedBalance(
                        wallet, 0
                    )

                if (unl <= 0) {
                    lib.MONERO_WalletManager_closeWallet(
                        wm, wallet, 0
                    )
                    throw IllegalStateException(
                        "No unlocked funds. unl=$unl"
                    )
                }

                val tx =
                    lib.MONERO_Wallet_createTransaction(
                        wallet, destination, "",
                        0L, 0, 0, 0, "", ""
                    )
                val txS =
                    lib.MONERO_PendingTransaction_status(
                        tx
                    )

                if (txS != 0) {
                    val err =
                        lib.MONERO_PendingTransaction_errorString(
                            tx
                        )
                    lib.MONERO_WalletManager_closeWallet(
                        wm, wallet, 0
                    )
                    throw IllegalStateException(
                        "Sweep failed: $err"
                    )
                }

                val commitResult =
                    lib.MONERO_PendingTransaction_commit(
                        tx, "", 0
                    )

                if (commitResult != 1) {
                    val walletErr =
                        lib.MONERO_Wallet_errorString(
                            wallet
                        )
                    val txErr =
                        lib.MONERO_PendingTransaction_errorString(
                            tx
                        )
                    lib.MONERO_WalletManager_closeWallet(
                        wm, wallet, 0
                    )
                    throw IllegalStateException(
                        "Commit failed ($commitResult): wallet=$walletErr tx=$txErr"
                    )
                }

                lib.MONERO_Wallet_store(wallet, path)

                var txid =
                    lib.MONERO_PendingTransaction_txid(
                        tx, ","
                    )
                if (txid.isNullOrBlank()) {
                    txid = getLastOutgoingTxHash(wallet)
                }

                lib.MONERO_WalletManager_closeWallet(
                    wm, wallet, 0
                )
                txid
            } finally {
                if (path.isNotEmpty()) cleanup(path)
            }
        }
    }

    private fun getLastOutgoingTxHash(
        wallet: Pointer
    ): String {
        val histPtr =
            lib.MONERO_Wallet_history(wallet)
        lib.MONERO_TransactionHistory_refresh(histPtr)
        val count =
            lib.MONERO_TransactionHistory_count(histPtr)
        for (i in count - 1 downTo 0) {
            val txPtr =
                lib.MONERO_TransactionHistory_transaction(
                    histPtr, i
                )
            val dir =
                lib.MONERO_TransactionInfo_direction(
                    txPtr
                )
            if (dir == 1) {
                return lib.MONERO_TransactionInfo_hash(
                    txPtr
                )
            }
        }
        return "submitted"
    }

    override fun isValidAddress(
        address: String
    ): Boolean {
        return address.length in 95..106
            && address.first() in
            listOf('4', '8', '5', '7', '9')
    }

    private fun checkStatus(
        wallet: Pointer, step: String
    ) {
        if (lib.MONERO_Wallet_status(wallet) != 0) {
            throw IllegalStateException(
                "[$step] ${lib.MONERO_Wallet_errorString(
                    wallet
                )}"
            )
        }
    }

    private fun cleanup(path: String) {
        listOf("", ".keys", ".address.txt").forEach {
            File("$path$it").delete()
        }
    }

    @Suppress("FunctionName")
    private interface MoneroLibrary : Library {
        fun MONERO_WalletManagerFactory_getWalletManager():
            Pointer

        fun MONERO_WalletManager_createWallet(
            wm: Pointer, path: String,
            password: String, language: String,
            networkType: Int
        ): Pointer

        fun MONERO_WalletManager_recoveryWallet(
            wm: Pointer, path: String,
            password: String, mnemonic: String,
            networkType: Int, restoreHeight: Long,
            kdfRounds: Long, seedOffset: String
        ): Pointer

        fun MONERO_WalletManager_openWallet(
            wm: Pointer, path: String,
            password: String, networkType: Int
        ): Pointer

        fun MONERO_WalletManager_createWalletFromKeys(
            wm: Pointer, path: String,
            password: String, language: String,
            nettype: Int, restoreHeight: Long,
            addressString: String,
            viewKeyString: String,
            spendKeyString: String,
            kdfRounds: Long
        ): Pointer

        fun MONERO_WalletManager_closeWallet(
            wm: Pointer, wallet: Pointer, store: Int
        ): Int

        fun MONERO_Wallet_status(wallet: Pointer): Int
        fun MONERO_Wallet_errorString(
            wallet: Pointer
        ): String

        fun MONERO_Wallet_address(
            wallet: Pointer, accountIndex: Long,
            addressIndex: Long
        ): String

        fun MONERO_Wallet_seed(
            wallet: Pointer, seedOffset: String
        ): String

        fun MONERO_Wallet_secretSpendKey(
            wallet: Pointer
        ): String

        fun MONERO_Wallet_secretViewKey(
            wallet: Pointer
        ): String

        fun MONERO_Wallet_store(
            wallet: Pointer, path: String
        ): Int

        fun MONERO_Wallet_init(
            wallet: Pointer, daemonAddress: String,
            upperTransactionSizeLimit: Long,
            daemonUsername: String,
            daemonPassword: String,
            useSsl: Int, lightWallet: Int,
            proxyAddress: String
        ): Int

        fun MONERO_Wallet_connected(
            wallet: Pointer
        ): Int

        fun MONERO_Wallet_setTrustedDaemon(
            wallet: Pointer, arg: Int
        )

        fun MONERO_Wallet_setRecoveringFromSeed(
            wallet: Pointer, recoveringFromSeed: Int
        )

        fun MONERO_Wallet_setRefreshFromBlockHeight(
            wallet: Pointer, refreshFromBlockHeight: Long
        )

        fun MONERO_Wallet_getRefreshFromBlockHeight(
            wallet: Pointer
        ): Long

        fun MONERO_Wallet_refresh(
            wallet: Pointer
        ): Int

        fun MONERO_Wallet_startRefresh(
            wallet: Pointer
        )

        fun MONERO_Wallet_pauseRefresh(
            wallet: Pointer
        )

        fun MONERO_Wallet_rescanBlockchain(
            wallet: Pointer
        ): Int

        fun MONERO_Wallet_synchronized(
            wallet: Pointer
        ): Int

        fun MONERO_Wallet_blockChainHeight(
            wallet: Pointer
        ): Long

        fun MONERO_Wallet_daemonBlockChainHeight(
            wallet: Pointer
        ): Long

        fun MONERO_Wallet_balance(
            wallet: Pointer, accountIndex: Int
        ): Long

        fun MONERO_Wallet_unlockedBalance(
            wallet: Pointer, accountIndex: Int
        ): Long

        fun MONERO_Wallet_watchOnly(
            wallet: Pointer
        ): Int

        fun MONERO_Wallet_getBytesReceived(
            wallet: Pointer
        ): Long

        fun MONERO_Wallet_history(
            wallet: Pointer
        ): Pointer

        fun MONERO_TransactionHistory_refresh(
            txHistory: Pointer
        )

        fun MONERO_TransactionHistory_count(
            txHistory: Pointer
        ): Int

        fun MONERO_TransactionHistory_transaction(
            txHistory: Pointer, index: Int
        ): Pointer

        fun MONERO_TransactionInfo_amount(
            txInfo: Pointer
        ): Long

        fun MONERO_TransactionInfo_blockHeight(
            txInfo: Pointer
        ): Long

        fun MONERO_TransactionInfo_direction(
            txInfo: Pointer
        ): Int

        fun MONERO_TransactionInfo_hash(
            txInfo: Pointer
        ): String

        fun MONERO_Wallet_createTransaction(
            wallet: Pointer, dstAddr: String,
            paymentId: String, amount: Long,
            mixinCount: Int,
            pendingTransactionPriority: Int,
            subaddrAccount: Int,
            preferredInputs: String,
            separator: String
        ): Pointer

        fun MONERO_PendingTransaction_status(
            pt: Pointer
        ): Int

        fun MONERO_PendingTransaction_errorString(
            pt: Pointer
        ): String

        fun MONERO_PendingTransaction_fee(
            pt: Pointer
        ): Long

        fun MONERO_PendingTransaction_commit(
            pt: Pointer, filename: String,
            overwrite: Int
        ): Int

        fun MONERO_PendingTransaction_txid(
            pt: Pointer, separator: String
        ): String

        fun MONERO_cw_getWalletListener(
            wallet: Pointer
        ): Pointer

        fun MONERO_cw_WalletListener_resetNeedToRefresh(
            listener: Pointer
        )

        fun MONERO_cw_WalletListener_isNeedToRefresh(
            listener: Pointer
        ): Int

        fun MONERO_cw_WalletListener_isNewTransactionExist(
            listener: Pointer
        ): Int

        fun MONERO_cw_WalletListener_resetIsNewTransactionExist(
            listener: Pointer
        )

        fun MONERO_cw_WalletListener_height(
            listener: Pointer
        ): Long

        fun MONERO_DEBUG_test1(x: Int): Int
        fun MONERO_DEBUG_test2(x: Int): Int
    }
}