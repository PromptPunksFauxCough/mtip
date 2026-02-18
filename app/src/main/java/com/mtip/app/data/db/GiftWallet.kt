package com.mtip.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gift_wallets")
data class GiftWallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val primaryAddress: String,
    val seed: String,
    val restoreHeight: Long,
    val createdAt: Long,
    val cachedBalance: Long = 0,
    val lastCheckedAt: Long = 0,
    val label: String = "",
    val networkType: Int = 0,
    val lastSyncedHeight: Long = 0
)