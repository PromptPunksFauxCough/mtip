package com.mtip.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [GiftWallet::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun giftWalletDao(): GiftWalletDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(
            context: Context, passphrase: ByteArray
        ): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mtip.db"
                )
                    .openHelperFactory(
                        SupportFactory(passphrase)
                    )
                    .addMigrations(
                        object :
                            androidx.room.migration.Migration(
                                1, 2
                            ) {
                            override fun migrate(
                                db: androidx.sqlite.db.SupportSQLiteDatabase
                            ) {
                                db.execSQL(
                                    "ALTER TABLE gift_wallets " +
                                        "ADD COLUMN networkType " +
                                        "INTEGER NOT NULL DEFAULT 0"
                                )
                            }
                        },
                        object :
                            androidx.room.migration.Migration(
                                2, 3
                            ) {
                            override fun migrate(
                                db: androidx.sqlite.db.SupportSQLiteDatabase
                            ) {
                                db.execSQL(
                                    "ALTER TABLE gift_wallets " +
                                        "ADD COLUMN lastSyncedHeight " +
                                        "INTEGER NOT NULL DEFAULT 0"
                                )
                            }
                        }
                    )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}