package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAtMillis DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE UPPER(kwId) = UPPER(:kwId) LIMIT 1")
    suspend fun getUserByKwId(kwId: String): UserEntity?

    @Query("SELECT * FROM users WHERE countryCode = :countryCode AND phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByPhone(countryCode: String, phoneNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByPhoneDigits(phoneNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("UPDATE users SET balance = :newBalance WHERE userId = :userId")
    suspend fun updateBalance(userId: String, newBalance: Double)

    @Query("UPDATE users SET hasReceivedFirstDepositBonus = :received WHERE userId = :userId")
    suspend fun updateFirstDepositBonusFlag(userId: String, received: Boolean)

    @Query("UPDATE users SET isBlocked = :blocked WHERE userId = :userId")
    suspend fun updateBlockedStatus(userId: String, blocked: Boolean)

    @Query("UPDATE users SET passwordHash = :newPasswordHash WHERE userId = :userId")
    suspend fun updatePasswordHash(userId: String, newPasswordHash: String)

    @Query("UPDATE users SET passwordHash = :newPasswordHash, encryptedPassword = :encryptedPassword WHERE userId = :userId")
    suspend fun updatePasswordCredentials(userId: String, newPasswordHash: String, encryptedPassword: String)

    @Query("UPDATE users SET kwId = :kwId, secretCode = :secretCode, secretCodeHash = :secretCodeHash, encryptedPassword = :encryptedPassword WHERE userId = :userId")
    suspend fun updateSecretRecovery(
        userId: String,
        kwId: String,
        secretCode: String,
        secretCodeHash: String,
        encryptedPassword: String
    )

    @Query("UPDATE users SET lastLoginAtMillis = :timestamp WHERE userId = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long)

    @Query("SELECT * FROM users ORDER BY createdAtMillis DESC")
    suspend fun getAllUsersSnapshot(): List<UserEntity>
}

@Dao
interface PayoutMethodDao {
    @Query("SELECT * FROM payout_methods WHERE userId = :userId ORDER BY isDefault DESC, updatedAtMillis DESC")
    fun observePayoutMethods(userId: String): Flow<List<PayoutMethodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(method: PayoutMethodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(methods: List<PayoutMethodEntity>)

    @Query("DELETE FROM payout_methods WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Transaction
    suspend fun replaceForUser(userId: String, methods: List<PayoutMethodEntity>) {
        deleteAllForUser(userId)
        if (methods.isNotEmpty()) upsertAll(methods)
    }

    @Query("UPDATE payout_methods SET isDefault = 0 WHERE userId = :userId")
    suspend fun clearDefaults(userId: String)

    @Query("UPDATE payout_methods SET isDefault = 1 WHERE userId = :userId AND id = :id")
    suspend fun setDefault(userId: String, id: String)

    @Query("DELETE FROM payout_methods WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)
}

@Dao
interface WalletTransactionDao {
    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeTransactions(userId: String): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY createdAtMillis DESC")
    fun observeAllTransactions(): Flow<List<WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: WalletTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WalletTransactionEntity>)

    @Query("UPDATE wallet_transactions SET status = :status, subtitle = :subtitle WHERE id = :txId")
    suspend fun updateStatus(txId: String, status: String, subtitle: String)
}

@Dao
interface PeriodHistoryDao {
    @Query("SELECT * FROM period_history WHERE modeId = :modeId ORDER BY periodId DESC LIMIT :limit")
    fun observeHistory(modeId: String, limit: Int = 60): Flow<List<PeriodHistoryEntity>>

    @Query("SELECT COUNT(*) FROM period_history WHERE modeId = :modeId")
    suspend fun countForMode(modeId: String): Int

    @Query("SELECT * FROM period_history WHERE modeId = :modeId AND periodId = :periodId LIMIT 1")
    suspend fun getByPeriod(modeId: String, periodId: String): PeriodHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PeriodHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PeriodHistoryEntity)
}

@Dao
interface UserBetDao {
    @Query("SELECT * FROM user_bets ORDER BY createdAtMillis DESC")
    fun observeAllBets(): Flow<List<UserBetEntity>>

    @Query("SELECT * FROM user_bets WHERE userId = :userId ORDER BY createdAtMillis DESC LIMIT 100")
    fun observeUserBets(userId: String): Flow<List<UserBetEntity>>

    @Query("SELECT * FROM user_bets WHERE userId = :userId AND modeId = :modeId AND periodId = :periodId ORDER BY createdAtMillis DESC")
    fun observeRoundBets(userId: String, modeId: String, periodId: String): Flow<List<UserBetEntity>>

    @Query("SELECT * FROM user_bets WHERE modeId = :modeId AND periodId = :periodId")
    fun observeAllRoundBets(modeId: String, periodId: String): Flow<List<UserBetEntity>>

    @Query("SELECT * FROM user_bets WHERE modeId = :modeId AND periodId = :periodId AND status = 'PENDING'")
    suspend fun getPendingBetsForPeriod(modeId: String, periodId: String): List<UserBetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bet: UserBetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(bets: List<UserBetEntity>)
}

@Dao
interface RoomControlDao {
    @Query("SELECT * FROM room_control_config")
    fun observeAllConfigs(): Flow<List<RoomControlConfigEntity>>

    @Query("SELECT * FROM room_control_config WHERE modeId = :modeId LIMIT 1")
    suspend fun getConfig(modeId: String): RoomControlConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: RoomControlConfigEntity)
}

@Dao
interface RoundAdminLogDao {
    @Query("SELECT * FROM round_admin_logs ORDER BY settledAtMillis DESC LIMIT 100")
    fun observeLogs(): Flow<List<RoundAdminLogEntity>>

    @Query("SELECT * FROM round_admin_logs WHERE modeId = :modeId ORDER BY settledAtMillis DESC LIMIT 60")
    fun observeLogsForMode(modeId: String): Flow<List<RoundAdminLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: RoundAdminLogEntity)
}

@Dao
interface DepositRequestDao {
    @Query("SELECT * FROM deposit_requests ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<DepositRequestEntity>>

    @Query("SELECT * FROM deposit_requests WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeForUser(userId: String): Flow<List<DepositRequestEntity>>

    @Query("SELECT * FROM deposit_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DepositRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(req: DepositRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DepositRequestEntity>)
}

@Dao
interface WithdrawalRequestDao {
    @Query("SELECT * FROM withdrawal_requests ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<WithdrawalRequestEntity>>

    @Query("SELECT * FROM withdrawal_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WithdrawalRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(req: WithdrawalRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WithdrawalRequestEntity>)
}

@Dao
interface PasswordResetRequestDao {
    @Query("SELECT * FROM password_reset_requests ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<PasswordResetRequestEntity>>

    @Query("SELECT * FROM password_reset_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PasswordResetRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(req: PasswordResetRequestEntity)
}

@Dao
interface AppControlConfigDao {
    @Query("SELECT * FROM app_control_config WHERE id = 1 LIMIT 1")
    fun observeConfig(): Flow<AppControlConfigEntity?>

    @Query("SELECT * FROM app_control_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): AppControlConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: AppControlConfigEntity)
}
