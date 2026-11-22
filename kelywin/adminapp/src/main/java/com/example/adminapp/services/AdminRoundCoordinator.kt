package com.example.adminapp.services

import android.util.Log
import com.example.adminapp.data.AdminAuthState
import com.example.adminapp.data.AdminFirebaseRepository
import com.example.shared.model.GameControlMode
import com.example.shared.model.GameMode
import com.example.shared.model.GameRoundState
import com.example.shared.model.RoomControlConfig
import com.example.shared.model.RoundPeriodClock
import com.example.shared.model.UserBet
import com.example.shared.model.XinLogicResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.SecureRandom

/**
 * Live 24x7 Round Coordinator running inside the standalone Admin APK.
 * Tracks 30sec and 1Min period clocks, resolves outcomes via Random, 1-period Manual,
 * Full Control, or XIN LOGIC (6-period High/Low cycle), and settles bets in Firestore.
 */
class AdminRoundCoordinator(
    private val adminFirebaseRepository: AdminFirebaseRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val secureRandom = SecureRandom()

    private val _roundStates = MutableStateFlow(computeSnapshot(System.currentTimeMillis()))
    val roundStates: StateFlow<Map<GameMode, GameRoundState>> = _roundStates.asStateFlow()

    private val _lastSettledNotice = MutableStateFlow<String?>(null)
    val lastSettledNotice: StateFlow<String?> = _lastSettledNotice.asStateFlow()

    private var lastObservedPeriods = mutableMapOf<GameMode, String>()
    private val settledPeriodKeys = mutableSetOf<String>()

    init {
        val initial = _roundStates.value
        GameMode.entries.forEach { mode ->
            lastObservedPeriods[mode] = initial.getValue(mode).periodId
        }

        scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val snapshot = computeSnapshot(now)
                _roundStates.value = snapshot

                if (adminFirebaseRepository.authState.value is AdminAuthState.Authorized) {
                    GameMode.entries.forEach { mode ->
                        val activePeriodId = snapshot.getValue(mode).periodId
                        val prevPeriodId = lastObservedPeriods[mode]
                        if (prevPeriodId != null && prevPeriodId != activePeriodId) {
                            lastObservedPeriods[mode] = activePeriodId
                            scope.launch {
                                settleClosedPeriod(mode, prevPeriodId)
                            }
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    private suspend fun settleClosedPeriod(mode: GameMode, closedPeriodId: String) {
        val key = "${mode.id}_$closedPeriodId"
        synchronized(settledPeriodKeys) {
            if (!settledPeriodKeys.add(key)) return
        }

        runCatching {
            val roomConfig = adminFirebaseRepository.roomControls.value[mode.id]
                ?: RoomControlConfig(modeId = mode.id)
            val appConfig = adminFirebaseRepository.appConfig.value
            val betsInPeriod = adminFirebaseRepository.fetchPendingBetsForPeriod(mode.id, closedPeriodId)

            val (winningNumber, modeUsed, revertManualConfig) = resolveWinningNumber(
                mode = mode,
                closedPeriodId = closedPeriodId,
                roomConfig = roomConfig,
                betsInPeriod = betsInPeriod,
                multipliers = appConfig.multipliers
            )

            adminFirebaseRepository.publishRoundResultAndSettle(
                mode = mode,
                periodId = closedPeriodId,
                winningNumber = winningNumber,
                controlModeUsed = modeUsed,
                multipliers = appConfig.multipliers,
                allBetsInPeriod = betsInPeriod,
                autoRevertManualToFallback = revertManualConfig
            ).onSuccess { log ->
                _lastSettledNotice.value =
                    "${mode.shortLabel} $closedPeriodId -> #${log.winningNumber} (${log.winningColor.label} / ${log.winningSide.label}) via ${modeUsed.label}"
            }.onFailure { err ->
                Log.w(TAG, "Settlement failed for $key: ${err.message}")
            }
        }
    }

    private fun resolveWinningNumber(
        mode: GameMode,
        closedPeriodId: String,
        roomConfig: RoomControlConfig,
        betsInPeriod: List<UserBet>,
        multipliers: com.example.shared.model.PayoutMultipliers
    ): Triple<Int, GameControlMode, RoomControlConfig?> {
        val isTargetedManual = roomConfig.controlMode == GameControlMode.MANUAL &&
            (roomConfig.manualTargetPeriodId.isNullOrBlank() || roomConfig.manualTargetPeriodId == closedPeriodId)

        if (isTargetedManual) {
            return Triple(
                roomConfig.manualNumber.coerceIn(0, 9),
                GameControlMode.MANUAL,
                roomConfig
            )
        }

        val effectiveMode = if (roomConfig.controlMode == GameControlMode.MANUAL) {
            if (roomConfig.fallbackMode == GameControlMode.MANUAL) GameControlMode.RANDOM else roomConfig.fallbackMode
        } else {
            roomConfig.controlMode
        }

        val winningNumber = when (effectiveMode) {
            GameControlMode.RANDOM -> secureRandom.nextInt(10)
            GameControlMode.FULL_CONTROL -> XinLogicResolver.resolveLowestPayoutNumber(
                pendingBets = betsInPeriod,
                multipliers = multipliers,
                randomIndexPicker = { size -> secureRandom.nextInt(size) }
            )
            GameControlMode.XIN_LOGIC -> XinLogicResolver.resolveNumber(
                pendingBets = betsInPeriod,
                periodIndex = RoundPeriodClock.periodIndexFromId(closedPeriodId),
                anchorPeriodIndex = roomConfig.xinAnchorPeriodIndex,
                pattern = roomConfig.xinPattern,
                multipliers = multipliers,
                randomIndexPicker = { size -> secureRandom.nextInt(size) }
            )
            GameControlMode.MANUAL -> secureRandom.nextInt(10)
        }

        return Triple(winningNumber.coerceIn(0, 9), effectiveMode, null)
    }

    private fun computeSnapshot(nowMillis: Long): Map<GameMode, GameRoundState> = mapOf(
        GameMode.SEC_30 to RoundPeriodClock.computeRoundState(GameMode.SEC_30, nowMillis),
        GameMode.MIN_1 to RoundPeriodClock.computeRoundState(GameMode.MIN_1, nowMillis)
    )

    companion object {
        private const val TAG = "AdminRoundCoord"
    }
}
