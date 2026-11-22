package com.example

import android.app.Application
import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.RememberedCredentials
import com.example.services.RecoveredPasswordInfo
import com.example.shared.auth.GoogleSignInHelper
import com.example.shared.model.AppControlConfig
import com.example.shared.model.BetSelectionType
import com.example.shared.model.DynamicPaymentOrder
import com.example.shared.model.GameMode
import com.example.shared.model.GameRoundState
import com.example.shared.model.PaymentGatewayResult
import com.example.shared.model.PayoutMethod
import com.example.shared.model.PayoutMethodType
import com.example.shared.model.PeriodHistoryItem
import com.example.shared.model.RoundSettlementResult
import com.example.shared.model.StrategyTip
import com.example.shared.model.UserAccount
import com.example.shared.model.UserBet
import com.example.shared.model.UserSettings
import com.example.shared.model.WalletTransaction
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class KelywinViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as KelywinApplication
    private val sessionPreferences = app.sessionPreferences
    private val userFirebaseRepository = app.userFirebaseRepository
    private val authService = app.authService
    private val paymentGatewayService = app.paymentGatewayService
    private val periodEngine = app.periodEngine

    val firebaseUser: StateFlow<FirebaseUser?> = userFirebaseRepository.firebaseUserFlow

    private val _isGoogleAuthLoading = MutableStateFlow(false)
    val isGoogleAuthLoading: StateFlow<Boolean> = _isGoogleAuthLoading.asStateFlow()

    private val _googleAuthError = MutableStateFlow<String?>(null)
    val googleAuthError: StateFlow<String?> = _googleAuthError.asStateFlow()

    val currentUser: StateFlow<UserAccount?> = authService.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isSessionInitialized: StateFlow<Boolean> = authService.activeUserIdFlow
        .map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val rememberedCredentials: StateFlow<RememberedCredentials> = authService.rememberedCredentialsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, RememberedCredentials())

    val userSettings: StateFlow<UserSettings> = authService.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val appConfig: StateFlow<AppControlConfig> = app.database.appControlConfigDao()
        .observeConfig()
        .map { it?.toDomain() ?: AppControlConfig() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppControlConfig())

    val activeGameMode: StateFlow<GameMode> = sessionPreferences.activeGameModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameMode.SEC_30)

    val roundStates: StateFlow<Map<GameMode, GameRoundState>> = periodEngine.roundStates

    val sec30History: StateFlow<List<PeriodHistoryItem>> = periodEngine.observeHistory(GameMode.SEC_30)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val min1History: StateFlow<List<PeriodHistoryItem>> = periodEngine.observeHistory(GameMode.MIN_1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeHistory: StateFlow<List<PeriodHistoryItem>> = activeGameMode
        .flatMapLatest { mode -> periodEngine.observeHistory(mode) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val strategyTips: StateFlow<List<StrategyTip>> = activeHistory
        .map { periodEngine.computeStrategyTips(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userBets: StateFlow<List<UserBet>> = authService.activeUserIdFlow
        .flatMapLatest { uid ->
            if (uid == null) flowOf(emptyList()) else periodEngine.observeUserBets(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentRoundBets: StateFlow<List<UserBet>> = combine(
        authService.activeUserIdFlow,
        activeGameMode,
        roundStates
    ) { uid, mode, states ->
        Triple(uid, mode, states[mode]?.periodId)
    }.flatMapLatest { (uid, mode, pid) ->
        if (uid == null || pid == null) flowOf(emptyList())
        else periodEngine.observeRoundBets(uid, mode, pid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<WalletTransaction>> = authService.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payoutMethods: StateFlow<List<PayoutMethod>> = authService.payoutMethodsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMultiplier = MutableStateFlow(1)
    val selectedMultiplier: StateFlow<Int> = _selectedMultiplier.asStateFlow()

    private val _latestSettlementPopup = MutableStateFlow<RoundSettlementResult?>(null)
    val latestSettlementPopup: StateFlow<RoundSettlementResult?> = _latestSettlementPopup.asStateFlow()

    private val _latestDepositReceipt = MutableStateFlow<PaymentGatewayResult?>(null)
    val latestDepositReceipt: StateFlow<PaymentGatewayResult?> = _latestDepositReceipt.asStateFlow()

    private val _activePaymentOrder = MutableStateFlow<DynamicPaymentOrder?>(null)
    val activePaymentOrder: StateFlow<DynamicPaymentOrder?> = _activePaymentOrder.asStateFlow()

    private val _isCreatingGatewayOrder = MutableStateFlow(false)
    val isCreatingGatewayOrder: StateFlow<Boolean> = _isCreatingGatewayOrder.asStateFlow()

    private val _isVerifyingPayment = MutableStateFlow(false)
    val isVerifyingPayment: StateFlow<Boolean> = _isVerifyingPayment.asStateFlow()

    private var famGatewayPollingJob: Job? = null

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _recoveredPasswordInfo = MutableStateFlow<RecoveredPasswordInfo?>(null)
    val recoveredPasswordInfo: StateFlow<RecoveredPasswordInfo?> = _recoveredPasswordInfo.asStateFlow()

    private val _recoveryError = MutableStateFlow<String?>(null)
    val recoveryError: StateFlow<String?> = _recoveryError.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        periodEngine.bindActiveUserProvider { currentUser.value?.userId }

        viewModelScope.launch {
            periodEngine.settlementEvents.collect { settlement ->
                if (userSettings.value.showResultPopup) {
                    _latestSettlementPopup.value = settlement
                }
            }
        }

        viewModelScope.launch {
            val webClientId = runCatching { app.getString(R.string.default_web_client_id) }.getOrDefault("")
            if (userFirebaseRepository.currentFirebaseUid() == null && webClientId.isNotBlank()) {
                GoogleSignInHelper.attemptSilentSignIn(app, webClientId)
            }
        }

        viewModelScope.launch {
            firebaseUser.collect { fbUser ->
                if (fbUser != null) {
                    userFirebaseRepository.startSyncForUser(fbUser.uid)
                    userFirebaseRepository.fetchUserProfileEntity(fbUser.uid)
                } else {
                    userFirebaseRepository.stopSync()
                }
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _isGoogleAuthLoading.value = true
            _googleAuthError.value = null
            val webClientId = runCatching { app.getString(R.string.default_web_client_id) }.getOrDefault("")
            GoogleSignInHelper.signInWithGoogle(activityContext, webClientId)
                .onSuccess { user ->
                    userFirebaseRepository.startSyncForUser(user.uid)
                    userFirebaseRepository.fetchUserProfileEntity(user.uid)
                }
                .onFailure { err ->
                    if (err !is GetCredentialCancellationException) {
                        _googleAuthError.value = err.message ?: "Google Sign-In failed."
                    }
                }
            _isGoogleAuthLoading.value = false
        }
    }

    fun signOutGoogle(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            authService.logout()
            GoogleSignInHelper.signOut(context)
            onComplete()
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearRecoveredPassword() {
        _recoveredPasswordInfo.value = null
        _recoveryError.value = null
    }

    fun dismissSettlementPopup() {
        _latestSettlementPopup.value = null
    }

    fun dismissDepositReceipt() {
        _latestDepositReceipt.value = null
    }

    fun clearDynamicGatewayOrder() {
        famGatewayPollingJob?.cancel()
        famGatewayPollingJob = null
        _isVerifyingPayment.value = false
        _activePaymentOrder.value = null
    }

    fun setGameMode(mode: GameMode) {
        viewModelScope.launch {
            sessionPreferences.setActiveGameMode(mode)
        }
    }

    fun setMultiplier(multiplier: Int) {
        _selectedMultiplier.value = multiplier
    }

    fun registerNoOtp(
        countryCode: String,
        phone: String,
        password: String,
        confirmPassword: String,
        inviteCode: String,
        acceptedPrivacy: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _authError.value = null
            authService.registerNoOtp(
                countryCode = countryCode,
                phoneNumber = phone,
                password = password,
                confirmPassword = confirmPassword,
                inviteCode = inviteCode,
                acceptedPrivacy = acceptedPrivacy
            ).onSuccess { account ->
                _snackbarEvents.tryEmit(
                    "Welcome ${account.kwId}! Save your Secret Code (${account.secretCode}) in Settings."
                )
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: "Registration failed."
            }
        }
    }

    fun loginPhoneNoOtp(
        countryCode: String,
        phone: String,
        password: String,
        rememberPassword: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _authError.value = null
            authService.loginByPhoneNoOtp(
                countryCode = countryCode,
                phoneNumber = phone,
                password = password,
                rememberPassword = rememberPassword
            ).onSuccess {
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: "Login failed."
            }
        }
    }

    fun loginEmailNoOtp(
        email: String,
        password: String,
        rememberPassword: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _authError.value = null
            authService.loginByEmailNoOtp(
                email = email,
                password = password,
                rememberPassword = rememberPassword
            ).onSuccess {
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: "Login failed."
            }
        }
    }

    fun recoverPasswordWithSecretCode(identifier: String, secretCode: String) {
        viewModelScope.launch {
            _recoveryError.value = null
            authService.recoverPasswordWithSecretCode(identifier, secretCode)
                .onSuccess { info ->
                    _recoveredPasswordInfo.value = info
                }
                .onFailure { err ->
                    _recoveredPasswordInfo.value = null
                    _recoveryError.value = err.message ?: "Could not verify Secret Recovery Code."
                }
        }
    }

    fun submitForgotPasswordAdminRequest(identifier: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authService.submitPasswordResetRequestToAdmin(identifier)
                .onSuccess {
                    _snackbarEvents.tryEmit("Reset request sent to Admin Password Reset Queue.")
                    onSuccess()
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Could not submit request.")
                }
        }
    }

    fun changePasswordNoOtp(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        val uid = currentUser.value?.userId ?: return
        viewModelScope.launch {
            authService.changePasswordNoOtp(uid, currentPassword, newPassword, confirmPassword)
                .onSuccess {
                    _snackbarEvents.tryEmit("Password changed & synced with Secret Code recovery.")
                    onSuccess()
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Password update failed.")
                }
        }
    }

    fun linkEmail(email: String) {
        val uid = currentUser.value?.userId ?: return
        viewModelScope.launch {
            authService.updateEmail(uid, email)
                .onSuccess { _snackbarEvents.tryEmit("Email saved to your Kelywin profile.") }
                .onFailure { _snackbarEvents.tryEmit(it.message ?: "Invalid email.") }
        }
    }

    fun placeBet(
        selectionType: BetSelectionType,
        selectionValue: String,
        baseStake: Double,
        multiplier: Int,
        onSuccess: () -> Unit = {}
    ) {
        val uid = currentUser.value?.userId ?: return
        val mode = activeGameMode.value
        viewModelScope.launch {
            periodEngine.placeBet(
                userId = uid,
                mode = mode,
                selectionType = selectionType,
                selectionValue = selectionValue,
                baseStake = baseStake,
                multiplier = multiplier
            ).onSuccess { bet ->
                val stakeFmt = String.format(Locale.US, if (bet.totalStake % 1.0 == 0.0) "%.0f" else "%.2f", bet.totalStake)
                _snackbarEvents.tryEmit("Bet placed: ${bet.selectionValue} (₹$stakeFmt)")
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Unable to place bet.")
            }
        }
    }

    fun generateDynamicGatewayQr(amount: Double) {
        val uid = currentUser.value?.userId ?: return
        if (_isCreatingGatewayOrder.value) return
        viewModelScope.launch {
            _isCreatingGatewayOrder.value = true
            paymentGatewayService.createDynamicQrOrder(uid, amount)
                .onSuccess { order ->
                    _activePaymentOrder.value = order
                    if (order.isFamGateway) {
                        _snackbarEvents.tryEmit("FamGateway QR ready. Pay without UTR — wallet credits automatically!")
                        startFamGatewayPolling(order)
                    } else {
                        _snackbarEvents.tryEmit("Dynamic Gateway Order ${order.orderId} generated.")
                    }
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Unable to create gateway QR order.")
                }
            _isCreatingGatewayOrder.value = false
        }
    }

    fun checkFamGatewayPaymentNow() {
        val order = _activePaymentOrder.value ?: return
        if (!order.isFamGateway || _isVerifyingPayment.value) return
        viewModelScope.launch {
            _isVerifyingPayment.value = true
            pollFamGatewayOnce(order, userInitiated = true)
            _isVerifyingPayment.value = false
        }
    }

    private fun startFamGatewayPolling(order: DynamicPaymentOrder) {
        famGatewayPollingJob?.cancel()
        famGatewayPollingJob = viewModelScope.launch {
            while (isActive && _activePaymentOrder.value?.orderId == order.orderId) {
                delay(3500L)
                if (_activePaymentOrder.value?.orderId != order.orderId) break
                pollFamGatewayOnce(order, userInitiated = false)
            }
        }
    }

    private suspend fun pollFamGatewayOnce(order: DynamicPaymentOrder, userInitiated: Boolean) {
        val result = paymentGatewayService.verifyFamGatewayOrder(order.orderId)
        result.onSuccess { poll ->
            when (poll.status) {
                "SUCCESS" -> {
                    famGatewayPollingJob?.cancel()
                    famGatewayPollingJob = null
                    _activePaymentOrder.value = null
                    userFirebaseRepository.currentFirebaseUid()?.let { fbUid ->
                        userFirebaseRepository.fetchUserProfileEntity(fbUid)
                    }
                    val amt = if (poll.amount > 0.0) poll.amount else order.amount
                    val bonusMsg = if (poll.bonusApplied > 0.0) " + ₹${poll.bonusApplied.toInt()} 1st-deposit bonus!" else "!"
                    _latestDepositReceipt.value = PaymentGatewayResult(
                        status = "SUCCESS",
                        referenceId = order.orderId,
                        orderId = order.orderId,
                        upiId = order.assignedUpiId,
                        utrNumber = poll.utrNumber.ifBlank { "AUTO-VERIFIED" },
                        amount = amt,
                        message = "Payment verified automatically! ₹${String.format(Locale.US, "%.2f", amt)} added to wallet$bonusMsg",
                        createdAtMillis = System.currentTimeMillis()
                    )
                    _snackbarEvents.tryEmit("Deposit ₹${amt.toInt()} credited automatically!")
                }
                "FAILED" -> {
                    famGatewayPollingJob?.cancel()
                    famGatewayPollingJob = null
                    _activePaymentOrder.value = null
                    _latestDepositReceipt.value = PaymentGatewayResult(
                        status = "FAILED",
                        referenceId = order.orderId,
                        orderId = order.orderId,
                        upiId = order.assignedUpiId,
                        utrNumber = "NONE",
                        amount = order.amount,
                        message = poll.message.ifBlank { "Payment failed or expired. Zero balance credited." },
                        createdAtMillis = System.currentTimeMillis()
                    )
                    _snackbarEvents.tryEmit("Payment failed or expired. Zero deposit added.")
                }
                else -> {
                    if (userInitiated) {
                        _snackbarEvents.tryEmit("Payment not received yet. Complete UPI transfer and it will verify automatically.")
                    }
                }
            }
        }.onFailure { err ->
            if (userInitiated) {
                _snackbarEvents.tryEmit(err.message ?: "Unable to reach gateway right now.")
            }
        }
    }

    fun submitUpiQrDeposit(amount: Double, utrNumber: String, onSuccess: () -> Unit) {
        val user = currentUser.value ?: return
        val order = _activePaymentOrder.value
        viewModelScope.launch {
            paymentGatewayService.submitUpiQrDeposit(
                userId = user.userId,
                userPhone = user.displayPhone,
                amount = order?.amount ?: amount,
                utrNumber = utrNumber,
                orderId = order?.orderId.orEmpty(),
                assignedUpiId = order?.assignedUpiId.orEmpty()
            ).onSuccess { receipt ->
                clearDynamicGatewayOrder()
                _latestDepositReceipt.value = receipt
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "UPI deposit failed.")
            }
        }
    }

    fun savePayoutMethodNoOtp(
        existingId: String?,
        type: PayoutMethodType,
        label: String,
        accountHolder: String,
        accountIdentifier: String,
        ifscCode: String,
        makeDefault: Boolean,
        onSuccess: () -> Unit
    ) {
        val uid = currentUser.value?.userId ?: return
        viewModelScope.launch {
            authService.savePayoutMethodNoOtp(
                userId = uid,
                existingId = existingId,
                type = type,
                label = label,
                accountHolder = accountHolder,
                accountIdentifier = accountIdentifier,
                ifscCode = ifscCode,
                makeDefault = makeDefault
            ).onSuccess {
                _snackbarEvents.tryEmit("${type.label} saved without OTP.")
                onSuccess()
            }.onFailure { err ->
                _snackbarEvents.tryEmit(err.message ?: "Could not save payout method.")
            }
        }
    }

    fun selectDefaultPayoutMethod(methodId: String) {
        val uid = currentUser.value?.userId ?: return
        viewModelScope.launch {
            authService.selectDefaultPayoutMethod(uid, methodId)
        }
    }

    fun deletePayoutMethod(methodId: String) {
        val uid = currentUser.value?.userId ?: return
        viewModelScope.launch {
            authService.deletePayoutMethod(uid, methodId)
            _snackbarEvents.tryEmit("Payout account removed.")
        }
    }

    fun submitWithdrawal(amount: Double, payoutMethod: PayoutMethod, onSuccess: () -> Unit) {
        val uid = currentUser.value?.userId ?: return
        viewModelScope.launch {
            authService.submitWithdrawal(uid, amount, payoutMethod)
                .onSuccess {
                    _snackbarEvents.tryEmit("Withdrawal of ₹${String.format(Locale.US, "%.2f", amount)} submitted.")
                    onSuccess()
                }
                .onFailure { err ->
                    _snackbarEvents.tryEmit(err.message ?: "Withdrawal failed.")
                }
        }
    }

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            authService.updateSettings(settings)
        }
    }

    fun notifyMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            clearDynamicGatewayOrder()
            authService.logout()
            onComplete()
        }
    }

    override fun onCleared() {
        famGatewayPollingJob?.cancel()
        super.onCleared()
    }
}
