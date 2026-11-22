package com.example.shared.model

enum class AuthTab {
    PHONE,
    EMAIL
}

enum class TransactionCategory(val label: String) {
    DEPOSIT("Deposit"),
    BET("Bet"),
    WIN("Win"),
    WITHDRAWAL("Withdrawal"),
    BONUS("Bonus"),
    ADJUSTMENT("Adjustment")
}

enum class TransactionStatus(val label: String) {
    COMPLETED("Completed"),
    PENDING_GATEWAY_INTEGRATION("Pending Approval"),
    SUBMITTED("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

enum class PayoutMethodType(val label: String) {
    UPI("UPI ID"),
    BANK("Bank Account")
}

enum class RequestStatus(val label: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

data class UserAccount(
    val userId: String,
    val kwId: String = userId,
    val countryCode: String,
    val phoneNumber: String,
    val email: String,
    val secretCode: String = "",
    val inviteCode: String,
    val balance: Double,
    val hasReceivedFirstDepositBonus: Boolean,
    val isBlocked: Boolean,
    val createdAtMillis: Long,
    val lastLoginAtMillis: Long = createdAtMillis
) {
    val displayPhone: String
        get() = if (phoneNumber.isNotBlank()) "$countryCode $phoneNumber" else email.ifBlank { kwId }
}

data class PayoutMethod(
    val id: String,
    val userId: String,
    val type: PayoutMethodType,
    val label: String,
    val accountHolder: String,
    val accountIdentifier: String,
    val ifscCode: String = "",
    val isDefault: Boolean = false,
    val updatedAtMillis: Long
)

data class WalletTransaction(
    val id: String,
    val userId: String,
    val category: TransactionCategory,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val status: TransactionStatus,
    val createdAtMillis: Long
)

data class UpiQrConfig(
    val gatewayName: String = "Kelywin Dynamic UPI Gateway",
    val gatewayApiUrl: String = "",
    val merchantKeyId: String = "",
    val merchantUpiPool: String = "kelywin.gw1@okaxis,kelywin.gw2@ybl,kelywin.gw3@okicici",
    val merchantName: String = "Kelywin Payments",
    val qrLabel: String = "Dynamic amount-locked UPI Gateway QR (unique per transaction)",
    val minDeposit: Int = 100,
    val maxDeposit: Int = 50000
)

data class DynamicPaymentOrder(
    val orderId: String,
    val amount: Double,
    val gatewayName: String,
    val assignedUpiId: String,
    val upiIntentUri: String,
    val isLiveGatewayApi: Boolean,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val checkoutUrl: String = "",
    val qrUrl: String = "",
    val isFamGateway: Boolean = false
)

data class AdminGatewaySecret(
    val provider: String = "FAMGATEWAY",
    val apiKey: String = "",
    val backendUrl: String = "",
    val enabled: Boolean = true,
    val updatedByUid: String = "",
    val updatedAtMillis: Long = 0L
)

data class DepositRequest(
    val id: String,
    val orderId: String,
    val userId: String,
    val userPhone: String,
    val amount: Double,
    val utrNumber: String,
    val upiIdUsed: String,
    val gatewayName: String,
    val status: RequestStatus,
    val bonusApplied: Double,
    val adminNote: String,
    val createdAtMillis: Long,
    val processedAtMillis: Long?
)

data class WithdrawalRequest(
    val id: String,
    val userId: String,
    val userPhone: String,
    val amount: Double,
    val methodType: PayoutMethodType,
    val methodLabel: String,
    val accountHolder: String,
    val accountIdentifier: String,
    val ifscCode: String,
    val status: RequestStatus,
    val adminNote: String,
    val createdAtMillis: Long,
    val processedAtMillis: Long?
)

data class PasswordResetRequest(
    val id: String,
    val userId: String,
    val identifier: String,
    val status: RequestStatus,
    val assignedPassword: String,
    val deliveryChannel: String,
    val createdAtMillis: Long,
    val resolvedAtMillis: Long?
)

data class PaymentGatewayResult(
    val status: String,
    val referenceId: String,
    val orderId: String,
    val upiId: String,
    val utrNumber: String,
    val amount: Double,
    val message: String,
    val createdAtMillis: Long
)

data class UserSettings(
    val showResultPopup: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val compactGridMode: Boolean = false
)

data class RoomControlConfig(
    val modeId: String,
    val controlMode: GameControlMode = GameControlMode.RANDOM,
    val fallbackMode: GameControlMode = GameControlMode.RANDOM,
    val xinPattern: XinLogicPattern = XinLogicPattern.TWO_HIGH_FOUR_LOW,
    val xinAnchorPeriodIndex: Int = 1,
    val manualNumber: Int = 7,
    val manualTargetPeriodId: String? = null
)

data class RoundResultRecord(
    val modeId: String,
    val periodId: String,
    val winningNumber: Int,
    val winningColor: BetColor,
    val winningSide: DragonTigerSide,
    val controlModeUsed: GameControlMode,
    val settledAtMillis: Long
)

data class RoundAdminLog(
    val periodId: String,
    val modeId: String,
    val controlModeUsed: GameControlMode,
    val winningNumber: Int,
    val winningColor: BetColor,
    val winningSide: DragonTigerSide,
    val totalBetsAmount: Double,
    val totalPayoutAmount: Double,
    val netProfit: Double,
    val settledAtMillis: Long
)

data class AppControlConfig(
    val announcementText: String = "Only deposit via official Kelywin Dynamic UPI Gateway. First deposit unlocks +₹12 instant bonus! Live 24x7 30sec & 1Min rooms.",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Kelywin servers are undergoing scheduled maintenance. Please check back shortly.",
    val enableRegistration: Boolean = true,
    val enableDeposits: Boolean = true,
    val enableWithdrawals: Boolean = true,
    val enableNumberBetting: Boolean = true,
    val firstDepositBonusAmount: Double = 12.0,
    val minBetAmount: Double = 1.0,
    val multipliers: PayoutMultipliers = PayoutMultipliers(),
    val upiQrConfig: UpiQrConfig = UpiQrConfig(),
    val apiBaseUrl: String = "https://api.kelywin.local/v1",
    val telegramWebhookUrl: String = "",
    val emailWebhookUrl: String = "",
    val smsWebhookUrl: String = ""
)

data class RoundBetPool(
    val greenStake: Double = 0.0,
    val violetStake: Double = 0.0,
    val redStake: Double = 0.0,
    val dragonStake: Double = 0.0,
    val tigerStake: Double = 0.0,
    val numberStakes: Map<Int, Double> = emptyMap(),
    val totalBetCount: Int = 0
) {
    val numbersTotalStake: Double get() = numberStakes.values.sum()
    val totalPool: Double get() = greenStake + violetStake + redStake + dragonStake + tigerStake + numbersTotalStake
}

data class AdminDashboardStats(
    val totalUsers: Int = 0,
    val activePlayers: Int = 0,
    val totalBetsVolume: Double = 0.0,
    val totalPayouts: Double = 0.0,
    val netMargin: Double = 0.0,
    val pendingDeposits: Int = 0,
    val pendingWithdrawals: Int = 0,
    val pendingPasswordResets: Int = 0
)

data class AdminMember(
    val uid: String,
    val email: String,
    val role: String = "ADMIN",
    val createdAtMillis: Long = 0L
)
