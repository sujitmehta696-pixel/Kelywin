package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.shared.model.AppControlConfig
import com.example.shared.model.BetColor
import com.example.shared.model.BetSelectionType
import com.example.shared.model.BetStatus
import com.example.shared.model.DepositRequest
import com.example.shared.model.DragonTigerSide
import com.example.shared.model.GameControlMode
import com.example.shared.model.GameRules
import com.example.shared.model.PasswordResetRequest
import com.example.shared.model.PayoutMethod
import com.example.shared.model.PayoutMethodType
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.PeriodHistoryItem
import com.example.shared.model.RequestStatus
import com.example.shared.model.RoomControlConfig
import com.example.shared.model.RoundAdminLog
import com.example.shared.model.TransactionCategory
import com.example.shared.model.TransactionStatus
import com.example.shared.model.UpiQrConfig
import com.example.shared.model.UserAccount
import com.example.shared.model.UserBet
import com.example.shared.model.WalletTransaction
import com.example.shared.model.WithdrawalRequest
import com.example.shared.model.XinLogicPattern

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val kwId: String = userId,
    val countryCode: String,
    val phoneNumber: String,
    val email: String,
    val passwordHash: String,
    val encryptedPassword: String = "",
    val secretCode: String = "",
    val secretCodeHash: String = "",
    val inviteCode: String,
    val balance: Double,
    val hasReceivedFirstDepositBonus: Boolean = false,
    val isBlocked: Boolean = false,
    val createdAtMillis: Long,
    val lastLoginAtMillis: Long = createdAtMillis
) {
    fun toDomain(): UserAccount = UserAccount(
        userId = userId,
        kwId = kwId.ifBlank { userId },
        countryCode = countryCode,
        phoneNumber = phoneNumber,
        email = email,
        secretCode = secretCode,
        inviteCode = inviteCode,
        balance = balance,
        hasReceivedFirstDepositBonus = hasReceivedFirstDepositBonus,
        isBlocked = isBlocked,
        createdAtMillis = createdAtMillis,
        lastLoginAtMillis = lastLoginAtMillis
    )
}

@Entity(tableName = "payout_methods")
data class PayoutMethodEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val label: String,
    val accountHolder: String,
    val accountIdentifier: String,
    val ifscCode: String,
    val isDefault: Boolean,
    val updatedAtMillis: Long
) {
    fun toDomain(): PayoutMethod = PayoutMethod(
        id = id,
        userId = userId,
        type = runCatching { PayoutMethodType.valueOf(type) }.getOrDefault(PayoutMethodType.UPI),
        label = label,
        accountHolder = accountHolder,
        accountIdentifier = accountIdentifier,
        ifscCode = ifscCode,
        isDefault = isDefault,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val status: String,
    val createdAtMillis: Long
) {
    fun toDomain(): WalletTransaction = WalletTransaction(
        id = id,
        userId = userId,
        category = runCatching { TransactionCategory.valueOf(category) }.getOrDefault(TransactionCategory.ADJUSTMENT),
        title = title,
        subtitle = subtitle,
        amount = amount,
        status = runCatching { TransactionStatus.valueOf(status) }.getOrDefault(TransactionStatus.COMPLETED),
        createdAtMillis = createdAtMillis
    )
}

@Entity(tableName = "period_history", primaryKeys = ["periodId", "modeId"])
data class PeriodHistoryEntity(
    val periodId: String,
    val modeId: String,
    val winningNumber: Int = 7,
    val winningColor: String,
    val winningSide: String,
    val controlModeUsed: String,
    val timestampMillis: Long
) {
    fun toDomain(): PeriodHistoryItem = PeriodHistoryItem(
        periodId = periodId,
        modeId = modeId,
        number = winningNumber,
        colors = GameRules.colorsForNumber(winningNumber),
        side = GameRules.sideForNumber(winningNumber),
        controlModeUsed = runCatching { GameControlMode.valueOf(controlModeUsed) }.getOrDefault(GameControlMode.RANDOM),
        timestampMillis = timestampMillis
    )
}

@Entity(tableName = "user_bets")
data class UserBetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val periodId: String,
    val modeId: String,
    val selectionType: String,
    val selectionValue: String,
    val baseStake: Double,
    val multiplier: Int,
    val totalStake: Double,
    val status: String,
    val payout: Double,
    val createdAtMillis: Long
) {
    fun toDomain(): UserBet = UserBet(
        id = id,
        userId = userId,
        periodId = periodId,
        modeId = modeId,
        selectionType = runCatching { BetSelectionType.valueOf(selectionType) }.getOrDefault(BetSelectionType.COLOR),
        selectionValue = selectionValue,
        baseStake = baseStake,
        multiplier = multiplier,
        totalStake = totalStake,
        status = runCatching { BetStatus.valueOf(status) }.getOrDefault(BetStatus.PENDING),
        payout = payout,
        createdAtMillis = createdAtMillis
    )
}

@Entity(tableName = "room_control_config")
data class RoomControlConfigEntity(
    @PrimaryKey val modeId: String,
    val controlMode: String,
    val fallbackMode: String = GameControlMode.RANDOM.name,
    val xinPattern: String = XinLogicPattern.TWO_HIGH_FOUR_LOW.name,
    val xinAnchorPeriodIndex: Int = 1,
    val manualNumber: Int = 7,
    val manualTargetPeriodId: String? = null,
    val manualColor: String = BetColor.GREEN.name,
    val manualSide: String = DragonTigerSide.DRAGON.name,
    val manualOverrideActive: Boolean = false
) {
    fun toDomain(): RoomControlConfig = RoomControlConfig(
        modeId = modeId,
        controlMode = runCatching { GameControlMode.valueOf(controlMode) }.getOrDefault(GameControlMode.RANDOM),
        fallbackMode = runCatching { GameControlMode.valueOf(fallbackMode) }.getOrDefault(GameControlMode.RANDOM),
        xinPattern = runCatching { XinLogicPattern.valueOf(xinPattern) }.getOrDefault(XinLogicPattern.TWO_HIGH_FOUR_LOW),
        xinAnchorPeriodIndex = xinAnchorPeriodIndex.coerceAtLeast(0),
        manualNumber = manualNumber.coerceIn(0, 9),
        manualTargetPeriodId = manualTargetPeriodId
    )
}

@Entity(tableName = "round_admin_logs", primaryKeys = ["periodId", "modeId"])
data class RoundAdminLogEntity(
    val periodId: String,
    val modeId: String,
    val controlModeUsed: String,
    val winningNumber: Int = 7,
    val winningColor: String,
    val winningSide: String,
    val totalBetsAmount: Double,
    val totalPayoutAmount: Double,
    val netProfit: Double,
    val settledAtMillis: Long
) {
    fun toDomain(): RoundAdminLog = RoundAdminLog(
        periodId = periodId,
        modeId = modeId,
        controlModeUsed = runCatching { GameControlMode.valueOf(controlModeUsed) }.getOrDefault(GameControlMode.RANDOM),
        winningNumber = winningNumber,
        winningColor = runCatching { BetColor.valueOf(winningColor) }.getOrDefault(BetColor.GREEN),
        winningSide = runCatching { DragonTigerSide.valueOf(winningSide) }.getOrDefault(DragonTigerSide.DRAGON),
        totalBetsAmount = totalBetsAmount,
        totalPayoutAmount = totalPayoutAmount,
        netProfit = netProfit,
        settledAtMillis = settledAtMillis
    )
}

@Entity(tableName = "deposit_requests")
data class DepositRequestEntity(
    @PrimaryKey val id: String,
    val orderId: String = id,
    val userId: String,
    val userPhone: String,
    val amount: Double,
    val utrNumber: String,
    val upiIdUsed: String,
    val gatewayName: String = "Kelywin Dynamic UPI Gateway",
    val status: String,
    val bonusApplied: Double,
    val adminNote: String,
    val createdAtMillis: Long,
    val processedAtMillis: Long?
) {
    fun toDomain(): DepositRequest = DepositRequest(
        id = id,
        orderId = orderId,
        userId = userId,
        userPhone = userPhone,
        amount = amount,
        utrNumber = utrNumber,
        upiIdUsed = upiIdUsed,
        gatewayName = gatewayName,
        status = runCatching { RequestStatus.valueOf(status) }.getOrDefault(RequestStatus.PENDING),
        bonusApplied = bonusApplied,
        adminNote = adminNote,
        createdAtMillis = createdAtMillis,
        processedAtMillis = processedAtMillis
    )
}

@Entity(tableName = "withdrawal_requests")
data class WithdrawalRequestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userPhone: String,
    val amount: Double,
    val methodType: String,
    val methodLabel: String,
    val accountHolder: String,
    val accountIdentifier: String,
    val ifscCode: String,
    val status: String,
    val adminNote: String,
    val createdAtMillis: Long,
    val processedAtMillis: Long?
) {
    fun toDomain(): WithdrawalRequest = WithdrawalRequest(
        id = id,
        userId = userId,
        userPhone = userPhone,
        amount = amount,
        methodType = runCatching { PayoutMethodType.valueOf(methodType) }.getOrDefault(PayoutMethodType.UPI),
        methodLabel = methodLabel,
        accountHolder = accountHolder,
        accountIdentifier = accountIdentifier,
        ifscCode = ifscCode,
        status = runCatching { RequestStatus.valueOf(status) }.getOrDefault(RequestStatus.PENDING),
        adminNote = adminNote,
        createdAtMillis = createdAtMillis,
        processedAtMillis = processedAtMillis
    )
}

@Entity(tableName = "password_reset_requests")
data class PasswordResetRequestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val identifier: String,
    val status: String,
    val assignedPassword: String,
    val deliveryChannel: String,
    val createdAtMillis: Long,
    val resolvedAtMillis: Long?
) {
    fun toDomain(): PasswordResetRequest = PasswordResetRequest(
        id = id,
        userId = userId,
        identifier = identifier,
        status = runCatching { RequestStatus.valueOf(status) }.getOrDefault(RequestStatus.PENDING),
        assignedPassword = assignedPassword,
        deliveryChannel = deliveryChannel,
        createdAtMillis = createdAtMillis,
        resolvedAtMillis = resolvedAtMillis
    )
}

@Entity(tableName = "app_control_config")
data class AppControlConfigEntity(
    @PrimaryKey val id: Int = 1,
    val announcementText: String,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Kelywin servers are undergoing scheduled maintenance. Please check back shortly.",
    val enableRegistration: Boolean = true,
    val enableDeposits: Boolean = true,
    val enableWithdrawals: Boolean = true,
    val enableNumberBetting: Boolean = true,
    val firstDepositBonusAmount: Double,
    val minBetAmount: Double = 1.0,
    val greenRedMultiplier: Double,
    val violetMultiplier: Double,
    val numberMultiplier: Double = 9.0,
    val dragonTigerMultiplier: Double,
    val platformFeeRate: Double,
    val gatewayName: String = "Kelywin Dynamic UPI Gateway",
    val gatewayApiUrl: String = "",
    val merchantKeyId: String = "",
    val upiId: String,
    val merchantName: String,
    val qrLabel: String,
    val minDeposit: Int,
    val maxDeposit: Int,
    val apiBaseUrl: String,
    val telegramWebhookUrl: String,
    val emailWebhookUrl: String,
    val smsWebhookUrl: String
) {
    fun toDomain(): AppControlConfig = AppControlConfig(
        announcementText = announcementText,
        maintenanceMode = maintenanceMode,
        maintenanceMessage = maintenanceMessage,
        enableRegistration = enableRegistration,
        enableDeposits = enableDeposits,
        enableWithdrawals = enableWithdrawals,
        enableNumberBetting = enableNumberBetting,
        firstDepositBonusAmount = firstDepositBonusAmount,
        minBetAmount = minBetAmount.coerceAtLeast(1.0),
        multipliers = PayoutMultipliers(
            greenRedMultiplier = greenRedMultiplier,
            violetMultiplier = violetMultiplier,
            numberMultiplier = numberMultiplier,
            dragonTigerMultiplier = dragonTigerMultiplier,
            platformFeeRate = platformFeeRate
        ),
        upiQrConfig = UpiQrConfig(
            gatewayName = gatewayName,
            gatewayApiUrl = gatewayApiUrl,
            merchantKeyId = merchantKeyId,
            merchantUpiPool = upiId,
            merchantName = merchantName,
            qrLabel = qrLabel,
            minDeposit = minDeposit,
            maxDeposit = maxDeposit
        ),
        apiBaseUrl = apiBaseUrl,
        telegramWebhookUrl = telegramWebhookUrl,
        emailWebhookUrl = emailWebhookUrl,
        smsWebhookUrl = smsWebhookUrl
    )
}
