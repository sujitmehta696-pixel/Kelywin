package com.example.adminapp.data

import android.content.Context
import android.util.Log
import com.example.shared.firebase.FirestoreCollections
import com.example.shared.firebase.FirestoreErrorMapper
import com.example.shared.firebase.FirestoreMappers
import com.example.shared.firebase.FirestoreProvider
import com.example.shared.model.AdminGatewaySecret
import com.example.shared.model.AdminMember
import com.example.shared.model.AppControlConfig
import com.example.shared.model.BetStatus
import com.example.shared.model.DepositRequest
import com.example.shared.model.GameControlMode
import com.example.shared.model.GameMode
import com.example.shared.model.GameRules
import com.example.shared.model.PasswordResetRequest
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.RequestStatus
import com.example.shared.model.RoomControlConfig
import com.example.shared.model.RoundAdminLog
import com.example.shared.model.RoundResultRecord
import com.example.shared.model.TransactionCategory
import com.example.shared.model.TransactionStatus
import com.example.shared.model.UserAccount
import com.example.shared.model.UserBet
import com.example.shared.model.WalletTransaction
import com.example.shared.model.WithdrawalRequest
import com.example.shared.model.XinLogicPattern
import com.example.shared.security.SecretRecoveryCrypto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

sealed interface AdminAuthState {
    data object Loading : AdminAuthState
    data object SignedOut : AdminAuthState
    data class Unauthorized(val email: String, val uid: String) : AdminAuthState
    data class Authorized(val uid: String, val email: String) : AdminAuthState
}

class AdminFirebaseRepository(context: Context) {

    private val db: FirebaseFirestore = run {
        val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
        FirestoreProvider.getDatabase(context, resId)
    }
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow<AdminAuthState>(AdminAuthState.Loading)
    val authState: StateFlow<AdminAuthState> = _authState.asStateFlow()

    private val _appConfig = MutableStateFlow(AppControlConfig())
    val appConfig: StateFlow<AppControlConfig> = _appConfig.asStateFlow()

    private val _roomControls = MutableStateFlow(
        mapOf(
            GameMode.SEC_30.id to RoomControlConfig(modeId = GameMode.SEC_30.id),
            GameMode.MIN_1.id to RoomControlConfig(modeId = GameMode.MIN_1.id)
        )
    )
    val roomControls: StateFlow<Map<String, RoomControlConfig>> = _roomControls.asStateFlow()

    private val _users = MutableStateFlow<List<UserAccount>>(emptyList())
    val users: StateFlow<List<UserAccount>> = _users.asStateFlow()

    private val _allBets = MutableStateFlow<List<UserBet>>(emptyList())
    val allBets: StateFlow<List<UserBet>> = _allBets.asStateFlow()

    private val _deposits = MutableStateFlow<List<DepositRequest>>(emptyList())
    val deposits: StateFlow<List<DepositRequest>> = _deposits.asStateFlow()

    private val _withdrawals = MutableStateFlow<List<WithdrawalRequest>>(emptyList())
    val withdrawals: StateFlow<List<WithdrawalRequest>> = _withdrawals.asStateFlow()

    private val _passwordResets = MutableStateFlow<List<PasswordResetRequest>>(emptyList())
    val passwordResets: StateFlow<List<PasswordResetRequest>> = _passwordResets.asStateFlow()

    private val _roundLogs = MutableStateFlow<List<RoundAdminLog>>(emptyList())
    val roundLogs: StateFlow<List<RoundAdminLog>> = _roundLogs.asStateFlow()

    private val _adminMembers = MutableStateFlow<List<AdminMember>>(emptyList())
    val adminMembers: StateFlow<List<AdminMember>> = _adminMembers.asStateFlow()

    private val _famGatewaySecret = MutableStateFlow(AdminGatewaySecret())
    val famGatewaySecret: StateFlow<AdminGatewaySecret> = _famGatewaySecret.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var activeAuthorizedUid: String? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        verifyCurrentUser(firebaseAuth.currentUser)
    }

    init {
        auth.addAuthStateListener(authListener)
        verifyCurrentUser(auth.currentUser)
    }

    fun refreshAuthorization() {
        verifyCurrentUser(auth.currentUser)
    }

    private fun verifyCurrentUser(user: FirebaseUser?) {
        if (user == null) {
            stopListeners()
            _authState.value = AdminAuthState.SignedOut
            return
        }
        val uid = user.uid
        val email = user.email.orEmpty()
        _authState.value = AdminAuthState.Loading

        ioScope.launch {
            val isBootstrapOwner = email.equals(OWNER_ADMIN_EMAIL, ignoreCase = true) && user.isEmailVerified
            val adminDocRef = db.collection(FirestoreCollections.ADMINS).document(uid)

            val docExists = runCatching {
                val snap = adminDocRef.get().await()
                snap.exists() && snap.getString("role") == "ADMIN"
            }.getOrDefault(false)

            if (isBootstrapOwner && !docExists) {
                runCatching {
                    adminDocRef.set(FirestoreMappers.adminMemberToCreateMap(uid, email)).await()
                }.onFailure { err ->
                    Log.w(TAG, "Bootstrap admin record write skipped: ${err.message}")
                }
            }

            if (isBootstrapOwner || docExists) {
                _authState.value = AdminAuthState.Authorized(uid = uid, email = email)
                seedDefaultsIfMissing()
                startListeners(uid)
            } else {
                stopListeners()
                _authState.value = AdminAuthState.Unauthorized(email = email.ifBlank { uid }, uid = uid)
            }
        }
    }

    private suspend fun seedDefaultsIfMissing() {
        runCatching {
            val configRef = db.collection(FirestoreCollections.APP_CONFIG)
                .document(FirestoreCollections.GLOBAL_CONFIG_DOC)
            if (!configRef.get().await().exists()) {
                configRef.set(FirestoreMappers.appConfigToMap(AppControlConfig())).await()
            }

            GameMode.entries.forEach { mode ->
                val rcRef = db.collection(FirestoreCollections.ROOM_CONTROLS).document(mode.id)
                if (!rcRef.get().await().exists()) {
                    rcRef.set(
                        FirestoreMappers.roomControlToMap(
                            RoomControlConfig(modeId = mode.id)
                        )
                    ).await()
                }
            }
        }.onFailure { err ->
            Log.w(TAG, "Default config seeding failed: ${err.message}")
        }
    }

    private fun startListeners(uid: String) {
        if (activeAuthorizedUid == uid && activeListeners.isNotEmpty()) return
        stopListeners()
        activeAuthorizedUid = uid

        activeListeners += db.collection(FirestoreCollections.APP_CONFIG)
            .document(FirestoreCollections.GLOBAL_CONFIG_DOC)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "app_config listener error: ${err.message}")
                    return@addSnapshotListener
                }
                snap?.let { FirestoreMappers.appConfigFromSnapshot(it) }?.let { cfg ->
                    _appConfig.value = cfg
                }
            }

        activeListeners += db.collection(FirestoreCollections.ROOM_CONTROLS)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val updated = _roomControls.value.toMutableMap()
                snap.documents.mapNotNull { FirestoreMappers.roomControlFromSnapshot(it) }
                    .forEach { rc -> updated[rc.modeId] = rc }
                _roomControls.value = updated
            }

        activeListeners += db.collection(FirestoreCollections.USERS)
            .orderBy("lastLoginAtMillis", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _users.value = snap.documents.mapNotNull { FirestoreMappers.userFromSnapshot(it) }
            }

        activeListeners += db.collection(FirestoreCollections.BETS)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(400)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _allBets.value = snap.documents.mapNotNull { FirestoreMappers.betFromSnapshot(it) }
            }

        activeListeners += db.collection(FirestoreCollections.DEPOSITS)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _deposits.value = snap.documents.mapNotNull { FirestoreMappers.depositFromSnapshot(it) }
            }

        activeListeners += db.collection(FirestoreCollections.WITHDRAWALS)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(200)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _withdrawals.value = snap.documents.mapNotNull { FirestoreMappers.withdrawalFromSnapshot(it) }
            }

        activeListeners += db.collection(FirestoreCollections.PASSWORD_RESETS)
            .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _passwordResets.value = snap.documents.mapNotNull { FirestoreMappers.passwordResetFromSnapshot(it) }
            }

        activeListeners += db.collection(FirestoreCollections.ROUND_LOGS)
            .orderBy("settledAtMillis", Query.Direction.DESCENDING)
            .limit(120)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _roundLogs.value = snap.documents.mapNotNull { FirestoreMappers.roundLogFromSnapshot(it) }
            }

        activeListeners += db.collection(FirestoreCollections.ADMINS)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                _adminMembers.value = snap.documents.mapNotNull { FirestoreMappers.adminMemberFromSnapshot(it) }
            }

        activeListeners += db.collection(FirestoreCollections.ADMIN_SECRETS)
            .document(FirestoreCollections.FAMGATEWAY_SECRET_DOC)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "admin_secrets listener error: ${err.message}")
                    return@addSnapshotListener
                }
                snap?.let { FirestoreMappers.adminGatewaySecretFromSnapshot(it) }?.let { sec ->
                    _famGatewaySecret.value = sec
                }
            }
    }

    fun stopListeners() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        activeAuthorizedUid = null
    }

    suspend fun saveAppConfig(config: AppControlConfig): Result<Unit> {
        if (_authState.value !is AdminAuthState.Authorized) {
            return Result.failure(IllegalStateException("Admin authorization required."))
        }
        return runCatching {
            val sanitized = config.copy(
                minBetAmount = config.minBetAmount.coerceIn(1.0, 10000.0),
                firstDepositBonusAmount = config.firstDepositBonusAmount.coerceIn(0.0, 100000.0),
                multipliers = PayoutMultipliers(
                    greenRedMultiplier = config.multipliers.greenRedMultiplier.coerceIn(1.0, 20.0),
                    violetMultiplier = config.multipliers.violetMultiplier.coerceIn(1.0, 50.0),
                    numberMultiplier = config.multipliers.numberMultiplier.coerceIn(1.0, 100.0),
                    dragonTigerMultiplier = config.multipliers.dragonTigerMultiplier.coerceIn(1.0, 20.0),
                    platformFeeRate = config.multipliers.platformFeeRate.coerceIn(0.0, 0.5)
                ),
                upiQrConfig = config.upiQrConfig.copy(
                    minDeposit = config.upiQrConfig.minDeposit.coerceAtLeast(1),
                    maxDeposit = config.upiQrConfig.maxDeposit.coerceAtLeast(config.upiQrConfig.minDeposit.coerceAtLeast(1))
                )
            )
            db.collection(FirestoreCollections.APP_CONFIG)
                .document(FirestoreCollections.GLOBAL_CONFIG_DOC)
                .set(FirestoreMappers.appConfigToMap(sanitized))
                .await()
            _appConfig.value = sanitized
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun saveFamGatewayConfig(
        apiKey: String,
        backendUrl: String,
        enabled: Boolean
    ): Result<Unit> {
        val authUser = _authState.value as? AdminAuthState.Authorized
            ?: return Result.failure(IllegalStateException("Admin authorization required."))
        val cleanUrl = backendUrl.trim().trimEnd('/')
        if (cleanUrl.isNotBlank() &&
            !cleanUrl.startsWith("https://", ignoreCase = true) &&
            !cleanUrl.startsWith("http://", ignoreCase = true)
        ) {
            return Result.failure(IllegalArgumentException("Backend URL must start with https:// or http://"))
        }
        val cleanKey = apiKey.trim()
        if (enabled && cleanUrl.isNotBlank() && cleanKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your FamGateway Secret API Key."))
        }
        return runCatching {
            val secret = AdminGatewaySecret(
                provider = "FAMGATEWAY",
                apiKey = cleanKey,
                backendUrl = cleanUrl,
                enabled = enabled,
                updatedByUid = authUser.uid,
                updatedAtMillis = System.currentTimeMillis()
            )
            db.collection(FirestoreCollections.ADMIN_SECRETS)
                .document(FirestoreCollections.FAMGATEWAY_SECRET_DOC)
                .set(FirestoreMappers.adminGatewaySecretToMap(secret))
                .await()
            _famGatewaySecret.value = secret

            val currentCfg = _appConfig.value
            val updatedGatewayName = if (enabled && cleanUrl.isNotBlank()) {
                "FamGateway Auto UPI"
            } else {
                currentCfg.upiQrConfig.gatewayName
            }
            val nextAppConfig = currentCfg.copy(
                upiQrConfig = currentCfg.upiQrConfig.copy(
                    gatewayName = updatedGatewayName,
                    gatewayApiUrl = cleanUrl
                )
            )
            db.collection(FirestoreCollections.APP_CONFIG)
                .document(FirestoreCollections.GLOBAL_CONFIG_DOC)
                .set(FirestoreMappers.appConfigToMap(nextAppConfig))
                .await()
            _appConfig.value = nextAppConfig
            Unit
        }.recoverCatching { err ->
            if (err is IllegalArgumentException || err is IllegalStateException) throw err
            throw IllegalStateException(FirestoreErrorMapper.userMessage(err), err)
        }
    }

    suspend fun updateRoomControl(config: RoomControlConfig): Result<Unit> {
        if (_authState.value !is AdminAuthState.Authorized) {
            return Result.failure(IllegalStateException("Admin authorization required."))
        }
        return runCatching {
            val safeFallback = if (config.fallbackMode == GameControlMode.MANUAL) GameControlMode.RANDOM else config.fallbackMode
            val clean = config.copy(
                fallbackMode = safeFallback,
                xinAnchorPeriodIndex = config.xinAnchorPeriodIndex.coerceAtLeast(0),
                manualNumber = config.manualNumber.coerceIn(0, 9),
                manualTargetPeriodId = if (config.controlMode == GameControlMode.MANUAL) config.manualTargetPeriodId else null
            )
            db.collection(FirestoreCollections.ROOM_CONTROLS)
                .document(clean.modeId)
                .set(FirestoreMappers.roomControlToMap(clean))
                .await()
            val map = _roomControls.value.toMutableMap()
            map[clean.modeId] = clean
            _roomControls.value = map
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun resetXinCycleToPeriod(mode: GameMode, periodIndex: Int, pattern: XinLogicPattern): Result<Unit> {
        val current = _roomControls.value[mode.id] ?: RoomControlConfig(modeId = mode.id)
        return updateRoomControl(
            current.copy(
                controlMode = GameControlMode.XIN_LOGIC,
                xinPattern = pattern,
                xinAnchorPeriodIndex = periodIndex.coerceAtLeast(1),
                manualTargetPeriodId = null
            )
        )
    }

    suspend fun fetchPendingBetsForPeriod(modeId: String, periodId: String): List<UserBet> {
        return runCatching {
            val snap = db.collection(FirestoreCollections.BETS)
                .whereEqualTo("modeId", modeId)
                .whereEqualTo("periodId", periodId)
                .get()
                .await()
            snap.documents.mapNotNull { FirestoreMappers.betFromSnapshot(it) }
        }.getOrElse {
            _allBets.value.filter { it.modeId == modeId && it.periodId == periodId }
        }
    }

    /**
     * Publishes authoritative RoundResult + RoundAdminLog and settles all users' PENDING bets
     * atomically per user using deterministic WIN tx ID `"WIN_${mode.id}_${periodId}_${uid.take(10)}"`.
     */
    suspend fun publishRoundResultAndSettle(
        mode: GameMode,
        periodId: String,
        winningNumber: Int,
        controlModeUsed: GameControlMode,
        multipliers: PayoutMultipliers,
        allBetsInPeriod: List<UserBet>,
        autoRevertManualToFallback: RoomControlConfig? = null
    ): Result<RoundAdminLog> {
        if (_authState.value !is AdminAuthState.Authorized) {
            return Result.failure(IllegalStateException("Admin authorization required."))
        }
        return runCatching {
            val safeNum = winningNumber.coerceIn(0, 9)
            val primaryColor = GameRules.primaryColorForNumber(safeNum)
            val side = GameRules.sideForNumber(safeNum)
            val docId = "${mode.id}_${periodId}"
            val now = System.currentTimeMillis()

            // 1. Write authoritative round_results/{modeId}_{periodId} if not already settled
            val resultRef = db.collection(FirestoreCollections.ROUND_RESULTS).document(docId)
            val existingResultSnap = resultRef.get().await()
            if (!existingResultSnap.exists()) {
                val record = RoundResultRecord(
                    modeId = mode.id,
                    periodId = periodId,
                    winningNumber = safeNum,
                    winningColor = primaryColor,
                    winningSide = side,
                    controlModeUsed = controlModeUsed,
                    settledAtMillis = now
                )
                resultRef.set(FirestoreMappers.roundResultToMap(record)).await()
            } else {
                // Use already-published winning number if another admin instance settled it first
                val existingNum = (existingResultSnap.get("winningNumber") as? Number)?.toInt()?.coerceIn(0, 9) ?: safeNum
                if (existingNum != safeNum) {
                    val existingLogSnap = db.collection(FirestoreCollections.ROUND_LOGS).document(docId).get().await()
                    FirestoreMappers.roundLogFromSnapshot(existingLogSnap)?.let { return@runCatching it }
                }
            }

            // 2. Auto-revert one-shot Manual target if this closed period was targeted
            if (autoRevertManualToFallback != null) {
                val fallback = if (autoRevertManualToFallback.fallbackMode == GameControlMode.MANUAL) {
                    GameControlMode.RANDOM
                } else {
                    autoRevertManualToFallback.fallbackMode
                }
                updateRoomControl(
                    autoRevertManualToFallback.copy(
                        controlMode = fallback,
                        manualTargetPeriodId = null
                    )
                )
            }

            // 3. Settle pending bets per user inside atomic Firestore transactions
            var totalBetsAmount = 0.0
            var totalPayoutAmount = 0.0

            val betsByUser = allBetsInPeriod.groupBy { it.userId }
            for ((uid, userBets) in betsByUser) {
                totalBetsAmount += userBets.sumOf { it.totalStake }
                val pendingForUser = userBets.filter { it.status == BetStatus.PENDING }
                if (pendingForUser.isEmpty()) {
                    totalPayoutAmount += userBets.sumOf { it.payout }
                    continue
                }

                val userRef = db.collection(FirestoreCollections.USERS).document(uid)
                val winTxId = "WIN_${mode.id}_${periodId}_${uid.take(10)}"
                val winTxRef = userRef.collection(FirestoreCollections.TRANSACTIONS).document(winTxId)

                val userPayout = db.runTransaction { txn ->
                    val userSnap = txn.get(userRef)
                    val winTxExists = txn.get(winTxRef).exists()
                    val betSnaps = pendingForUser.map { b ->
                        b to txn.get(db.collection(FirestoreCollections.BETS).document(b.id))
                    }

                    var creditTotal = 0.0
                    var settledPayoutSum = 0.0

                    for ((bet, bSnap) in betSnaps) {
                        val liveStatus = bSnap.getString("status")
                        if (liveStatus != BetStatus.PENDING.name) {
                            settledPayoutSum += (bSnap.get("payout") as? Number)?.toDouble() ?: 0.0
                            continue
                        }
                        val payout = GameRules.calculatePayout(
                            selectionType = bet.selectionType,
                            selectionValue = bet.selectionValue,
                            totalStake = bet.totalStake,
                            winningNumber = safeNum,
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
                        if (payout > 0.0) {
                            creditTotal += payout
                        }
                        settledPayoutSum += payout
                    }

                    if (creditTotal > 0.0 && !winTxExists && userSnap.exists()) {
                        val curBal = (userSnap.get("balance") as? Number)?.toDouble() ?: 0.0
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
                            title = "${mode.shortLabel} Win · #$safeNum (${primaryColor.label})",
                            subtitle = "Period $periodId (${side.label})",
                            amount = creditTotal,
                            status = TransactionStatus.COMPLETED,
                            createdAtMillis = now
                        )
                        txn.set(winTxRef, FirestoreMappers.transactionToCreateMap(winTx))
                    }
                    settledPayoutSum
                }.await()

                totalPayoutAmount += userPayout
            }

            val log = RoundAdminLog(
                periodId = periodId,
                modeId = mode.id,
                controlModeUsed = controlModeUsed,
                winningNumber = safeNum,
                winningColor = primaryColor,
                winningSide = side,
                totalBetsAmount = totalBetsAmount,
                totalPayoutAmount = totalPayoutAmount,
                netProfit = totalBetsAmount - totalPayoutAmount,
                settledAtMillis = now
            )

            db.collection(FirestoreCollections.ROUND_LOGS)
                .document(docId)
                .set(FirestoreMappers.roundLogToMap(log))
                .await()

            log
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun approveDeposit(depositId: String, note: String = "Verified by Admin"): Result<Double> {
        return runCatching {
            val depRef = db.collection(FirestoreCollections.DEPOSITS).document(depositId)
            val bonusAmt = _appConfig.value.firstDepositBonusAmount.coerceAtLeast(0.0)
            val now = System.currentTimeMillis()

            db.runTransaction { txn ->
                val depSnap = txn.get(depRef)
                val dep = FirestoreMappers.depositFromSnapshot(depSnap)
                    ?: throw IllegalStateException("Deposit request not found.")
                if (dep.status != RequestStatus.PENDING) {
                    throw IllegalStateException("Deposit is already ${dep.status.label}.")
                }

                val userRef = db.collection(FirestoreCollections.USERS).document(dep.userId)
                val userSnap = txn.get(userRef)
                if (!userSnap.exists()) throw IllegalStateException("User profile not found.")

                val depTxRef = userRef.collection(FirestoreCollections.TRANSACTIONS).document(dep.id)
                val depTxSnap = txn.get(depTxRef)

                val bonusTxRef = userRef.collection(FirestoreCollections.TRANSACTIONS).document("BONUS_${dep.id}")

                val currentBalance = (userSnap.get("balance") as? Number)?.toDouble() ?: 0.0
                val alreadyGotBonus = userSnap.getBoolean("hasReceivedFirstDepositBonus") ?: false
                val appliedBonus = if (!alreadyGotBonus && bonusAmt > 0.0) bonusAmt else 0.0
                val newBalance = currentBalance + dep.amount + appliedBonus

                txn.update(
                    userRef,
                    mapOf(
                        "balance" to newBalance,
                        "hasReceivedFirstDepositBonus" to (alreadyGotBonus || appliedBonus > 0.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                txn.update(
                    depRef,
                    mapOf(
                        "status" to RequestStatus.APPROVED.name,
                        "bonusApplied" to appliedBonus,
                        "adminNote" to note.ifBlank { "Verified by Admin" },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val subtitleApproved = if (dep.utrNumber.isNotBlank()) {
                    "Order ${dep.orderId} · UTR ${dep.utrNumber} · Verified"
                } else {
                    "Order ${dep.orderId} · Approved by Admin"
                }
                if (depTxSnap.exists()) {
                    txn.update(
                        depTxRef,
                        mapOf(
                            "status" to TransactionStatus.APPROVED.name,
                            "subtitle" to subtitleApproved
                        )
                    )
                } else {
                    txn.set(
                        depTxRef,
                        FirestoreMappers.transactionToCreateMap(
                            WalletTransaction(
                                id = dep.id,
                                userId = dep.userId,
                                category = TransactionCategory.DEPOSIT,
                                title = "Gateway UPI Deposit · ₹${dep.amount.toInt()}",
                                subtitle = subtitleApproved,
                                amount = dep.amount,
                                status = TransactionStatus.APPROVED,
                                createdAtMillis = now
                            )
                        )
                    )
                }

                if (appliedBonus > 0.0) {
                    txn.set(
                        bonusTxRef,
                        FirestoreMappers.transactionToCreateMap(
                            WalletTransaction(
                                id = "BONUS_${dep.id}",
                                userId = dep.userId,
                                category = TransactionCategory.BONUS,
                                title = "First Deposit Welcome Bonus (+₹${appliedBonus.toInt()})",
                                subtitle = "Credited once on first approved UPI deposit",
                                amount = appliedBonus,
                                status = TransactionStatus.COMPLETED,
                                createdAtMillis = now + 1
                            )
                        )
                    )
                }
                appliedBonus
            }.await()
        }.recoverCatching { err ->
            if (err is IllegalStateException) throw err
            throw IllegalStateException(FirestoreErrorMapper.userMessage(err), err)
        }
    }

    suspend fun rejectDeposit(depositId: String, note: String = "UTR verification failed"): Result<Unit> {
        return runCatching {
            val depRef = db.collection(FirestoreCollections.DEPOSITS).document(depositId)
            db.runTransaction { txn ->
                val depSnap = txn.get(depRef)
                val dep = FirestoreMappers.depositFromSnapshot(depSnap)
                    ?: throw IllegalStateException("Deposit request not found.")
                if (dep.status != RequestStatus.PENDING) {
                    throw IllegalStateException("Deposit is already ${dep.status.label}.")
                }
                val userRef = db.collection(FirestoreCollections.USERS).document(dep.userId)
                val depTxRef = userRef.collection(FirestoreCollections.TRANSACTIONS).document(dep.id)
                val depTxSnap = txn.get(depTxRef)

                val reason = note.ifBlank { "UTR verification failed" }
                txn.update(
                    depRef,
                    mapOf(
                        "status" to RequestStatus.REJECTED.name,
                        "bonusApplied" to 0.0,
                        "adminNote" to reason,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (depTxSnap.exists()) {
                    txn.update(
                        depTxRef,
                        mapOf(
                            "status" to TransactionStatus.REJECTED.name,
                            "subtitle" to "Rejected: $reason"
                        )
                    )
                }
            }.await()
            Unit
        }.recoverCatching { err ->
            if (err is IllegalStateException) throw err
            throw IllegalStateException(FirestoreErrorMapper.userMessage(err), err)
        }
    }

    suspend fun approveWithdrawal(withdrawalId: String, note: String = "Paid by Admin"): Result<Unit> {
        return runCatching {
            val wdRef = db.collection(FirestoreCollections.WITHDRAWALS).document(withdrawalId)
            db.runTransaction { txn ->
                val wdSnap = txn.get(wdRef)
                val wd = FirestoreMappers.withdrawalFromSnapshot(wdSnap)
                    ?: throw IllegalStateException("Withdrawal request not found.")
                if (wd.status != RequestStatus.PENDING) {
                    throw IllegalStateException("Withdrawal is already ${wd.status.label}.")
                }
                val wdTxRef = db.collection(FirestoreCollections.USERS)
                    .document(wd.userId)
                    .collection(FirestoreCollections.TRANSACTIONS)
                    .document(wd.id)
                val wdTxSnap = txn.get(wdTxRef)
                val finalNote = note.ifBlank { "Paid by Admin" }

                txn.update(
                    wdRef,
                    mapOf(
                        "status" to RequestStatus.APPROVED.name,
                        "adminNote" to finalNote,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (wdTxSnap.exists()) {
                    txn.update(
                        wdTxRef,
                        mapOf(
                            "status" to TransactionStatus.APPROVED.name,
                            "subtitle" to "${wd.methodLabel} · ${wd.accountIdentifier} · $finalNote"
                        )
                    )
                }
            }.await()
            Unit
        }.recoverCatching { err ->
            if (err is IllegalStateException) throw err
            throw IllegalStateException(FirestoreErrorMapper.userMessage(err), err)
        }
    }

    suspend fun rejectWithdrawal(withdrawalId: String, note: String = "Rejected & refunded"): Result<Unit> {
        return runCatching {
            val wdRef = db.collection(FirestoreCollections.WITHDRAWALS).document(withdrawalId)
            val now = System.currentTimeMillis()
            db.runTransaction { txn ->
                val wdSnap = txn.get(wdRef)
                val wd = FirestoreMappers.withdrawalFromSnapshot(wdSnap)
                    ?: throw IllegalStateException("Withdrawal request not found.")
                if (wd.status != RequestStatus.PENDING) {
                    throw IllegalStateException("Withdrawal is already ${wd.status.label}.")
                }

                val userRef = db.collection(FirestoreCollections.USERS).document(wd.userId)
                val userSnap = txn.get(userRef)
                val wdTxRef = userRef.collection(FirestoreCollections.TRANSACTIONS).document(wd.id)
                val wdTxSnap = txn.get(wdTxRef)
                val refundTxRef = userRef.collection(FirestoreCollections.TRANSACTIONS).document("REFUND_${wd.id}")

                val reason = note.ifBlank { "Rejected & refunded" }
                txn.update(
                    wdRef,
                    mapOf(
                        "status" to RequestStatus.REJECTED.name,
                        "adminNote" to reason,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (wdTxSnap.exists()) {
                    txn.update(
                        wdTxRef,
                        mapOf(
                            "status" to TransactionStatus.REJECTED.name,
                            "subtitle" to "Rejected: $reason"
                        )
                    )
                }
                if (userSnap.exists()) {
                    val curBal = (userSnap.get("balance") as? Number)?.toDouble() ?: 0.0
                    txn.update(
                        userRef,
                        mapOf(
                            "balance" to (curBal + wd.amount),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    txn.set(
                        refundTxRef,
                        FirestoreMappers.transactionToCreateMap(
                            WalletTransaction(
                                id = "REFUND_${wd.id}",
                                userId = wd.userId,
                                category = TransactionCategory.ADJUSTMENT,
                                title = "Withdrawal Refund (+₹${wd.amount.toInt()})",
                                subtitle = reason,
                                amount = wd.amount,
                                status = TransactionStatus.COMPLETED,
                                createdAtMillis = now
                            )
                        )
                    )
                }
            }.await()
            Unit
        }.recoverCatching { err ->
            if (err is IllegalStateException) throw err
            throw IllegalStateException(FirestoreErrorMapper.userMessage(err), err)
        }
    }

    suspend fun adjustUserBalance(userId: String, delta: Double, note: String): Result<Double> {
        if (delta == 0.0) return Result.failure(IllegalArgumentException("Enter a non-zero adjustment amount."))
        return runCatching {
            val userRef = db.collection(FirestoreCollections.USERS).document(userId)
            val now = System.currentTimeMillis()
            val txId = "ADJ-${now}-${(100..999).random()}"
            val txRef = userRef.collection(FirestoreCollections.TRANSACTIONS).document(txId)

            db.runTransaction { txn ->
                val userSnap = txn.get(userRef)
                if (!userSnap.exists()) throw IllegalStateException("User not found.")
                val curBal = (userSnap.get("balance") as? Number)?.toDouble() ?: 0.0
                val nextBal = (curBal + delta).coerceIn(0.0, 100_000_000.0)
                val appliedDelta = nextBal - curBal

                txn.update(
                    userRef,
                    mapOf(
                        "balance" to nextBal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                txn.set(
                    txRef,
                    FirestoreMappers.transactionToCreateMap(
                        WalletTransaction(
                            id = txId,
                            userId = userId,
                            category = TransactionCategory.ADJUSTMENT,
                            title = if (appliedDelta >= 0) "Admin Credit" else "Admin Debit",
                            subtitle = note.ifBlank { "Wallet adjustment by Kelywin Admin" },
                            amount = appliedDelta,
                            status = TransactionStatus.COMPLETED,
                            createdAtMillis = now
                        )
                    )
                )
                nextBal
            }.await()
        }.recoverCatching { err ->
            if (err is IllegalStateException) throw err
            throw IllegalStateException(FirestoreErrorMapper.userMessage(err), err)
        }
    }

    suspend fun toggleUserBlocked(userId: String): Result<Boolean> {
        return runCatching {
            val userRef = db.collection(FirestoreCollections.USERS).document(userId)
            db.runTransaction { txn ->
                val snap = txn.get(userRef)
                if (!snap.exists()) throw IllegalStateException("User not found.")
                val currentlyBlocked = snap.getBoolean("blocked") ?: false
                val nextBlocked = !currentlyBlocked
                txn.update(
                    userRef,
                    mapOf(
                        "blocked" to nextBlocked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                nextBlocked
            }.await()
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun resolvePasswordReset(
        resetId: String,
        newPassword: String,
        deliveryChannel: String,
        approve: Boolean
    ): Result<Unit> {
        if (approve && newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Assigned password must be at least 6 characters."))
        }
        return runCatching {
            val resetRef = db.collection(FirestoreCollections.PASSWORD_RESETS).document(resetId)
            db.runTransaction { txn ->
                val resetSnap = txn.get(resetRef)
                val req = FirestoreMappers.passwordResetFromSnapshot(resetSnap)
                    ?: throw IllegalStateException("Reset request not found.")
                val userRef = db.collection(FirestoreCollections.USERS).document(req.userId)
                val userSnap = txn.get(userRef)

                if (approve && userSnap.exists()) {
                    val existingSecret = userSnap.getString("secretCode").orEmpty()
                        .ifBlank { SecretRecoveryCrypto.generateSecretCode() }
                    val existingSecretHash = userSnap.getString("secretCodeHash").orEmpty()
                        .ifBlank { SecretRecoveryCrypto.hashSecretCode(existingSecret) }
                    val newHash = SecretRecoveryCrypto.hashPassword(newPassword)
                    val newEncrypted = SecretRecoveryCrypto.encryptPassword(newPassword, existingSecret, req.userId)

                    txn.update(
                        userRef,
                        mapOf(
                            "passwordHash" to newHash,
                            "encryptedPassword" to newEncrypted,
                            "secretCode" to existingSecret,
                            "secretCodeHash" to existingSecretHash,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                txn.update(
                    resetRef,
                    mapOf(
                        "status" to if (approve) RequestStatus.APPROVED.name else RequestStatus.REJECTED.name,
                        "assignedPassword" to if (approve) newPassword else "",
                        "deliveryChannel" to deliveryChannel.ifBlank { "Admin Direct" },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Unit
        }.recoverCatching { err ->
            if (err is IllegalStateException) throw err
            throw IllegalStateException(FirestoreErrorMapper.userMessage(err), err)
        }
    }

    suspend fun grantAdminAccess(uid: String, email: String): Result<Unit> {
        val cleanUid = uid.trim()
        val cleanEmail = email.trim()
        if (cleanUid.isBlank() || cleanEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Provide both Admin Firebase UID and Email."))
        }
        return runCatching {
            db.collection(FirestoreCollections.ADMINS)
                .document(cleanUid)
                .set(FirestoreMappers.adminMemberToCreateMap(cleanUid, cleanEmail))
                .await()
            Unit
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    suspend fun revokeAdminAccess(uid: String): Result<Unit> {
        if (uid == activeAuthorizedUid) {
            return Result.failure(IllegalStateException("You cannot revoke your own active Admin session."))
        }
        return runCatching {
            db.collection(FirestoreCollections.ADMINS).document(uid).delete().await()
            Unit
        }.recoverCatching { throw IllegalStateException(FirestoreErrorMapper.userMessage(it), it) }
    }

    fun exportSnapshotJson(): String {
        val root = JSONObject()
        val cfg = _appConfig.value
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put(
            "appConfig",
            JSONObject().apply {
                put("maintenanceMode", cfg.maintenanceMode)
                put("enableRegistration", cfg.enableRegistration)
                put("enableDeposits", cfg.enableDeposits)
                put("enableWithdrawals", cfg.enableWithdrawals)
                put("enableNumberBetting", cfg.enableNumberBetting)
                put("minBetAmount", cfg.minBetAmount)
                put("firstDepositBonusAmount", cfg.firstDepositBonusAmount)
                put("gatewayName", cfg.upiQrConfig.gatewayName)
                put("merchantUpiPool", cfg.upiQrConfig.merchantUpiPool)
            }
        )
        root.put(
            "users",
            JSONArray().apply {
                _users.value.forEach { u ->
                    put(
                        JSONObject().apply {
                            put("userId", u.userId)
                            put("kwId", u.kwId)
                            put("phone", u.displayPhone)
                            put("balance", u.balance)
                            put("blocked", u.isBlocked)
                            put("secretCode", u.secretCode)
                        }
                    )
                }
            }
        )
        root.put(
            "recentRoundLogs",
            JSONArray().apply {
                _roundLogs.value.take(50).forEach { log ->
                    put(
                        JSONObject().apply {
                            put("modeId", log.modeId)
                            put("periodId", log.periodId)
                            put("winningNumber", log.winningNumber)
                            put("winningColor", log.winningColor.name)
                            put("winningSide", log.winningSide.name)
                            put("controlMode", log.controlModeUsed.name)
                            put("totalBetsAmount", log.totalBetsAmount)
                            put("totalPayoutAmount", log.totalPayoutAmount)
                            put("netProfit", log.netProfit)
                        }
                    )
                }
            }
        )
        return root.toString(2)
    }

    companion object {
        private const val TAG = "AdminFirebaseRepo"
        const val OWNER_ADMIN_EMAIL = "sujitmehta696@gmail.com"
    }
}
