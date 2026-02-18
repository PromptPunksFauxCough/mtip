package com.mtip.app.domain

data class CreatedWallet(
    val primaryAddress: String,
    val seed: String,
    val restoreHeight: Long
)

data class BalanceResult(
    val balance: Long,
    val syncedHeight: Long
)

interface MoneroWallet {
    suspend fun create(
        filesDir: String, networkType: Int = 0
    ): CreatedWallet

    suspend fun getBalance(
        seed: String, restoreHeight: Long,
        nodeUri: String, filesDir: String,
        networkType: Int = 0
    ): Long

    suspend fun getBalanceWithHeight(
        seed: String, restoreHeight: Long,
        nodeUri: String, filesDir: String,
        networkType: Int = 0
    ): BalanceResult

    suspend fun sweepAll(
        seed: String, restoreHeight: Long,
        destination: String, nodeUri: String,
        filesDir: String, networkType: Int = 0
    ): String

    fun isValidAddress(address: String): Boolean
}