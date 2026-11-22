package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        PayoutMethodEntity::class,
        WalletTransactionEntity::class,
        PeriodHistoryEntity::class,
        UserBetEntity::class,
        RoomControlConfigEntity::class,
        RoundAdminLogEntity::class,
        DepositRequestEntity::class,
        WithdrawalRequestEntity::class,
        PasswordResetRequestEntity::class,
        AppControlConfigEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class KelywinDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun payoutMethodDao(): PayoutMethodDao
    abstract fun walletTransactionDao(): WalletTransactionDao
    abstract fun periodHistoryDao(): PeriodHistoryDao
    abstract fun userBetDao(): UserBetDao
    abstract fun roomControlDao(): RoomControlDao
    abstract fun roundAdminLogDao(): RoundAdminLogDao
    abstract fun depositRequestDao(): DepositRequestDao
    abstract fun withdrawalRequestDao(): WithdrawalRequestDao
    abstract fun passwordResetRequestDao(): PasswordResetRequestDao
    abstract fun appControlConfigDao(): AppControlConfigDao

    companion object {
        @Volatile
        private var INSTANCE: KelywinDatabase? = null

        private fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            table: String,
            column: String,
            definition: String
        ) {
            val cursor = db.query("PRAGMA table_info(`$table`)")
            var exists = false
            cursor.use {
                val nameIdx = it.getColumnIndex("name")
                while (it.moveToNext()) {
                    if (nameIdx >= 0 && it.getString(nameIdx) == column) {
                        exists = true
                        break
                    }
                }
            }
            if (!exists) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
            }
        }

        private fun ensureLatestTablesAndColumns(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `room_control_config` (`modeId` TEXT NOT NULL, `controlMode` TEXT NOT NULL, `fallbackMode` TEXT NOT NULL DEFAULT 'RANDOM', `xinPattern` TEXT NOT NULL DEFAULT 'TWO_HIGH_FOUR_LOW', `xinAnchorPeriodIndex` INTEGER NOT NULL DEFAULT 1, `manualNumber` INTEGER NOT NULL DEFAULT 7, `manualTargetPeriodId` TEXT, `manualColor` TEXT NOT NULL DEFAULT 'GREEN', `manualSide` TEXT NOT NULL DEFAULT 'DRAGON', `manualOverrideActive` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`modeId`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `round_admin_logs` (`periodId` TEXT NOT NULL, `modeId` TEXT NOT NULL, `controlModeUsed` TEXT NOT NULL, `winningNumber` INTEGER NOT NULL DEFAULT 7, `winningColor` TEXT NOT NULL, `winningSide` TEXT NOT NULL, `totalBetsAmount` REAL NOT NULL, `totalPayoutAmount` REAL NOT NULL, `netProfit` REAL NOT NULL, `settledAtMillis` INTEGER NOT NULL, PRIMARY KEY(`periodId`, `modeId`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `deposit_requests` (`id` TEXT NOT NULL, `orderId` TEXT NOT NULL DEFAULT '', `userId` TEXT NOT NULL, `userPhone` TEXT NOT NULL, `amount` REAL NOT NULL, `utrNumber` TEXT NOT NULL, `upiIdUsed` TEXT NOT NULL, `gatewayName` TEXT NOT NULL DEFAULT 'Kelywin Dynamic UPI Gateway', `status` TEXT NOT NULL, `bonusApplied` REAL NOT NULL, `adminNote` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `processedAtMillis` INTEGER, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `withdrawal_requests` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `userPhone` TEXT NOT NULL, `amount` REAL NOT NULL, `methodType` TEXT NOT NULL, `methodLabel` TEXT NOT NULL, `accountHolder` TEXT NOT NULL, `accountIdentifier` TEXT NOT NULL, `ifscCode` TEXT NOT NULL, `status` TEXT NOT NULL, `adminNote` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `processedAtMillis` INTEGER, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `password_reset_requests` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `identifier` TEXT NOT NULL, `status` TEXT NOT NULL, `assignedPassword` TEXT NOT NULL, `deliveryChannel` TEXT NOT NULL, `createdAtMillis` INTEGER NOT NULL, `resolvedAtMillis` INTEGER, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_control_config` (`id` INTEGER NOT NULL, `announcementText` TEXT NOT NULL, `maintenanceMode` INTEGER NOT NULL DEFAULT 0, `maintenanceMessage` TEXT NOT NULL DEFAULT '', `enableRegistration` INTEGER NOT NULL DEFAULT 1, `enableDeposits` INTEGER NOT NULL DEFAULT 1, `enableWithdrawals` INTEGER NOT NULL DEFAULT 1, `enableNumberBetting` INTEGER NOT NULL DEFAULT 1, `firstDepositBonusAmount` REAL NOT NULL, `minBetAmount` REAL NOT NULL DEFAULT 1.0, `greenRedMultiplier` REAL NOT NULL, `violetMultiplier` REAL NOT NULL, `numberMultiplier` REAL NOT NULL DEFAULT 9.0, `dragonTigerMultiplier` REAL NOT NULL, `platformFeeRate` REAL NOT NULL, `gatewayName` TEXT NOT NULL DEFAULT 'Kelywin Dynamic UPI Gateway', `gatewayApiUrl` TEXT NOT NULL DEFAULT '', `merchantKeyId` TEXT NOT NULL DEFAULT '', `upiId` TEXT NOT NULL, `merchantName` TEXT NOT NULL, `qrLabel` TEXT NOT NULL, `minDeposit` INTEGER NOT NULL, `maxDeposit` INTEGER NOT NULL, `apiBaseUrl` TEXT NOT NULL, `telegramWebhookUrl` TEXT NOT NULL, `emailWebhookUrl` TEXT NOT NULL, `smsWebhookUrl` TEXT NOT NULL, PRIMARY KEY(`id`))"
            )

            addColumnIfMissing(db, "users", "kwId", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "users", "encryptedPassword", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "users", "secretCode", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "users", "secretCodeHash", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "users", "hasReceivedFirstDepositBonus", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "users", "isBlocked", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "users", "lastLoginAtMillis", "INTEGER NOT NULL DEFAULT 0")

            addColumnIfMissing(db, "period_history", "winningNumber", "INTEGER NOT NULL DEFAULT 7")
            addColumnIfMissing(db, "period_history", "controlModeUsed", "TEXT NOT NULL DEFAULT 'RANDOM'")

            addColumnIfMissing(db, "room_control_config", "fallbackMode", "TEXT NOT NULL DEFAULT 'RANDOM'")
            addColumnIfMissing(db, "room_control_config", "xinPattern", "TEXT NOT NULL DEFAULT 'TWO_HIGH_FOUR_LOW'")
            addColumnIfMissing(db, "room_control_config", "xinAnchorPeriodIndex", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "room_control_config", "manualNumber", "INTEGER NOT NULL DEFAULT 7")
            addColumnIfMissing(db, "room_control_config", "manualTargetPeriodId", "TEXT")

            addColumnIfMissing(db, "round_admin_logs", "winningNumber", "INTEGER NOT NULL DEFAULT 7")

            addColumnIfMissing(db, "deposit_requests", "orderId", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "deposit_requests", "gatewayName", "TEXT NOT NULL DEFAULT 'Kelywin Dynamic UPI Gateway'")

            addColumnIfMissing(db, "app_control_config", "maintenanceMode", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "app_control_config", "maintenanceMessage", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "app_control_config", "enableRegistration", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "app_control_config", "enableDeposits", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "app_control_config", "enableWithdrawals", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "app_control_config", "enableNumberBetting", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "app_control_config", "minBetAmount", "REAL NOT NULL DEFAULT 1.0")
            addColumnIfMissing(db, "app_control_config", "numberMultiplier", "REAL NOT NULL DEFAULT 9.0")
            addColumnIfMissing(db, "app_control_config", "gatewayName", "TEXT NOT NULL DEFAULT 'Kelywin Dynamic UPI Gateway'")
            addColumnIfMissing(db, "app_control_config", "gatewayApiUrl", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "app_control_config", "merchantKeyId", "TEXT NOT NULL DEFAULT ''")
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureLatestTablesAndColumns(db)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureLatestTablesAndColumns(db)
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureLatestTablesAndColumns(db)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureLatestTablesAndColumns(db)
            }
        }

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureLatestTablesAndColumns(db)
            }
        }

        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                ensureLatestTablesAndColumns(db)
            }
        }

        fun getInstance(context: Context): KelywinDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KelywinDatabase::class.java,
                    "kelywin_user_panel.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_1_3,
                        MIGRATION_3_4,
                        MIGRATION_1_4,
                        MIGRATION_2_4
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
