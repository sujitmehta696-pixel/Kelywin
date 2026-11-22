package com.example.services

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AppControlConfigDao
import com.example.data.local.AppControlConfigEntity
import com.example.data.local.DepositRequestDao
import com.example.data.local.DepositRequestEntity
import com.example.data.local.PayoutMethodDao
import com.example.data.local.PayoutMethodEntity
import com.example.data.local.PeriodHistoryDao
import com.example.data.local.PeriodHistoryEntity
import com.example.data.local.RoomControlConfigEntity
import com.example.data.local.RoomControlDao
import com.example.data.local.UserBetDao
import com.example.data.local.UserBetEntity
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.local.WalletTransactionDao
import com.example.data.local.WalletTransactionEntity
import com.example.data.local.WithdrawalRequestDao
import com.example.data.local.WithdrawalRequestEntity
import com.example.shared.firebase.FirestoreCollections
import com.example.shared.firebase.FirestoreErrorMapper
import com.example.shared.firebase.FirestoreMappers
import com.example.shared.firebase.FirestoreProvider
import com.example.shared.model.BetSelectionType
import com.example.shared.model.BetStatus
import com.example.shared.model.DepositRequest
import com.example.shared.model.GameMode
import com.example.shared.model.GameRules
import com.example.shared.model.PasswordResetRequest
import com.example.shared.model.PayoutMethod
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.RoundResultRecord
import com.example.shared.model.RoundSettlementResult
import com.example.shared.model.TransactionCategory
import com.example.shared.model.TransactionStatus
import com.example.shared.model.UserBet
import com.example.shared.model.WalletTransaction
import com.example.shared.model.WithdrawalRequest
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserFirebaseRepository(
    context: Context,
    private val userDao: UserDao,
    private val payoutMethodDao: PayoutMethodDao,
    private val walletTransactionDao: WalletTransactionDao,
    private val periodHistoryDao: PeriodHistoryDao,
    private val userBetDao: UserBetDao,
    private val roomControlDao: RoomControlDao,
    private val depositRequestDao: DepositRequestDao,
    private val withdrawalRequestDao: WithdrawalRequestDao,
    private val appControlConfigDao: AppControlConfigDao
) {
    private val db: FirebaseFirestore = FirestoreProvider.getDatabase(context, R.string.firestore_database_id)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _firebaseUserFlow = MutableStateFlow(auth.currentUser)
    val firebaseUserFlow: StateFlow<FirebaseUser?> = _firebaseUserFlow.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _firebaseUserFlow.value = firebaseAuth.currentUser
    }

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var syncedUid: String? = null

    init {
        auth.addAuthStateListener(authListener)
    }

    fun currentFirebaseUid(): String? = auth.currentUser?.uid

    fun startSyncForUser(firebaseUid: String) {
        if (syncedUid == firebaseUid && activeListeners.isNotEmpty()) return
        stopSync()
        syncedUid = firebaseUid

        // 1. Global app config (maintenance, feature flags, multipliers, dynamic UPI gateway)
        activeListeners += db.collection(FirestoreCollections.APP_CONFIG)
            .document(FirestoreCollections.GLOBAL_CONFIG_DOC)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "app_config listen failed: ${err.message}")
                    return@addSnapshotListener
                }
                val cfg = snap?.let { FirestoreMappers.appConfigFromSnapshot(it) } ?: return@addSnapshotListener
                ioScope.launch {
                    appControlConfigDao.upsert(
                        AppControlConfigEntity(
                            id = 1,
                            announcementText = cfg.announcementText,
                            maintenanceMode = cfg.maintenanceMode,
                            maintenanceMessage = cfg.maintenanceMessage,
                            enableRegistration = cfg.enableRegistration,
                            enableDeposits = cfg.enableDeposits,
                            enableWithdrawals = cfg.enableWithdrawals,
                            enableNumberBetting = cfg.enableNumberBetting,
                            firstDepositBonusAmount = cfg.firstDepositBonusAmount,
                            minBetAmount = cfg.minBetAmount,
                            greenRedMultiplier = cfg.multipliers.greenRedMultiplier,
                            violetMultiplier = cfg.multipliers.violetMultiplier,
                            numberMultiplier = cfg.multipliers.numberMultiplier,
                            dragonTigerMultiplier = cfg.multipliers.dragonTigerMultiplier,
                            platformFeeRate = cfg.multipliers.platformFeeRate,
                            gatewayName = cfg.upiQrConfig.gatewayName,
                            gatewayApiUrl = cfg.upiQrConfig.gatewayApiUrl,
                            merchantKeyId = cfg.upiQrConfig.merchantKeyId,
                            upiId = cfg.upiQrConfig.merchantUpiPool,
                            merchantName = cfg.upiQrConfig.merchantName,
                            qrLabel = cfg.upiQrConfig.qrLabel,
                            minDeposit = cfg.upiQrConfig.minDeposit,
                            maxDeposit = cfg.upiQrConfig.maxDeposit,
                            apiBaseUrl = cfg.apiBaseUrl,
                            telegramWebhookUrl = cfg.telegramWebhookUrl,
                            emailWebhookUrl = cfg.emailWebhookUrl,
                            smsWebhookUrl = cfg.smsWebhookUrl
                        )
                    )
                }
            }

        // 2. Room controls (read-only for users, used only if offline fallback is needed)
        activeListeners += db.collection(FirestoreCollections.ROOM_CONTROLS)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                ioScope.launch {
                    snap.documents.mapNotNull { FirestoreMappers.roomControlFromSnapshot(it) }
                        .forEach { rc ->
                            roomControlDao.upsert(
                                RoomControlConfigEntity(
                                    modeId = rc.modeId,
                                    controlMode = rc.controlMode.name,
                                    fallbackMode = rc.fallbackMode.name,
                                    xinPattern = rc.xinPattern.name,
                                    xinAnchorPeriodIndex = rc.xinAnchorPeriodIndex,
                                    manualNumber = rc.manualNumber,
                                    manualTargetPeriodId = rc.manualTargetPeriodId
                                )
                            )
                        }
                }
            }

        // 3. Authoritative settled round results from Admin APK
        activeListeners += db.collection(FirestoreCollections.ROUND_RESULTS)
            .orderBy("settledAt", Query.Direction.DESCENDING)
            .limit(80)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val items = snap.documents.mapNotNull { FirestoreMappers.roundResultFromSnapshot(it) }
                    .map { rec ->
                        PeriodHistoryEntity(
                            periodId = rec.periodId,
                            modeId = rec.modeId,
                            winningNumber = rec.winningNumber,
                            winningColor = rec.winningColor.name,
                            winningSide = rec.winningSide.name,
                            controlModeUsed = rec.controlModeUsed.name,
                            timestampMillis = rec.settledAtMillis
                        )
                    }
                if (items.isNotEmpty()) {
                    ioScope.launch {
                        periodHistoryDao.insertAll(items)
                    }
                }
            }

        // 4. User profile document
        val userRef = db.collection(FirestoreCollections.USERS).document(firebaseUid)
        activeListeners += userRef.addSnapshotListener { snap, err ->
            if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
            val entity = userEntityFromSnapshot(snap) ?: return@addSnapshotListener
            ioScope.launch {
                userDao.upsertUser(entity)
            }
        }

        // 5. Payout methods subcollection
        activeListeners += userRef.collection(FirestoreCollections.PAYOUT_METHODS)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val entities = snap.documents.mapNotNull { FirestoreMappers.payoutMethodFromSnapshot(it) }
                    .map { m ->
                        PayoutMethodEntity(
                            id = m.id,
                            userId = m.userId,
                            type = m.type.name,
                            label = m.label,
                            accountHolder = m.accountHolder,
                            accountIdentifier = m.accountIdentifier,
                            ifscCode = m.ifscCode,
                            isDefault = m.isDefault,
                            updatedAtMillis = m.updatedAtMillis
                        )
                    }
                ioScope.launch {
                    payoutMethodDao.replaceForUser(firebaseUid, entities)
                }
            }

        // 6. Wallet transactions subcollection
        activeListeners += userRef.collection(FirestoreCollections.TRANSACTIONS)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val entities = snap.documents.mapNotNull { FirestoreMappers.transactionFromSnapshot(it) }
                    .map { tx ->
                        WalletTransactionEntity(
                            id = tx.id,
                            userId = tx.userId,
                            category = tx.category.name,
                            title = tx.title,
                            subtitle = tx.subtitle,
                            amount = tx.amount,
                            status = tx.status.name,
                            createdAtMillis = tx.createdAtMillis
                        )
                    }
                if (entities.isNotEmpty()) {
                    ioScope.launch {
                        walletTransactionDao.upsertAll(entities)
                    }
                }
            }

        // 7. User bets
        activeListeners += db.collection(FirestoreCollections.BETS)
            .whereEqualTo("userId", firebaseUid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val entities = snap.documents.mapNotNull { FirestoreMappers.betFromSnapshot(it) }
                    .map { b ->
                        UserBetEntity(
                            id = b.id,
                            userId = b.userId,
                            periodId = b.periodId,
                            modeId = b.modeId,
                            selectionType = b.selectionType.name,
                            selectionValue = b.selectionValue,
                            baseStake = b.baseStake,
                            multiplier = b.multiplier,
                            totalStake = b.totalStake,
                            status = b.status.name,
                            payout = b.payout,
                            createdAtMillis = b.createdAtMillis
                        )
                    }
                if (entities.isNotEmpty()) {
                    ioScope.launch {
                        userBetDao.updateAll(entities)
                    }
                }
            }

        // 8. User deposit requests
        activeListeners += db.collection(FirestoreCollections.DEPOSITS)
            .whereEqualTo("userId", firebaseUid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val entities = snap.documents.mapNotNull { FirestoreMappers.depositFromSnapshot(it) }
                    .map { r ->
                        DepositRequestEntity(
                            id = r.id,
                            orderId = r.orderId,
                            userId = r.userId,
                            userPhone = r.userPhone,
                            amount = r.amount,
                            utrNumber = r.utrNumber,
                            upiIdUsed = r.upiIdUsed,
                            gatewayName = r.gatewayName,
                            status = r.status.name,
                            bonusApplied = r.bonusApplied,
                            adminNote = r.adminNote,
                            createdAtMillis = r.createdAtMillis,
                            processedAtMillis = r.processedAtMillis
                        )
                    }
                if (entities.isNotEmpty()) {
                    ioScope.launch {
                        depositRequestDao.upsertAll(entities)
                    }
                }
            }

        // 9. User withdrawal requests
        activeListeners += db.collection(FirestoreCollections.WITHDRAWALS)
            .whereEqualTo("userId", firebaseUid)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val entities = snap.documents.mapNotNull { FirestoreMappers.withdrawalFromSnapshot(it) }
                    .map { w ->
                        WithdrawalRequestEntity(
                            id = w.id,
                            userId = w.userId,
                            userPhone = w.userPhone,
                            amount = w.amount,
                            methodType = w.methodType.name,
                            methodLabel = w.methodLabel,
                            accountHolder = w.accountHolder,
                            accountIdentifier = w.accountIdentifier,
                            ifscCode = w.ifscCode,
                            status = w.status.name,
                            adminNote = w.adminNote,
                            createdAtMillis = w.createdAtMillis,
                            processedAtMillis = w.processedAtMillis
                        )
                    }
                if (entities.isNotEmpty()) {
                    ioScope.launch {
                        withdrawalRequestDao.upsertAll(entities)
                    }
                }
            }
    }

    fun stopSync() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        syncedUid = null
    }

    suspend fun fetchUserProfileEntity(uid: String): Result<UserEntity?> {
        return runCatching {
            val snap = db.collection(FirestoreCollections.USERS).document(uid).get().await()
            val entity = userEntityFromSnapshot(snap)
            if (entity != null) {
                userDao.upsertUser(entity)
            }
            entity
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun createUserProfile(user: UserEntity): Result<Unit> {
        return runCatching {
            val payload = FirestoreMappers.userCreateMap(
                userId = user.userId,
                kwId = user.kwId,
                countryCode = user.countryCode,
                phoneNumber = user.phoneNumber,
                email = user.email,
                passwordHash = user.passwordHash,
                encryptedPassword = user.encryptedPassword,
                secretCode = user.secretCode,
                secretCodeHash = user.secretCodeHash,
                inviteCode = user.inviteCode,
                lastLoginAtMillis = user.lastLoginAtMillis
            )
            db.collection(FirestoreCollections.USERS)
                .document(user.userId)
                .set(payload)
                .await()
            Unit
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun updateLastLogin(uid: String, timestampMillis: Long): Result<Unit> {
        return runCatching {
            db.collection(FirestoreCollections.USERS)
                .document(uid)
                .update(
                    mapOf(
                        "lastLoginAtMillis" to timestampMillis,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Unit
        }
    }

    suspend fun updateSecretRecoveryFields(
        uid: String,
        secretCode: String,
        secretCodeHash: String,
        encryptedPassword: String
    ): Result<Unit> {
        return runCatching {
            db.collection(FirestoreCollections.USERS)
                .document(uid)
                .update(
                    mapOf(
                        "secretCode" to secretCode,
                        "secretCodeHash" to secretCodeHash,
                        "encryptedPassword" to encryptedPassword,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Unit
        }
    }

    suspend fun updatePasswordCredentials(
        uid: String,
        passwordHash: String,
        encryptedPassword: String
    ): Result<Unit> {
        return runCatching {
            db.collection(FirestoreCollections.USERS)
                .document(uid)
                .update(
                    mapOf(
                        "passwordHash" to passwordHash,
                        "encryptedPassword" to encryptedPassword,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Unit
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun updateEmail(uid: String, email: String): Result<Unit> {
        return runCatching {
            db.collection(FirestoreCollections.USERS)
                .document(uid)
                .update(
                    mapOf(
                        "email" to email,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Unit
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun savePayoutMethod(uid: String, method: PayoutMethod, allMethodsInUser: List<PayoutMethod>): Result<Unit> {
        return runCatching {
            val col = db.collection(FirestoreCollections.USERS).document(uid)
                .collection(FirestoreCollections.PAYOUT_METHODS)
            val batch = db.batch()
            if (method.isDefault) {
                allMethodsInUser.filter { it.id != method.id && it.isDefault }.forEach { other ->
                    batch.update(
                        col.document(other.id),
                        mapOf("isDefault" to false, "updatedAt" to FieldValue.serverTimestamp())
                    )
                }
            }
            val ref = col.document(method.id)
            val existing = ref.get().await()
            if (existing.exists()) {
                batch.update(
                    ref,
                    mapOf(
                        "userId" to uid,
                        "type" to method.type.name,
                        "label" to method.label,
                        "accountHolder" to method.accountHolder,
                        "accountIdentifier" to method.accountIdentifier,
                        "ifscCode" to method.ifscCode,
                        "isDefault" to method.isDefault,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(ref, FirestoreMappers.payoutMethodToCreateMap(method))
            }
            batch.commit().await()
            Unit
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun selectDefaultPayoutMethod(uid: String, methodId: String, allIds: List<String>): Result<Unit> {
        return runCatching {
            val col = db.collection(FirestoreCollections.USERS).document(uid)
                .collection(FirestoreCollections.PAYOUT_METHODS)
            val batch = db.batch()
            allIds.forEach { id ->
                batch.update(
                    col.document(id),
                    mapOf("isDefault" to (id == methodId), "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
            Unit
        }
    }

    suspend fun deletePayoutMethod(uid: String, methodId: String): Result<Unit> {
        return runCatching {
            db.collection(FirestoreCollections.USERS).document(uid)
                .collection(FirestoreCollections.PAYOUT_METHODS)
                .document(methodId)
                .delete()
                .await()
            Unit
        }
    }

    suspend fun placeBetCloud(
        uid: String,
        bet: UserBet,
        tx: WalletTransaction
    ): Result<Double> {
        return runCatching {
            val userRef = db.collection(FirestoreCollections.USERS).document(uid)
            val betRef = db.collection(FirestoreCollections.BETS).document(bet.id)
            val txRef = userRef.collection(FirestoreCollections.TRANSACTIONS).document(tx.id)

            db.runTransaction { txn ->
                val userSnap = txn.get(userRef)
                if (!userSnap.exists()) {
                    throw IllegalStateException("Cloud profile not found. Please log in again.")
                }
                val blocked = userSnap.getBoolean("blocked") ?: false
                if (blocked) {
                    throw IllegalStateException("Your account is blocked. Contact Kelywin support.")
                }
                val currentBalance = userSnap.getDouble("balance") ?: 0.0
                if (currentBalance + 0.0001 < bet.totalStake) {
                    throw IllegalStateException("Insufficient cloud balance (₹${String.format(java.util.Locale.US, "%.2f", currentBalance)}).")
                }
                val newBalance = (currentBalance - bet.totalStake).coerceAtLeast(0.0)
                txn.update(
                    userRef,
                    mapOf(
                        "balance" to newBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                txn.set(betRef, FirestoreMappers.betToCreateMap(bet))
                txn.set(txRef, FirestoreMappers.transactionToCreateMap(tx))
                newBalance
            }.await()
        }.recoverCatching { err ->
            if (err is IllegalStateException) throw err
            throw IllegalStateException(FirestoreErrorMapper.userMessage(err), err)
        }
    }

    suspend fun submitDepositCloud(req: DepositRequest, tx: WalletTransaction): Result<Unit> {
        return runCatching {
            val depRef = db.collection(FirestoreCollections.DEPOSITS).document(req.id)
            val txRef = db.collection(FirestoreCollections.USERS)
                .document(req.userId)
                .collection(FirestoreCollections.TRANSACTIONS)
                .document(tx.id)
            val batch = db.batch()
            batch.set(depRef, FirestoreMappers.depositToCreateMap(req))
            batch.set(txRef, FirestoreMappers.transactionToCreateMap(tx))
            batch.commit().await()
            Unit
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun submitWithdrawalCloud(
        uid: String,
        req: WithdrawalRequest,
        tx: WalletTransaction
    ): Result<Double> {
        return runCatching {
            val userRef = db.collection(FirestoreCollections.USERS).document(uid)
            val wdRef = db.collection(FirestoreCollections.WITHDRAWALS).document(req.id)
            val txRef = userRef.collection(FirestoreCollections.TRANSACTIONS).document(tx.id)

            db.runTransaction { txn ->
                val userSnap = txn.get(userRef)
                if (!userSnap.exists()) throw IllegalStateException("User session not found.")
                if (userSnap.getBoolean("blocked") == true) {
                    throw IllegalStateException("Your account is blocked.")
                }
                val currentBalance = userSnap.getDouble("balance") ?: 0.0
                if (req.amount > currentBalance + 0.0001) {
                    throw IllegalStateException("Insufficient balance for withdrawal.")
                }
                val newBalance = (currentBalance - req.amount).coerceAtLeast(0.0)
                txn.update(
                    userRef,
                    mapOf(
                        "balance" to newBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                txn.set(wdRef, FirestoreMappers.withdrawalToCreateMap(req))
                txn.set(txRef, FirestoreMappers.transactionToCreateMap(tx))
                newBalance
            }.await()
        }.recoverCatching { err ->
            if (err is IllegalStateException) throw err
            throw IllegalStateException(FirestoreErrorMapper.userMessage(err), err)
        }
    }

    suspend fun submitPasswordResetRequest(req: PasswordResetRequest): Result<Unit> {
        return runCatching {
            db.collection(FirestoreCollections.PASSWORD_RESETS)
                .document(req.id)
                .set(FirestoreMappers.passwordResetToCreateMap(req))
                .await()
            Unit
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun fetchRoundResultRecord(modeId: String, periodId: String): RoundResultRecord? {
        return runCatching {
            val snap = db.collection(FirestoreCollections.ROUND_RESULTS)
                .document("${modeId}_${periodId}")
                .get()
                .await()
            FirestoreMappers.roundResultFromSnapshot(snap)
        }.getOrNull()
    }

    /**
     * Settles the signed-in user's PENDING bets in Firestore for [periodId].
     * Uses an atomic transaction & deterministic WIN transaction ID `WIN_${mode.id}_${periodId}_${uid.take(10)}`
     * so concurrent settlement from Admin APK never double-credits the user.
     */
    suspend fun settleUserBetsInCloud(
        uid: String,
        mode: GameMode,
        periodId: String,
        winningNumber: Int,
        multipliers: PayoutMultipliers
    ): RoundSettlementResult? {
        return runCatching {
            val snap = db.collection(FirestoreCollections.BETS)
                .whereEqualTo("userId", uid)
                .whereEqualTo("modeId", mode.id)
                .whereEqualTo("periodId", periodId)
                .get()
                .await()

            if (snap.isEmpty) return@runCatching null

            val userBetsAll = snap.documents.mapNotNull { FirestoreMappers.betFromSnapshot(it) }
            val pendingBets = userBetsAll.filter { it.status == BetStatus.PENDING }

            val side = GameRules.sideForNumber(winningNumber)
            val colors = GameRules.colorsForNumber(winningNumber)
            val primaryColor = colors.firstOrNull() ?: com.example.shared.model.BetColor.GREEN

            if (pendingBets.isEmpty()) {
                // Already settled by Admin APK; still return settlement summary so User sees Win/Loss popup if desired
                val totalStake = userBetsAll.sumOf { it.totalStake }
                val totalPayout = userBetsAll.sumOf { it.payout }
                return@runCatching RoundSettlementResult(
                    mode = mode,
                    periodId = periodId,
                    winningNumber = winningNumber,
                    winningColors = colors,
                    winningSide = side,
                    settledBets = userBetsAll,
                    totalStake = totalStake,
                    totalPayout = totalPayout
                )
            }

            val userRef = db.collection(FirestoreCollections.USERS).document(uid)
            val winTxId = "WIN_${mode.id}_${periodId}_${uid.take(10)}"
            val winTxRef = userRef.collection(FirestoreCollections.TRANSACTIONS).document(winTxId)
            val now = System.currentTimeMillis()

            val settledList = db.runTransaction { txn ->
                val userSnap = txn.get(userRef)
                val winTxExists = txn.get(winTxRef).exists()
                val betSnaps = pendingBets.map { b ->
                    b to txn.get(db.collection(FirestoreCollections.BETS).document(b.id))
                }

                var creditTotal = 0.0
                val resultList = mutableListOf<UserBet>()

                for ((bet, bSnap) in betSnaps) {
                    val liveStatus = bSnap.getString("status")
                    if (liveStatus != BetStatus.PENDING.name) {
                        FirestoreMappers.betFromSnapshot(bSnap)?.let { resultList.add(it) }
                        continue
                    }
                    val payout = GameRules.calculatePayout(
                        selectionType = bet.selectionType,
                        selectionValue = bet.selectionValue,
                        totalStake = bet.totalStake,
                        winningNumber = winningNumber,
                        multipliers = multipliers
                    )
                    val newStatus = if (payout > 0.0) BetStatus.WON else BetStatus.LOST
                    txn.update(
                        bSnap.reference,
                        mapOf(
                            "status" to newStatus.name,
                            "payout" to payout,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (payout > 0.0) creditTotal += payout
                    resultList.add(bet.copy(status = newStatus, payout = payout))
                }

                if (creditTotal > 0.0 && !winTxExists && userSnap.exists()) {
                    val curBal = userSnap.getDouble("balance") ?: 0.0
                    txn.update(
                        userRef,
                        mapOf(
                            "balance" to (curBal + creditTotal),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val winTx = WalletTransaction(
                        id = winTxId,
                        userId = uid,
                        category = TransactionCategory.WIN,
                        title = "${mode.shortLabel} Win · #$winningNumber (${primaryColor.label})",
                        subtitle = "Period $periodId (${side.label})",
                        amount = creditTotal,
                        status = TransactionStatus.COMPLETED,
                        createdAtMillis = now
                    )
                    txn.set(winTxRef, FirestoreMappers.transactionToCreateMap(winTx))
                }
                resultList
            }.await()

            RoundSettlementResult(
                mode = mode,
                periodId = periodId,
                winningNumber = winningNumber,
                winningColors = colors,
                winningSide = side,
                settledBets = settledList,
                totalStake = settledList.sumOf { it.totalStake },
                totalPayout = settledList.sumOf { it.payout }
            )
        }.getOrNull()
    }

    private fun userEntityFromSnapshot(snap: DocumentSnapshot): UserEntity? {
        if (!snap.exists()) return null
        val account = FirestoreMappers.userFromSnapshot(snap) ?: return null
        val d = snap.data ?: return null
        return UserEntity(
            userId = account.userId,
            kwId = account.kwId,
            countryCode = account.countryCode,
            phoneNumber = account.phoneNumber,
            email = account.email,
            passwordHash = (d["passwordHash"] as? String) ?: "",
            encryptedPassword = (d["encryptedPassword"] as? String) ?: "",
            secretCode = account.secretCode,
            secretCodeHash = (d["secretCodeHash"] as? String) ?: "",
            inviteCode = account.inviteCode,
            balance = account.balance,
            hasReceivedFirstDepositBonus = account.hasReceivedFirstDepositBonus,
            isBlocked = account.isBlocked,
            createdAtMillis = account.createdAtMillis,
            lastLoginAtMillis = account.lastLoginAtMillis
        )
    }

    companion object {
        private const val TAG = "UserFirebaseRepo"
    }
}
