package com.example.services

import com.example.data.local.AppControlConfigDao
import com.example.data.local.PeriodHistoryDao
import com.example.data.local.PeriodHistoryEntity
import com.example.data.local.RoomControlConfigEntity
import com.example.data.local.RoomControlDao
import com.example.data.local.RoundAdminLogDao
import com.example.data.local.UserBetDao
import com.example.data.local.UserBetEntity
import com.example.data.local.UserDao
import com.example.data.local.WalletTransactionDao
import com.example.data.local.WalletTransactionEntity
import com.example.shared.model.BetColor
import com.example.shared.model.BetSelectionType
import com.example.shared.model.BetStatus
import com.example.shared.model.DragonTigerSide
import com.example.shared.model.GameControlMode
import com.example.shared.model.GameMode
import com.example.shared.model.GameRoundState
import com.example.shared.model.GameRules
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.PeriodHistoryItem
import com.example.shared.model.RoundPeriodClock
import com.example.shared.model.RoundSettlementResult
import com.example.shared.model.StrategyTip
import com.example.shared.model.TransactionCategory
import com.example.shared.model.TransactionStatus
import com.example.shared.model.UserBet
import com.example.shared.model.WalletTransaction
import com.example.shared.model.XinLogicResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

data class ResolvedRoundOutcome(
    val number: Int,
    val color: BetColor,
    val side: DragonTigerSide,
    val controlModeUsed: GameControlMode
)

/**
 * User-APK 24x7 PeriodEngine.
 * - Uses [RoundPeriodClock] so 30sec & 1Min period IDs match the Admin APK to the exact second.
 * - Enforces ₹1+ custom bets (or Admin's configured `minBetAmount`), `enableNumberBetting`, and transactional Firestore balance deduction.
 * - Reads authoritative Admin outcomes from Firestore `/round_results/{modeId}_{periodId}` on rollover (never writes to `/round_results` or `/room_controls`).
 * - Falls back cleanly to synced `RoomControlConfig` (`RANDOM`, `FULL_CONTROL`, or `XIN_LOGIC`) if Admin APK is offline, settling user bets idempotently.
 */
class PeriodEngine(
    private val periodHistoryDao: PeriodHistoryDao,
    private val userBetDao: UserBetDao,
    private val userDao: UserDao,
    private val walletTransactionDao: WalletTransactionDao,
    private val roomControlDao: RoomControlDao,
    @Suppress("unused") private val roundAdminLogDao: RoundAdminLogDao,
    private val appControlConfigDao: AppControlConfigDao,
    private val userFirebaseRepository: UserFirebaseRepository? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val settlementMutex = Mutex()
    private val random = SecureRandom()

    private val _roundStates = MutableStateFlow(
        GameMode.entries.associateWith { RoundPeriodClock.computeRoundState(it, System.currentTimeMillis()) }
    )
    val roundStates: StateFlow<Map<GameMode, GameRoundState>> = _roundStates.asStateFlow()

    private val _settlementEvents = MutableSharedFlow<RoundSettlementResult>(extraBufferCapacity = 8)
    val settlementEvents: SharedFlow<RoundSettlementResult> = _settlementEvents.asSharedFlow()

    private val activePeriodTracker = mutableMapOf<GameMode, String>()
    private var activeUserIdProvider: () -> String? = { null }

    init {
        scope.launch {
            seedDefaultConfigsIfNeeded()
            seedInitialHistoryIfNeeded()
            startContinuousClock()
        }
    }

    fun bindActiveUserProvider(provider: () -> String?) {
        activeUserIdProvider = provider
    }

    fun observeHistory(mode: GameMode): Flow<List<PeriodHistoryItem>> =
        periodHistoryDao.observeHistory(mode.id, limit = 60).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeUserBets(userId: String): Flow<List<UserBet>> =
        userBetDao.observeUserBets(userId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeRoundBets(userId: String, mode: GameMode, periodId: String): Flow<List<UserBet>> =
        userBetDao.observeRoundBets(userId, mode.id, periodId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun computeStrategyTips(history: List<PeriodHistoryItem>): List<StrategyTip> {
        if (history.isEmpty()) {
            return listOf(
                StrategyTip(
                    title = "Dragon / Tiger Momentum",
                    suggestedPick = DragonTigerSide.DRAGON.label,
                    selectionType = BetSelectionType.SIDE,
                    confidencePercent = 64,
                    rationale = "Waiting for initial rounds to calibrate trend signals."
                )
            )
        }

        val recent = history.take(12)
        val dragonCount = recent.count { it.side == DragonTigerSide.DRAGON }
        val tigerCount = recent.size - dragonCount
        val sidePick = if (dragonCount >= tigerCount) DragonTigerSide.DRAGON else DragonTigerSide.TIGER
        val sideConfidence = (55 + kotlin.math.abs(dragonCount - tigerCount) * 4).coerceAtMost(84)

        val greenCount = recent.count { BetColor.GREEN in it.colors }
        val redCount = recent.count { BetColor.RED in it.colors }
        val colorPick = if (greenCount <= redCount) BetColor.GREEN else BetColor.RED
        val colorConfidence = (54 + kotlin.math.abs(greenCount - redCount) * 4).coerceAtMost(81)

        val freqMap = (0..9).associateWith { num -> recent.count { it.number == num } }
        val coldNumber = freqMap.minByOrNull { it.value }?.key ?: 7

        return listOf(
            StrategyTip(
                title = "Dragon / Tiger Streak",
                suggestedPick = sidePick.label,
                selectionType = BetSelectionType.SIDE,
                confidencePercent = sideConfidence,
                rationale = "Last ${recent.size} rounds: Dragon $dragonCount vs Tiger $tigerCount."
            ),
            StrategyTip(
                title = "Color Parity Signal",
                suggestedPick = colorPick.label,
                selectionType = BetSelectionType.COLOR,
                confidencePercent = colorConfidence,
                rationale = "Green appeared $greenCount times vs Red $redCount times."
            ),
            StrategyTip(
                title = "Cold Number Rebound",
                suggestedPick = coldNumber.toString(),
                selectionType = BetSelectionType.NUMBER,
                confidencePercent = 69,
                rationale = "Number $coldNumber has the fewest hits (${freqMap[coldNumber] ?: 0}) in recent rounds."
            )
        )
    }

    suspend fun placeBet(
        userId: String,
        mode: GameMode,
        selectionType: BetSelectionType,
        selectionValue: String,
        baseStake: Double,
        multiplier: Int
    ): Result<UserBet> {
        val currentState = _roundStates.value[mode] ?: RoundPeriodClock.computeRoundState(mode, System.currentTimeMillis())
        if (currentState.isLocked) {
            return Result.failure(IllegalStateException("Round ${currentState.periodId} is locked for settlement."))
        }

        val appConfig = appControlConfigDao.getConfig()?.toDomain()
        if (appConfig != null && !appConfig.enableNumberBetting && selectionType == BetSelectionType.NUMBER) {
            return Result.failure(IllegalStateException("0–9 number betting is currently paused by Admin."))
        }

        val minBet = (appConfig?.minBetAmount ?: 1.0).coerceAtLeast(1.0)
        val totalStake = baseStake * multiplier
        if (baseStake < 1.0 || totalStake < minBet) {
            return Result.failure(IllegalArgumentException("Minimum bet amount is ₹${minBet.toInt()}."))
        }

        return settlementMutex.withLock {
            val user = userDao.getUserById(userId)
                ?: return@withLock Result.failure(IllegalStateException("Please log in to place bets."))
            if (user.isBlocked) {
                return@withLock Result.failure(IllegalStateException("Your account is blocked. Contact Kelywin support."))
            }
            if (user.balance + 0.0001 < totalStake) {
                return@withLock Result.failure(IllegalStateException("Insufficient balance. Available ₹${String.format(Locale.US, "%.2f", user.balance)}."))
            }

            val now = System.currentTimeMillis()
            val betId = "BET-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
            val txId = "TX-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"

            val betDomain = UserBet(
                id = betId,
                userId = userId,
                periodId = currentState.periodId,
                modeId = mode.id,
                selectionType = selectionType,
                selectionValue = selectionValue,
                baseStake = baseStake,
                multiplier = multiplier,
                totalStake = totalStake,
                status = BetStatus.PENDING,
                payout = 0.0,
                createdAtMillis = now
            )
            val txDomain = WalletTransaction(
                id = txId,
                userId = userId,
                category = TransactionCategory.BET,
                title = "${mode.shortLabel} Bet · $selectionValue",
                subtitle = "Period ${currentState.periodId} (x$multiplier)",
                amount = -totalStake,
                status = TransactionStatus.COMPLETED,
                createdAtMillis = now
            )

            val updatedBalance = if (userFirebaseRepository != null && userFirebaseRepository.currentFirebaseUid() == userId) {
                userFirebaseRepository.placeBetCloud(userId, betDomain, txDomain)
                    .getOrElse { return@withLock Result.failure(it) }
            } else {
                (user.balance - totalStake).coerceAtLeast(0.0)
            }

            userDao.updateBalance(userId, updatedBalance)
            userBetDao.insert(
                UserBetEntity(
                    id = betDomain.id,
                    userId = betDomain.userId,
                    periodId = betDomain.periodId,
                    modeId = betDomain.modeId,
                    selectionType = betDomain.selectionType.name,
                    selectionValue = betDomain.selectionValue,
                    baseStake = betDomain.baseStake,
                    multiplier = betDomain.multiplier,
                    totalStake = betDomain.totalStake,
                    status = betDomain.status.name,
                    payout = 0.0,
                    createdAtMillis = now
                )
            )
            walletTransactionDao.insert(
                WalletTransactionEntity(
                    id = txDomain.id,
                    userId = txDomain.userId,
                    category = txDomain.category.name,
                    title = txDomain.title,
                    subtitle = txDomain.subtitle,
                    amount = txDomain.amount,
                    status = txDomain.status.name,
                    createdAtMillis = now
                )
            )

            Result.success(betDomain)
        }
    }

    private suspend fun startContinuousClock() {
        GameMode.entries.forEach { mode ->
            val initial = RoundPeriodClock.computeRoundState(mode, System.currentTimeMillis())
            activePeriodTracker[mode] = initial.periodId
        }

        while (scope.isActive) {
            val now = System.currentTimeMillis()
            val updated = mutableMapOf<GameMode, GameRoundState>()

            for (mode in GameMode.entries) {
                val state = RoundPeriodClock.computeRoundState(mode, now)
                updated[mode] = state

                val previousPeriod = activePeriodTracker[mode]
                if (previousPeriod != null && previousPeriod != state.periodId) {
                    activePeriodTracker[mode] = state.periodId
                    scope.launch {
                        // Brief grace window so Admin APK can publish /round_results first
                        delay(1400L)
                        settleClosedPeriod(mode, previousPeriod)
                    }
                }
            }
            _roundStates.value = updated
            delay(250L)
        }
    }

    private suspend fun settleClosedPeriod(mode: GameMode, periodId: String) {
        settlementMutex.withLock {
            val appConfig = appControlConfigDao.getConfig()?.toDomain()
            val multipliers = appConfig?.multipliers ?: PayoutMultipliers()
            val pendingLocalBets = userBetDao.getPendingBetsForPeriod(mode.id, periodId)

            // 1. Prefer authoritative Admin-published result from Firestore
            val cloudRecord = userFirebaseRepository?.fetchRoundResultRecord(mode.id, periodId)
            val existingLocal = periodHistoryDao.getByPeriod(mode.id, periodId)

            val outcome = when {
                cloudRecord != null -> {
                    val primaryColor = GameRules.primaryColorForNumber(cloudRecord.winningNumber)
                    ResolvedRoundOutcome(
                        number = cloudRecord.winningNumber,
                        color = primaryColor,
                        side = cloudRecord.winningSide,
                        controlModeUsed = cloudRecord.controlModeUsed
                    )
                }
                existingLocal != null -> {
                    ResolvedRoundOutcome(
                        number = existingLocal.winningNumber,
                        color = runCatching { BetColor.valueOf(existingLocal.winningColor) }.getOrDefault(BetColor.GREEN),
                        side = runCatching { DragonTigerSide.valueOf(existingLocal.winningSide) }.getOrDefault(DragonTigerSide.DRAGON),
                        controlModeUsed = runCatching { GameControlMode.valueOf(existingLocal.controlModeUsed) }
                            .getOrDefault(GameControlMode.RANDOM)
                    )
                }
                else -> resolveFallbackLocally(mode, periodId, pendingLocalBets.map { it.toDomain() }, multipliers)
            }

            periodHistoryDao.insert(
                PeriodHistoryEntity(
                    periodId = periodId,
                    modeId = mode.id,
                    winningNumber = outcome.number,
                    winningColor = outcome.color.name,
                    winningSide = outcome.side.name,
                    controlModeUsed = outcome.controlModeUsed.name,
                    timestampMillis = cloudRecord?.settledAtMillis ?: System.currentTimeMillis()
                )
            )

            val activeUid = activeUserIdProvider()
            if (activeUid != null) {
                val cloudUid = userFirebaseRepository?.currentFirebaseUid()
                if (userFirebaseRepository != null && cloudUid == activeUid) {
                    val settledFromCloud = userFirebaseRepository.settleUserBetsInCloud(
                        uid = activeUid,
                        mode = mode,
                        periodId = periodId,
                        winningNumber = outcome.number,
                        multipliers = multipliers
                    )
                    if (settledFromCloud != null && settledFromCloud.settledBets.isNotEmpty()) {
                        userBetDao.updateAll(
                            settledFromCloud.settledBets.map { b ->
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
                        )
                        _settlementEvents.tryEmit(settledFromCloud)
                        return@withLock
                    }
                }

                // Local-only fallback if running offline or in unit tests
                val userPending = pendingLocalBets.filter { it.userId == activeUid }
                if (userPending.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val updatedEntities = mutableListOf<UserBetEntity>()
                    var creditTotal = 0.0
                    for (bet in userPending) {
                        val payout = GameRules.calculatePayout(
                            selectionType = BetSelectionType.valueOf(bet.selectionType),
                            selectionValue = bet.selectionValue,
                            totalStake = bet.totalStake,
                            winningNumber = outcome.number,
                            multipliers = multipliers
                        )
                        val status = if (payout > 0.0) BetStatus.WON else BetStatus.LOST
                        updatedEntities.add(bet.copy(status = status.name, payout = payout))
                        creditTotal += payout
                    }
                    userBetDao.updateAll(updatedEntities)

                    if (creditTotal > 0.0) {
                        val user = userDao.getUserById(activeUid)
                        if (user != null) {
                            userDao.updateBalance(activeUid, user.balance + creditTotal)
                            walletTransactionDao.insert(
                                WalletTransactionEntity(
                                    id = "WIN_${mode.id}_${periodId}_${activeUid.take(10)}",
                                    userId = activeUid,
                                    category = TransactionCategory.WIN.name,
                                    title = "${mode.shortLabel} Win · #${outcome.number} (${outcome.color.label})",
                                    subtitle = "Period $periodId (${outcome.side.label})",
                                    amount = creditTotal,
                                    status = TransactionStatus.COMPLETED.name,
                                    createdAtMillis = now
                                )
                            )
                        }
                    }

                    val domainList = updatedEntities.map { it.toDomain() }
                    _settlementEvents.tryEmit(
                        RoundSettlementResult(
                            mode = mode,
                            periodId = periodId,
                            winningNumber = outcome.number,
                            winningColors = GameRules.colorsForNumber(outcome.number),
                            winningSide = outcome.side,
                            settledBets = domainList,
                            totalStake = domainList.sumOf { it.totalStake },
                            totalPayout = domainList.sumOf { it.payout }
                        )
                    )
                }
            }
        }
    }

    private suspend fun resolveFallbackLocally(
        mode: GameMode,
        periodId: String,
        pendingBets: List<UserBet>,
        multipliers: PayoutMultipliers
    ): ResolvedRoundOutcome {
        val cfg = roomControlDao.getConfig(mode.id)?.toDomain()
        val effectiveMode = when {
            cfg == null -> GameControlMode.RANDOM
            cfg.controlMode == GameControlMode.MANUAL && cfg.manualTargetPeriodId == periodId -> GameControlMode.MANUAL
            cfg.controlMode == GameControlMode.MANUAL -> cfg.fallbackMode
            else -> cfg.controlMode
        }

        val num = when (effectiveMode) {
            GameControlMode.MANUAL -> cfg?.manualNumber?.coerceIn(0, 9) ?: random.nextInt(10)
            GameControlMode.FULL_CONTROL -> XinLogicResolver.resolveLowestPayoutNumber(pendingBets, multipliers)
            GameControlMode.XIN_LOGIC -> {
                val periodIdx = RoundPeriodClock.periodIndexFromId(periodId)
                XinLogicResolver.resolveNumber(
                    pendingBets = pendingBets,
                    periodIndex = periodIdx,
                    anchorPeriodIndex = cfg?.xinAnchorPeriodIndex ?: 1,
                    pattern = cfg?.xinPattern ?: com.example.shared.model.XinLogicPattern.TWO_HIGH_FOUR_LOW,
                    multipliers = multipliers
                )
            }
            GameControlMode.RANDOM -> random.nextInt(10)
        }

        return ResolvedRoundOutcome(
            number = num,
            color = GameRules.primaryColorForNumber(num),
            side = GameRules.sideForNumber(num),
            controlModeUsed = effectiveMode
        )
    }

    private suspend fun seedDefaultConfigsIfNeeded() {
        for (mode in GameMode.entries) {
            if (roomControlDao.getConfig(mode.id) == null) {
                roomControlDao.upsert(
                    RoomControlConfigEntity(
                        modeId = mode.id,
                        controlMode = GameControlMode.RANDOM.name,
                        fallbackMode = GameControlMode.RANDOM.name,
                        manualNumber = 7,
                        manualTargetPeriodId = null
                    )
                )
            }
        }
    }

    private suspend fun seedInitialHistoryIfNeeded() {
        val now = System.currentTimeMillis()
        for (mode in GameMode.entries) {
            if (periodHistoryDao.countForMode(mode.id) > 0) continue

            val currentState = RoundPeriodClock.computeRoundState(mode, now)
            val currentIdx = currentState.periodIndex
            val prefix = currentState.periodId.dropLast(5)
            val durationMillis = mode.durationSeconds * 1000L

            val seeded = (1..20).map { offset ->
                val idx = (currentIdx - offset).coerceAtLeast(1)
                val pid = prefix + idx.toString().padStart(5, '0')
                val num = random.nextInt(10)
                val primaryColor = GameRules.primaryColorForNumber(num)
                val side = GameRules.sideForNumber(num)
                PeriodHistoryEntity(
                    periodId = pid,
                    modeId = mode.id,
                    winningNumber = num,
                    winningColor = primaryColor.name,
                    winningSide = side.name,
                    controlModeUsed = GameControlMode.RANDOM.name,
                    timestampMillis = now - offset * durationMillis
                )
            }
            periodHistoryDao.insertAll(seeded)
        }
    }
}
