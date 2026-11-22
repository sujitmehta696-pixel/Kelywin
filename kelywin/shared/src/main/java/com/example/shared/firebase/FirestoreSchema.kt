package com.example.shared.firebase

import android.content.Context
import com.example.shared.model.AdminGatewaySecret
import com.example.shared.model.AdminMember
import com.example.shared.model.AppControlConfig
import com.example.shared.model.BetColor
import com.example.shared.model.BetSelectionType
import com.example.shared.model.BetStatus
import com.example.shared.model.DepositRequest
import com.example.shared.model.DragonTigerSide
import com.example.shared.model.GameControlMode
import com.example.shared.model.PasswordResetRequest
import com.example.shared.model.PayoutMethod
import com.example.shared.model.PayoutMethodType
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.RequestStatus
import com.example.shared.model.RoomControlConfig
import com.example.shared.model.RoundAdminLog
import com.example.shared.model.RoundResultRecord
import com.example.shared.model.TransactionCategory
import com.example.shared.model.TransactionStatus
import com.example.shared.model.UpiQrConfig
import com.example.shared.model.UserAccount
import com.example.shared.model.UserBet
import com.example.shared.model.WalletTransaction
import com.example.shared.model.WithdrawalRequest
import com.example.shared.model.XinLogicPattern
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

object FirestoreCollections {
    const val ADMINS = "admins"
    const val ADMIN_SECRETS = "admin_secrets"
    const val FAMGATEWAY_SECRET_DOC = "famgateway"
    const val APP_CONFIG = "app_config"
    const val GLOBAL_CONFIG_DOC = "global"
    const val ROOM_CONTROLS = "room_controls"
    const val ROUND_RESULTS = "round_results"
    const val USERS = "users"
    const val PAYOUT_METHODS = "payout_methods"
    const val TRANSACTIONS = "transactions"
    const val BETS = "bets"
    const val DEPOSITS = "deposits"
    const val WITHDRAWALS = "withdrawals"
    const val PASSWORD_RESETS = "password_resets"
    const val ROUND_LOGS = "round_logs"
}

object FirestoreProvider {
    fun getDatabase(context: Context, databaseIdResId: Int): FirebaseFirestore {
        val dbId = context.getString(databaseIdResId)
        return FirebaseFirestore.getInstance(dbId)
    }
}

object FirestoreErrorMapper {
    fun userMessage(error: Throwable?): String {
        val fsErr = error as? FirebaseFirestoreException
        return when (fsErr?.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Access denied by security rules. Please verify your signed-in Google account."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Network unavailable. Changes will sync automatically once reconnected."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "Requested cloud record was not found."
            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "Account state changed; please refresh and retry."
            else -> error?.message ?: "Cloud synchronization error."
        }
    }
}

object FirestoreMappers {

    fun appConfigToMap(config: AppControlConfig): Map<String, Any?> = mapOf(
        "announcementText" to config.announcementText,
        "maintenanceMode" to config.maintenanceMode,
        "maintenanceMessage" to config.maintenanceMessage,
        "enableRegistration" to config.enableRegistration,
        "enableDeposits" to config.enableDeposits,
        "enableWithdrawals" to config.enableWithdrawals,
        "enableNumberBetting" to config.enableNumberBetting,
        "firstDepositBonusAmount" to config.firstDepositBonusAmount,
        "minBetAmount" to config.minBetAmount.coerceAtLeast(1.0),
        "greenRedMultiplier" to config.multipliers.greenRedMultiplier,
        "violetMultiplier" to config.multipliers.violetMultiplier,
        "numberMultiplier" to config.multipliers.numberMultiplier,
        "dragonTigerMultiplier" to config.multipliers.dragonTigerMultiplier,
        "platformFeeRate" to config.multipliers.platformFeeRate,
        "gatewayName" to config.upiQrConfig.gatewayName,
        "gatewayApiUrl" to config.upiQrConfig.gatewayApiUrl,
        "merchantKeyId" to config.upiQrConfig.merchantKeyId,
        "merchantUpiPool" to config.upiQrConfig.merchantUpiPool,
        "merchantName" to config.upiQrConfig.merchantName,
        "qrLabel" to config.upiQrConfig.qrLabel,
        "minDeposit" to config.upiQrConfig.minDeposit,
        "maxDeposit" to config.upiQrConfig.maxDeposit,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun appConfigFromSnapshot(doc: DocumentSnapshot): AppControlConfig? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        val defaults = AppControlConfig()
        return AppControlConfig(
            announcementText = (d["announcementText"] as? String) ?: defaults.announcementText,
            maintenanceMode = (d["maintenanceMode"] as? Boolean) ?: false,
            maintenanceMessage = (d["maintenanceMessage"] as? String) ?: defaults.maintenanceMessage,
            enableRegistration = (d["enableRegistration"] as? Boolean) ?: true,
            enableDeposits = (d["enableDeposits"] as? Boolean) ?: true,
            enableWithdrawals = (d["enableWithdrawals"] as? Boolean) ?: true,
            enableNumberBetting = (d["enableNumberBetting"] as? Boolean) ?: true,
            firstDepositBonusAmount = (d["firstDepositBonusAmount"] as? Number)?.toDouble() ?: defaults.firstDepositBonusAmount,
            minBetAmount = ((d["minBetAmount"] as? Number)?.toDouble() ?: defaults.minBetAmount).coerceAtLeast(1.0),
            multipliers = PayoutMultipliers(
                greenRedMultiplier = (d["greenRedMultiplier"] as? Number)?.toDouble() ?: 2.0,
                violetMultiplier = (d["violetMultiplier"] as? Number)?.toDouble() ?: 4.5,
                numberMultiplier = (d["numberMultiplier"] as? Number)?.toDouble() ?: 9.0,
                dragonTigerMultiplier = (d["dragonTigerMultiplier"] as? Number)?.toDouble() ?: 2.0,
                platformFeeRate = (d["platformFeeRate"] as? Number)?.toDouble() ?: 0.02
            ),
            upiQrConfig = UpiQrConfig(
                gatewayName = (d["gatewayName"] as? String) ?: defaults.upiQrConfig.gatewayName,
                gatewayApiUrl = (d["gatewayApiUrl"] as? String) ?: "",
                merchantKeyId = (d["merchantKeyId"] as? String) ?: "",
                merchantUpiPool = (d["merchantUpiPool"] as? String) ?: defaults.upiQrConfig.merchantUpiPool,
                merchantName = (d["merchantName"] as? String) ?: defaults.upiQrConfig.merchantName,
                qrLabel = (d["qrLabel"] as? String) ?: defaults.upiQrConfig.qrLabel,
                minDeposit = (d["minDeposit"] as? Number)?.toInt() ?: 100,
                maxDeposit = (d["maxDeposit"] as? Number)?.toInt() ?: 50000
            )
        )
    }

    fun roomControlToMap(config: RoomControlConfig): Map<String, Any?> = mapOf(
        "modeId" to config.modeId,
        "controlMode" to config.controlMode.name,
        "fallbackMode" to (if (config.fallbackMode == GameControlMode.MANUAL) GameControlMode.RANDOM else config.fallbackMode).name,
        "xinPattern" to config.xinPattern.name,
        "xinAnchorPeriodIndex" to config.xinAnchorPeriodIndex.coerceAtLeast(0),
        "manualNumber" to config.manualNumber.coerceIn(0, 9),
        "manualTargetPeriodId" to config.manualTargetPeriodId,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun roomControlFromSnapshot(doc: DocumentSnapshot): RoomControlConfig? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        val modeId = (d["modeId"] as? String) ?: doc.id
        return RoomControlConfig(
            modeId = modeId,
            controlMode = enumOrDefault(d["controlMode"] as? String, GameControlMode.RANDOM),
            fallbackMode = enumOrDefault(d["fallbackMode"] as? String, GameControlMode.RANDOM),
            xinPattern = enumOrDefault(d["xinPattern"] as? String, XinLogicPattern.TWO_HIGH_FOUR_LOW),
            xinAnchorPeriodIndex = ((d["xinAnchorPeriodIndex"] as? Number)?.toInt() ?: 1).coerceAtLeast(0),
            manualNumber = ((d["manualNumber"] as? Number)?.toInt() ?: 7).coerceIn(0, 9),
            manualTargetPeriodId = d["manualTargetPeriodId"] as? String
        )
    }

    fun roundResultToMap(record: RoundResultRecord): Map<String, Any?> = mapOf(
        "modeId" to record.modeId,
        "periodId" to record.periodId,
        "winningNumber" to record.winningNumber.coerceIn(0, 9),
        "winningColor" to record.winningColor.name,
        "winningSide" to record.winningSide.name,
        "controlModeUsed" to record.controlModeUsed.name,
        "settledAt" to FieldValue.serverTimestamp()
    )

    fun roundResultFromSnapshot(doc: DocumentSnapshot): RoundResultRecord? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        val num = ((d["winningNumber"] as? Number)?.toInt() ?: return null).coerceIn(0, 9)
        val ts = (d["settledAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        return RoundResultRecord(
            modeId = (d["modeId"] as? String) ?: return null,
            periodId = (d["periodId"] as? String) ?: return null,
            winningNumber = num,
            winningColor = enumOrDefault(d["winningColor"] as? String, BetColor.GREEN),
            winningSide = enumOrDefault(d["winningSide"] as? String, DragonTigerSide.DRAGON),
            controlModeUsed = enumOrDefault(d["controlModeUsed"] as? String, GameControlMode.RANDOM),
            settledAtMillis = ts
        )
    }

    fun userCreateMap(
        userId: String,
        kwId: String,
        countryCode: String,
        phoneNumber: String,
        email: String,
        passwordHash: String,
        encryptedPassword: String,
        secretCode: String,
        secretCodeHash: String,
        inviteCode: String,
        lastLoginAtMillis: Long
    ): Map<String, Any?> = mapOf(
        "userId" to userId,
        "kwId" to kwId,
        "countryCode" to countryCode,
        "phoneNumber" to phoneNumber,
        "email" to email,
        "passwordHash" to passwordHash,
        "encryptedPassword" to encryptedPassword,
        "secretCode" to secretCode,
        "secretCodeHash" to secretCodeHash,
        "inviteCode" to inviteCode,
        "balance" to 0.0,
        "hasReceivedFirstDepositBonus" to false,
        "blocked" to false,
        "lastLoginAtMillis" to lastLoginAtMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun userFromSnapshot(doc: DocumentSnapshot): UserAccount? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        val createdMillis = (d["createdAt"] as? Timestamp)?.toDate()?.time
            ?: (d["lastLoginAtMillis"] as? Number)?.toLong()
            ?: System.currentTimeMillis()
        val uid = (d["userId"] as? String) ?: doc.id
        return UserAccount(
            userId = uid,
            kwId = (d["kwId"] as? String) ?: uid,
            countryCode = (d["countryCode"] as? String) ?: "+91",
            phoneNumber = (d["phoneNumber"] as? String) ?: "",
            email = (d["email"] as? String) ?: "",
            secretCode = (d["secretCode"] as? String) ?: "",
            inviteCode = (d["inviteCode"] as? String) ?: "",
            balance = (d["balance"] as? Number)?.toDouble() ?: 0.0,
            hasReceivedFirstDepositBonus = (d["hasReceivedFirstDepositBonus"] as? Boolean) ?: false,
            isBlocked = (d["blocked"] as? Boolean) ?: false,
            createdAtMillis = createdMillis,
            lastLoginAtMillis = (d["lastLoginAtMillis"] as? Number)?.toLong() ?: createdMillis
        )
    }

    fun payoutMethodToCreateMap(method: PayoutMethod): Map<String, Any?> = mapOf(
        "userId" to method.userId,
        "type" to method.type.name,
        "label" to method.label,
        "accountHolder" to method.accountHolder,
        "accountIdentifier" to method.accountIdentifier,
        "ifscCode" to method.ifscCode,
        "isDefault" to method.isDefault,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun payoutMethodFromSnapshot(doc: DocumentSnapshot): PayoutMethod? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        val ts = (d["updatedAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        return PayoutMethod(
            id = doc.id,
            userId = (d["userId"] as? String) ?: return null,
            type = enumOrDefault(d["type"] as? String, PayoutMethodType.UPI),
            label = (d["label"] as? String) ?: "UPI",
            accountHolder = (d["accountHolder"] as? String) ?: "",
            accountIdentifier = (d["accountIdentifier"] as? String) ?: "",
            ifscCode = (d["ifscCode"] as? String) ?: "",
            isDefault = (d["isDefault"] as? Boolean) ?: false,
            updatedAtMillis = ts
        )
    }

    fun transactionToCreateMap(tx: WalletTransaction): Map<String, Any?> = mapOf(
        "userId" to tx.userId,
        "category" to tx.category.name,
        "title" to tx.title,
        "subtitle" to tx.subtitle,
        "amount" to tx.amount,
        "status" to tx.status.name,
        "createdAtMillis" to tx.createdAtMillis,
        "createdAt" to FieldValue.serverTimestamp()
    )

    fun transactionFromSnapshot(doc: DocumentSnapshot): WalletTransaction? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        return WalletTransaction(
            id = doc.id,
            userId = (d["userId"] as? String) ?: return null,
            category = enumOrDefault(d["category"] as? String, TransactionCategory.ADJUSTMENT),
            title = (d["title"] as? String) ?: "",
            subtitle = (d["subtitle"] as? String) ?: "",
            amount = (d["amount"] as? Number)?.toDouble() ?: 0.0,
            status = enumOrDefault(d["status"] as? String, TransactionStatus.COMPLETED),
            createdAtMillis = (d["createdAtMillis"] as? Number)?.toLong() ?: 0L
        )
    }

    fun betToCreateMap(bet: UserBet): Map<String, Any?> = mapOf(
        "userId" to bet.userId,
        "periodId" to bet.periodId,
        "modeId" to bet.modeId,
        "selectionType" to bet.selectionType.name,
        "selectionValue" to bet.selectionValue,
        "baseStake" to bet.baseStake,
        "multiplier" to bet.multiplier,
        "totalStake" to bet.totalStake,
        "status" to BetStatus.PENDING.name,
        "payout" to 0.0,
        "createdAtMillis" to bet.createdAtMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun betFromSnapshot(doc: DocumentSnapshot): UserBet? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        return UserBet(
            id = doc.id,
            userId = (d["userId"] as? String) ?: return null,
            periodId = (d["periodId"] as? String) ?: return null,
            modeId = (d["modeId"] as? String) ?: return null,
            selectionType = enumOrDefault(d["selectionType"] as? String, BetSelectionType.COLOR),
            selectionValue = (d["selectionValue"] as? String) ?: return null,
            baseStake = (d["baseStake"] as? Number)?.toDouble() ?: 1.0,
            multiplier = (d["multiplier"] as? Number)?.toInt() ?: 1,
            totalStake = (d["totalStake"] as? Number)?.toDouble() ?: 1.0,
            status = enumOrDefault(d["status"] as? String, BetStatus.PENDING),
            payout = (d["payout"] as? Number)?.toDouble() ?: 0.0,
            createdAtMillis = (d["createdAtMillis"] as? Number)?.toLong() ?: 0L
        )
    }

    fun depositToCreateMap(req: DepositRequest): Map<String, Any?> = mapOf(
        "userId" to req.userId,
        "orderId" to req.orderId,
        "userPhone" to req.userPhone,
        "amount" to req.amount,
        "utrNumber" to req.utrNumber,
        "upiIdUsed" to req.upiIdUsed,
        "gatewayName" to req.gatewayName,
        "status" to RequestStatus.PENDING.name,
        "bonusApplied" to 0.0,
        "adminNote" to req.adminNote,
        "createdAtMillis" to req.createdAtMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun depositFromSnapshot(doc: DocumentSnapshot): DepositRequest? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        val status = enumOrDefault(d["status"] as? String, RequestStatus.PENDING)
        return DepositRequest(
            id = doc.id,
            orderId = (d["orderId"] as? String) ?: doc.id,
            userId = (d["userId"] as? String) ?: return null,
            userPhone = (d["userPhone"] as? String) ?: "",
            amount = (d["amount"] as? Number)?.toDouble() ?: 0.0,
            utrNumber = (d["utrNumber"] as? String) ?: "",
            upiIdUsed = (d["upiIdUsed"] as? String) ?: "",
            gatewayName = (d["gatewayName"] as? String) ?: "Kelywin Dynamic UPI Gateway",
            status = status,
            bonusApplied = (d["bonusApplied"] as? Number)?.toDouble() ?: 0.0,
            adminNote = (d["adminNote"] as? String) ?: "",
            createdAtMillis = (d["createdAtMillis"] as? Number)?.toLong() ?: 0L,
            processedAtMillis = if (status == RequestStatus.PENDING) null else (d["updatedAt"] as? Timestamp)?.toDate()?.time
        )
    }

    fun withdrawalToCreateMap(req: WithdrawalRequest): Map<String, Any?> = mapOf(
        "userId" to req.userId,
        "userPhone" to req.userPhone,
        "amount" to req.amount,
        "methodType" to req.methodType.name,
        "methodLabel" to req.methodLabel,
        "accountHolder" to req.accountHolder,
        "accountIdentifier" to req.accountIdentifier,
        "ifscCode" to req.ifscCode,
        "status" to RequestStatus.PENDING.name,
        "adminNote" to req.adminNote,
        "createdAtMillis" to req.createdAtMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun withdrawalFromSnapshot(doc: DocumentSnapshot): WithdrawalRequest? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        val status = enumOrDefault(d["status"] as? String, RequestStatus.PENDING)
        return WithdrawalRequest(
            id = doc.id,
            userId = (d["userId"] as? String) ?: return null,
            userPhone = (d["userPhone"] as? String) ?: "",
            amount = (d["amount"] as? Number)?.toDouble() ?: 0.0,
            methodType = enumOrDefault(d["methodType"] as? String, PayoutMethodType.UPI),
            methodLabel = (d["methodLabel"] as? String) ?: "",
            accountHolder = (d["accountHolder"] as? String) ?: "",
            accountIdentifier = (d["accountIdentifier"] as? String) ?: "",
            ifscCode = (d["ifscCode"] as? String) ?: "",
            status = status,
            adminNote = (d["adminNote"] as? String) ?: "",
            createdAtMillis = (d["createdAtMillis"] as? Number)?.toLong() ?: 0L,
            processedAtMillis = if (status == RequestStatus.PENDING) null else (d["updatedAt"] as? Timestamp)?.toDate()?.time
        )
    }

    fun passwordResetToCreateMap(req: PasswordResetRequest): Map<String, Any?> = mapOf(
        "userId" to req.userId,
        "identifier" to req.identifier,
        "status" to RequestStatus.PENDING.name,
        "assignedPassword" to req.assignedPassword,
        "deliveryChannel" to req.deliveryChannel,
        "createdAtMillis" to req.createdAtMillis,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun passwordResetFromSnapshot(doc: DocumentSnapshot): PasswordResetRequest? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        val status = enumOrDefault(d["status"] as? String, RequestStatus.PENDING)
        return PasswordResetRequest(
            id = doc.id,
            userId = (d["userId"] as? String) ?: return null,
            identifier = (d["identifier"] as? String) ?: "",
            status = status,
            assignedPassword = (d["assignedPassword"] as? String) ?: "",
            deliveryChannel = (d["deliveryChannel"] as? String) ?: "",
            createdAtMillis = (d["createdAtMillis"] as? Number)?.toLong() ?: 0L,
            resolvedAtMillis = if (status == RequestStatus.PENDING) null else (d["updatedAt"] as? Timestamp)?.toDate()?.time
        )
    }

    fun roundLogToMap(log: RoundAdminLog): Map<String, Any?> = mapOf(
        "periodId" to log.periodId,
        "modeId" to log.modeId,
        "controlModeUsed" to log.controlModeUsed.name,
        "winningNumber" to log.winningNumber.coerceIn(0, 9),
        "winningColor" to log.winningColor.name,
        "winningSide" to log.winningSide.name,
        "totalBetsAmount" to log.totalBetsAmount,
        "totalPayoutAmount" to log.totalPayoutAmount,
        "netProfit" to log.netProfit,
        "settledAtMillis" to log.settledAtMillis,
        "createdAt" to FieldValue.serverTimestamp()
    )

    fun roundLogFromSnapshot(doc: DocumentSnapshot): RoundAdminLog? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        return RoundAdminLog(
            periodId = (d["periodId"] as? String) ?: return null,
            modeId = (d["modeId"] as? String) ?: return null,
            controlModeUsed = enumOrDefault(d["controlModeUsed"] as? String, GameControlMode.RANDOM),
            winningNumber = ((d["winningNumber"] as? Number)?.toInt() ?: 7).coerceIn(0, 9),
            winningColor = enumOrDefault(d["winningColor"] as? String, BetColor.GREEN),
            winningSide = enumOrDefault(d["winningSide"] as? String, DragonTigerSide.DRAGON),
            totalBetsAmount = (d["totalBetsAmount"] as? Number)?.toDouble() ?: 0.0,
            totalPayoutAmount = (d["totalPayoutAmount"] as? Number)?.toDouble() ?: 0.0,
            netProfit = (d["netProfit"] as? Number)?.toDouble() ?: 0.0,
            settledAtMillis = (d["settledAtMillis"] as? Number)?.toLong() ?: 0L
        )
    }

    fun adminMemberToCreateMap(uid: String, email: String): Map<String, Any?> = mapOf(
        "uid" to uid,
        "email" to email.trim(),
        "role" to "ADMIN",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun adminMemberFromSnapshot(doc: DocumentSnapshot): AdminMember? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        return AdminMember(
            uid = (d["uid"] as? String) ?: doc.id,
            email = (d["email"] as? String) ?: "",
            role = (d["role"] as? String) ?: "ADMIN",
            createdAtMillis = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
        )
    }

    fun adminGatewaySecretToMap(secret: AdminGatewaySecret): Map<String, Any?> = mapOf(
        "provider" to secret.provider.ifBlank { "FAMGATEWAY" },
        "apiKey" to secret.apiKey.trim(),
        "backendUrl" to secret.backendUrl.trim().trimEnd('/'),
        "enabled" to secret.enabled,
        "updatedByUid" to secret.updatedByUid,
        "updatedAtMillis" to secret.updatedAtMillis,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun adminGatewaySecretFromSnapshot(doc: DocumentSnapshot): AdminGatewaySecret? {
        if (!doc.exists()) return null
        val d = doc.data ?: return null
        return AdminGatewaySecret(
            provider = (d["provider"] as? String) ?: "FAMGATEWAY",
            apiKey = (d["apiKey"] as? String) ?: "",
            backendUrl = (d["backendUrl"] as? String) ?: "",
            enabled = (d["enabled"] as? Boolean) ?: true,
            updatedByUid = (d["updatedByUid"] as? String) ?: "",
            updatedAtMillis = (d["updatedAtMillis"] as? Number)?.toLong() ?: 0L
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
