package com.example

import android.app.Application
import com.example.adminapp.AdminRuntimeHost
import com.example.adminapp.data.AdminFirebaseRepository
import com.example.adminapp.services.AdminRoundCoordinator
import com.example.data.local.AppControlConfigEntity
import com.example.data.local.KelywinDatabase
import com.example.data.local.SessionPreferences
import com.example.services.AuthAndAccountService
import com.example.services.PaymentGatewayService
import com.example.services.PeriodEngine
import com.example.services.UpiQrPaymentGatewayService
import com.example.services.UserFirebaseRepository
import com.example.shared.model.AppControlConfig
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KelywinApplication : Application(), AdminRuntimeHost {
    override val adminFirebaseRepository: AdminFirebaseRepository by lazy {
        AdminFirebaseRepository(applicationContext)
    }
    override val adminRoundCoordinator: AdminRoundCoordinator by lazy {
        AdminRoundCoordinator(adminFirebaseRepository)
    }

    lateinit var database: KelywinDatabase
        private set
    lateinit var sessionPreferences: SessionPreferences
        private set
    lateinit var userFirebaseRepository: UserFirebaseRepository
        private set
    lateinit var authService: AuthAndAccountService
        private set
    lateinit var paymentGatewayService: PaymentGatewayService
        private set
    lateinit var periodEngine: PeriodEngine
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        database = KelywinDatabase.getInstance(this)
        sessionPreferences = SessionPreferences(this)

        appScope.launch {
            if (database.appControlConfigDao().getConfig() == null) {
                val defaults = AppControlConfig()
                database.appControlConfigDao().upsert(
                    AppControlConfigEntity(
                        id = 1,
                        announcementText = defaults.announcementText,
                        maintenanceMode = defaults.maintenanceMode,
                        maintenanceMessage = defaults.maintenanceMessage,
                        enableRegistration = defaults.enableRegistration,
                        enableDeposits = defaults.enableDeposits,
                        enableWithdrawals = defaults.enableWithdrawals,
                        enableNumberBetting = defaults.enableNumberBetting,
                        firstDepositBonusAmount = defaults.firstDepositBonusAmount,
                        minBetAmount = defaults.minBetAmount,
                        greenRedMultiplier = defaults.multipliers.greenRedMultiplier,
                        violetMultiplier = defaults.multipliers.violetMultiplier,
                        numberMultiplier = defaults.multipliers.numberMultiplier,
                        dragonTigerMultiplier = defaults.multipliers.dragonTigerMultiplier,
                        platformFeeRate = defaults.multipliers.platformFeeRate,
                        gatewayName = defaults.upiQrConfig.gatewayName,
                        gatewayApiUrl = defaults.upiQrConfig.gatewayApiUrl,
                        merchantKeyId = defaults.upiQrConfig.merchantKeyId,
                        upiId = defaults.upiQrConfig.merchantUpiPool,
                        merchantName = defaults.upiQrConfig.merchantName,
                        qrLabel = defaults.upiQrConfig.qrLabel,
                        minDeposit = defaults.upiQrConfig.minDeposit,
                        maxDeposit = defaults.upiQrConfig.maxDeposit,
                        apiBaseUrl = defaults.apiBaseUrl,
                        telegramWebhookUrl = defaults.telegramWebhookUrl,
                        emailWebhookUrl = defaults.emailWebhookUrl,
                        smsWebhookUrl = defaults.smsWebhookUrl
                    )
                )
            }
        }

        userFirebaseRepository = UserFirebaseRepository(
            context = this,
            userDao = database.userDao(),
            payoutMethodDao = database.payoutMethodDao(),
            walletTransactionDao = database.walletTransactionDao(),
            periodHistoryDao = database.periodHistoryDao(),
            userBetDao = database.userBetDao(),
            roomControlDao = database.roomControlDao(),
            depositRequestDao = database.depositRequestDao(),
            withdrawalRequestDao = database.withdrawalRequestDao(),
            appControlConfigDao = database.appControlConfigDao()
        )

        authService = AuthAndAccountService(
            userDao = database.userDao(),
            payoutMethodDao = database.payoutMethodDao(),
            walletTransactionDao = database.walletTransactionDao(),
            withdrawalRequestDao = database.withdrawalRequestDao(),
            passwordResetRequestDao = database.passwordResetRequestDao(),
            appControlConfigDao = database.appControlConfigDao(),
            sessionPreferences = sessionPreferences,
            userFirebaseRepository = userFirebaseRepository
        )

        paymentGatewayService = UpiQrPaymentGatewayService(
            walletTransactionDao = database.walletTransactionDao(),
            depositRequestDao = database.depositRequestDao(),
            appControlConfigDao = database.appControlConfigDao(),
            userFirebaseRepository = userFirebaseRepository
        )

        periodEngine = PeriodEngine(
            periodHistoryDao = database.periodHistoryDao(),
            userBetDao = database.userBetDao(),
            userDao = database.userDao(),
            walletTransactionDao = database.walletTransactionDao(),
            roomControlDao = database.roomControlDao(),
            roundAdminLogDao = database.roundAdminLogDao(),
            appControlConfigDao = database.appControlConfigDao(),
            userFirebaseRepository = userFirebaseRepository
        )
    }
}
