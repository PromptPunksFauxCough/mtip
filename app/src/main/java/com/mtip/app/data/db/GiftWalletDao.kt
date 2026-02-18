package com.mtip.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GiftWalletDao {
    @Query("SELECT * FROM gift_wallets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GiftWallet>>

    @Query("SELECT * FROM gift_wallets WHERE id = :id")
    suspend fun getById(id: Long): GiftWallet?

    @Insert
    suspend fun insert(wallet: GiftWallet): Long

    @Query(
        "UPDATE gift_wallets SET cachedBalance = :balance, " +
            "lastCheckedAt = :at, lastSyncedHeight = :syncedHeight " +
            "WHERE id = :id"
    )
    suspend fun updateBalance(
        id: Long, balance: Long, at: Long,
        syncedHeight: Long
    )

    @Delete
    suspend fun delete(wallet: GiftWallet)
}