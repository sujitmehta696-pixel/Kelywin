package com.example.adminapp

import android.app.Application
import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.adminapp.data.AdminAuthState
import com.example.shared.auth.GoogleSignInHelper
import com.example.shared.model.AdminDashboardStats
import com.example.shared.model.AdminGatewaySecret
import com.example.shared.model.AdminMember
import com.example.shared.model.AppControlConfig
import com.example.shared.model.BetColor
import com.example.shared.model.BetSelectionType
import com.example.shared.model.BetStatus
import com.example.shared.model.CandidateOutcomeScore
import com.example.shared.model.DepositRequest
import com.example.shared.model.DragonTigerSide
import com.example.shared.model.GameControlMode
import com.example.shared.model.GameMode
import com.example.shared.model.GameRoundState
import com.example.shared.model.PasswordResetRequest
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.RequestStatus
import com.example.shared.model.RoundBetPool
import com.example.shared.model.RoomControlConfig
import com.example.shared.model.RoundAdminLog
import com.example.shared.model.RoundPeriodClock
import com.example.shared.model.UpiQrConfig
import com.example.shared.model.UserAccount
import com.example.shared.model.UserBet
import com.example.shared.model.WithdrawalRequest
import com.example.shared.model.XinLogicPattern
import com.example.shared.model.XinLogicResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val adminApp = application as AdminRuntimeHost
    private val repo = adminApp.adminFirebaseRepository
    private val coordinator = adminApp.adminRoundCoordinator

    val authState: StateFlow<AdminAuthState> = repo.authState
    val roundStates: StateFlow<Map<GameMode, GameRoundState>> = coordinator.roundStates
    val appConfig: StateFlow<AppControlConfig> = repo.appConfig
    val roomControls: StateFlow<Map<String, RoomControlConfig>> = repo.roomControls
    val users: StateFlow<List<UserAccount>> = repo.users
    val allBets: StateFlow<List<UserBet>> = repo.allBets
    val deposits: StateFlow<List<DepositRequest>> = repo.deposits
    val withdrawals: StateFlow<List<WithdrawalRequest>> = repo.withdrawals
    val passwordResets: StateFlow<List<PasswordResetRequest>> = repo.passwordResets
    val roundLogs: StateFlow<List<RoundAdminLog>> = repo.roundLogs
    val adminMembers: StateFlow<List<AdminMember>> = repo.adminMembers
    val famGatewaySecret: StateFlow<AdminGatewaySecret> = repo.famGatewaySecret

    private val _selectedGameMode = MutableStateFlow(GameMode.SEC_30)
    val selectedGameMode: StateFlow<GameMode> = _selectedGameMode.asStateFlow()

    private val _googleSignInLoading = MutableStateFlow(false)
    val googleSignInLoading: StateFlow<Boolean> = _googleSignInLoading.asStateFlow()

    private val _googleSignInError = MutableStateFlow<String?>(null)
    val googleSignInError: StateFlow<String?> = _googleSignInError.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _exportedJsonDialog = MutableStateFlow<String?>(null)
    val exportedJsonDialog: StateFlow<String?> = _exportedJsonDialog.asStateFlow()

    val currentActiveBetsForSelectedRoom: StateFlow<List<UserBet>> = combine(
        _selectedGameMode,
        roundStates,
        allBets
    ) { mode, states, bets ->
        val periodId = states[mode]?.periodId.orEmpty()
        bets.filter { it.modeId == mode.id && it.periodId == periodId && it.status == BetStatus.PENDING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveRoundPool: StateFlow<RoundBetPool> = currentActiveBetsForSelectedRoom
        .combine(_selectedGameMode) { bets, _ ->
            var green = 0.0
            var violet = 0.0
            var red = 0.0
            var dragon = 0.0
            var tiger = 0.0
            val numMap = mutableMapOf<Int, Double>()

            bets.forEach { bet ->
                when (bet.selectionType) {
                    BetSelectionType.COLOR -> when {
                        bet.selectionValue.equals(BetColor.GREEN.label, ignoreCase = true) -> green += bet.totalStake
                        bet.selectionValue.equals(BetColor.VIOLET.label, ignoreCase = true) -> violet += bet.totalStake
                        bet.selectionValue.equals(BetColor.RED.label, ignoreCase = true) -> red += bet.totalStake
                    }
                    BetSelectionType.SIDE -> when {
                        bet.selectionValue.equals(DragonTigerSide.DRAGON.label, ignoreCase = true) -> dragon += bet.totalStake
                        bet.selectionValue.equals(DragonTigerSide.TIGER.label, ignoreCase = true) -> tiger += bet.totalStake
                    }
                    BetSelectionType.NUMBER -> {
                        val n = bet.selectionValue.toIntOrNull()
                        if (n != null && n in 0..9) {
                            numMap[n] = (numMap[n] ?: 0.0) + bet.totalStake
                        }
                    }
                }
            }

            RoundBetPool(
                greenStake = green,
                violetStake = violet,
                redStake = red,
                dragonStake = dragon,
                tigerStake = tiger,
                numberStakes = numMap,
                totalBetCount = bets.size
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoundBetPool())

    val candidateScores: StateFlow<List<CandidateOutcomeScore>> = combine(
        currentActiveBetsForSelectedRoom,
        appConfig
    ) { bets, cfg ->
        XinLogicResolver.evaluateCandidateScores(bets, cfg.multipliers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<AdminDashboardStats> = combine(
        users,
        allBets,
        deposits,
        withdrawals,
        passwordResets
    ) { userList, betList, depList, wdList, resetList ->
        val totalVolume = betList.sumOf { it.totalStake }
        val totalPayouts = betList.filter { it.status == BetStatus.WON }.sumOf { it.payout }
        AdminDashboardStats(
            totalUsers = userList.size,
            activePlayers = userList.count { !it.isBlocked },
            totalBetsVolume = totalVolume,
            totalPayouts = totalPayouts,
            netMargin = totalVolume - totalPayouts,
            pendingDeposits = depList.count { it.status == RequestStatus.PENDING },
            pendingWithdrawals = wdList.count { it.status == RequestStatus.PENDING },
            pendingPasswordResets = resetList.count { it.status == RequestStatus.PENDING }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardStats())

    init {
        viewModelScope.launch {
            coordinator.lastSettledNotice.collect { notice ->
                if (!notice.isNullOrBlank()) {
                    _feedbackMessage.value = notice
                }
            }
        }
    }

    fun attemptSilentSignIn(context: Context) {
        viewModelScope.launch {
            val webClientId = resolveWebClientId(context)
            if (webClientId.isNotBlank()) {
                val signed = GoogleSignInHelper.attemptSilentSignIn(context, webClientId)
                if (signed != null) {
                    repo.refreshAuthorization()
                }
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        if (_googleSignInLoading.value) return
        val webClientId = resolveWebClientId(activityContext)
        _googleSignInLoading.value = true
        _googleSignInError.value = null

        viewModelScope.launch {
            val result = GoogleSignInHelper.signInWithGoogle(activityContext, webClientId)
            _googleSignInLoading.value = false
            result.onSuccess {
                repo.refreshAuthorization()
            }.onFailure { err ->
                if (err !is GetCredentialCancellationException) {
                    _googleSignInError.value = err.message ?: "Google Sign-In failed."
                }
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            repo.stopListeners()
            GoogleSignInHelper.signOut(context)
            repo.refreshAuthorization()
        }
    }

    fun selectGameMode(mode: GameMode) {
        _selectedGameMode.value = mode
    }

    fun setControlMode(mode: GameMode, controlMode: GameControlMode) {
        viewModelScope.launch {
            val existing = roomControls.value[mode.id] ?: RoomControlConfig(modeId = mode.id)
            val currentState = roundStates.value[mode] ?: RoundPeriodClock.computeRoundState(mode, System.currentTimeMillis())
            val next = when (controlMode) {
                GameControlMode.MANUAL -> existing.copy(
                    controlMode = GameControlMode.MANUAL,
                    manualTargetPeriodId = currentState.periodId
                )
                GameControlMode.XIN_LOGIC -> existing.copy(
                    controlMode = GameControlMode.XIN_LOGIC,
                    xinAnchorPeriodIndex = if (existing.xinAnchorPeriodIndex <= 0) currentState.periodIndex else existing.xinAnchorPeriodIndex,
                    manualTargetPeriodId = null
                )
                else -> existing.copy(
                    controlMode = controlMode,
                    manualTargetPeriodId = null
                )
            }
            repo.updateRoomControl(next).fold(
                onSuccess = {
                    _feedbackMessage.value = "${mode.shortLabel} switched to ${controlMode.label}"
                },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun setManualOutcome(mode: GameMode, number: Int, targetNextPeriod: Boolean = false) {
        viewModelScope.launch {
            val existing = roomControls.value[mode.id] ?: RoomControlConfig(modeId = mode.id)
            val liveState = roundStates.value[mode] ?: RoundPeriodClock.computeRoundState(mode, System.currentTimeMillis())
            val targetPeriod = if (targetNextPeriod) {
                RoundPeriodClock.computeNextPeriodId(mode, liveState.periodId)
            } else {
                liveState.periodId
            }
            val next = existing.copy(
                controlMode = GameControlMode.MANUAL,
                manualNumber = number.coerceIn(0, 9),
                manualTargetPeriodId = targetPeriod
            )
            repo.updateRoomControl(next).fold(
                onSuccess = {
                    _feedbackMessage.value = "Manual result #$number locked for 1 round ($targetPeriod)"
                },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun setFallbackMode(mode: GameMode, fallback: GameControlMode) {
        viewModelScope.launch {
            val existing = roomControls.value[mode.id] ?: RoomControlConfig(modeId = mode.id)
            val safeFallback = if (fallback == GameControlMode.MANUAL) GameControlMode.RANDOM else fallback
            repo.updateRoomControl(existing.copy(fallbackMode = safeFallback)).fold(
                onSuccess = { _feedbackMessage.value = "Fallback after 1-period Manual set to ${safeFallback.label}" },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun setXinPattern(mode: GameMode, pattern: XinLogicPattern) {
        viewModelScope.launch {
            val existing = roomControls.value[mode.id] ?: RoomControlConfig(modeId = mode.id)
            val livePeriodIndex = roundStates.value[mode]?.periodIndex ?: existing.xinAnchorPeriodIndex.coerceAtLeast(1)
            val anchor = if (existing.xinAnchorPeriodIndex <= 0) livePeriodIndex else existing.xinAnchorPeriodIndex
            repo.updateRoomControl(
                existing.copy(
                    controlMode = GameControlMode.XIN_LOGIC,
                    xinPattern = pattern,
                    xinAnchorPeriodIndex = anchor,
                    manualTargetPeriodId = null
                )
            ).fold(
                onSuccess = { _feedbackMessage.value = "XIN LOGIC pattern set: ${pattern.label}" },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun resetXinCycleToNow(mode: GameMode, pattern: XinLogicPattern) {
        viewModelScope.launch {
            val liveIndex = roundStates.value[mode]?.periodIndex
                ?: RoundPeriodClock.computeRoundState(mode, System.currentTimeMillis()).periodIndex
            repo.resetXinCycleToPeriod(mode, liveIndex, pattern).fold(
                onSuccess = { _feedbackMessage.value = "XIN cycle reset to Step 1/6 starting Period #$liveIndex" },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun saveGatewayConfig(upiConfig: UpiQrConfig) {
        viewModelScope.launch {
            val updated = appConfig.value.copy(upiQrConfig = upiConfig)
            repo.saveAppConfig(updated).fold(
                onSuccess = { _feedbackMessage.value = "Dynamic UPI Gateway settings synced to User App." },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun saveFamGatewayConfig(apiKey: String, backendUrl: String, enabled: Boolean) {
        viewModelScope.launch {
            repo.saveFamGatewayConfig(apiKey = apiKey, backendUrl = backendUrl, enabled = enabled).fold(
                onSuccess = {
                    _feedbackMessage.value = "FamGateway Secret Key & Backend URL saved securely & synced!"
                },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun approveDeposit(depositId: String, note: String = "Verified by Admin") {
        viewModelScope.launch {
            repo.approveDeposit(depositId, note).fold(
                onSuccess = { bonus ->
                    _feedbackMessage.value = if (bonus > 0) {
                        "Deposit approved + ₹${bonus.toInt()} 1st-deposit bonus credited!"
                    } else {
                        "Deposit approved & credited to user wallet."
                    }
                },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun rejectDeposit(depositId: String, note: String = "UTR mismatch") {
        viewModelScope.launch {
            repo.rejectDeposit(depositId, note).fold(
                onSuccess = { _feedbackMessage.value = "Deposit marked as Rejected." },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun approveWithdrawal(withdrawalId: String, note: String = "Paid by Admin") {
        viewModelScope.launch {
            repo.approveWithdrawal(withdrawalId, note).fold(
                onSuccess = { _feedbackMessage.value = "Withdrawal marked as Paid & Approved." },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun rejectWithdrawal(withdrawalId: String, note: String = "Payout failed, refunded") {
        viewModelScope.launch {
            repo.rejectWithdrawal(withdrawalId, note).fold(
                onSuccess = { _feedbackMessage.value = "Withdrawal rejected & balance refunded." },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun adjustUserBalance(userId: String, delta: Double, note: String) {
        viewModelScope.launch {
            repo.adjustUserBalance(userId, delta, note).fold(
                onSuccess = { nextBal ->
                    _feedbackMessage.value = "Updated wallet balance: ₹${String.format(java.util.Locale.US, "%.2f", nextBal)}"
                },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun toggleUserBlocked(userId: String) {
        viewModelScope.launch {
            repo.toggleUserBlocked(userId).fold(
                onSuccess = { blocked ->
                    _feedbackMessage.value = if (blocked) "User account blocked." else "User account unblocked."
                },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun resolvePasswordReset(resetId: String, newPassword: String, channel: String, approve: Boolean) {
        viewModelScope.launch {
            repo.resolvePasswordReset(resetId, newPassword, channel, approve).fold(
                onSuccess = {
                    _feedbackMessage.value = if (approve) "New password saved & synced to cloud." else "Reset request rejected."
                },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun saveRemoteAppControls(
        announcementText: String,
        maintenanceMode: Boolean,
        maintenanceMessage: String,
        enableRegistration: Boolean,
        enableDeposits: Boolean,
        enableWithdrawals: Boolean,
        enableNumberBetting: Boolean,
        minBetAmount: Double,
        firstDepositBonusAmount: Double,
        multipliers: PayoutMultipliers
    ) {
        viewModelScope.launch {
            val next = appConfig.value.copy(
                announcementText = announcementText.trim(),
                maintenanceMode = maintenanceMode,
                maintenanceMessage = maintenanceMessage.trim(),
                enableRegistration = enableRegistration,
                enableDeposits = enableDeposits,
                enableWithdrawals = enableWithdrawals,
                enableNumberBetting = enableNumberBetting,
                minBetAmount = minBetAmount.coerceAtLeast(1.0),
                firstDepositBonusAmount = firstDepositBonusAmount.coerceAtLeast(0.0),
                multipliers = multipliers
            )
            repo.saveAppConfig(next).fold(
                onSuccess = { _feedbackMessage.value = "Remote User App settings published!" },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun grantAdminAccess(uid: String, email: String) {
        viewModelScope.launch {
            repo.grantAdminAccess(uid, email).fold(
                onSuccess = { _feedbackMessage.value = "Admin access granted to $email" },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun revokeAdminAccess(uid: String) {
        viewModelScope.launch {
            repo.revokeAdminAccess(uid).fold(
                onSuccess = { _feedbackMessage.value = "Admin access revoked." },
                onFailure = { _feedbackMessage.value = it.message }
            )
        }
    }

    fun exportSnapshot() {
        _exportedJsonDialog.value = repo.exportSnapshotJson()
    }

    fun dismissExportDialog() {
        _exportedJsonDialog.value = null
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    private fun resolveWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else ""
    }
}
