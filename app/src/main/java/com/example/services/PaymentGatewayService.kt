package com.example.services

import com.example.data.local.AppControlConfigDao
import com.example.data.local.DepositRequestDao
import com.example.data.local.DepositRequestEntity
import com.example.data.local.WalletTransactionDao
import com.example.data.local.WalletTransactionEntity
import com.example.shared.model.DepositRequest
import com.example.shared.model.DynamicPaymentOrder
import com.example.shared.model.PaymentGatewayResult
import com.example.shared.model.RequestStatus
import com.example.shared.model.TransactionCategory
import com.example.shared.model.TransactionStatus
import com.example.shared.model.UpiQrConfig
import com.example.shared.model.WalletTransaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

data class FamGatewayPollResult(
    val status: String,
    val orderId: String,
    val amount: Double,
    val utrNumber: String = "",
    val senderName: String = "",
    val bonusApplied: Double = 0.0,
    val message: String = ""
)

/**
 * Dynamic Payment Gateway abstraction for UPI QR deposits.
 * Supports FamGateway v2.0 (Without-UTR automatic credit via backend) and manual UPI fallback.
 */
interface PaymentGatewayService {
    suspend fun getUpiQrConfig(): UpiQrConfig
    suspend fun createDynamicQrOrder(userId: String, amount: Double): Result<DynamicPaymentOrder>
    suspend fun verifyFamGatewayOrder(orderId: String): Result<FamGatewayPollResult>
    suspend fun submitUpiQrDeposit(
        userId: String,
        userPhone: String,
        amount: Double,
        utrNumber: String,
        orderId: String,
        assignedUpiId: String
    ): Result<PaymentGatewayResult>
}

class UpiQrPaymentGatewayService(
    private val walletTransactionDao: WalletTransactionDao,
    private val depositRequestDao: DepositRequestDao,
    private val appControlConfigDao: AppControlConfigDao,
    private val userFirebaseRepository: UserFirebaseRepository? = null
) : PaymentGatewayService {

    private val rotationCounter = AtomicInteger(0)

    override suspend fun getUpiQrConfig(): UpiQrConfig {
        return appControlConfigDao.getConfig()?.toDomain()?.upiQrConfig ?: UpiQrConfig()
    }

    override suspend fun createDynamicQrOrder(userId: String, amount: Double): Result<DynamicPaymentOrder> {
        val appConfig = appControlConfigDao.getConfig()?.toDomain()
        if (appConfig != null && !appConfig.enableDeposits) {
            return Result.failure(IllegalStateException("Deposits are temporarily paused by Admin."))
        }
        val config = appConfig?.upiQrConfig ?: UpiQrConfig()
        if (amount < config.minDeposit) {
            return Result.failure(IllegalArgumentException("Minimum gateway deposit is ₹${config.minDeposit}."))
        }
        if (amount > config.maxDeposit) {
            return Result.failure(IllegalArgumentException("Maximum gateway deposit is ₹${config.maxDeposit}."))
        }

        // If Admin configured a live FamGateway Backend URL, create an automatic Without-UTR order.
        if (config.gatewayApiUrl.startsWith("http", ignoreCase = true)) {
            return createFamGatewayBackendOrder(
                backendBaseUrl = config.gatewayApiUrl.trim().trimEnd('/'),
                amount = amount
            )
        }

        val now = System.currentTimeMillis()
        val orderId = "KWPG-${now.toString().takeLast(6)}-${UUID.randomUUID().toString().take(4).uppercase(Locale.US)}"
        val expiresAt = now + 5 * 60 * 1000L

        val upiPool = config.merchantUpiPool
            .split(",", "\n", ";")
            .map { it.trim() }
            .filter { it.contains("@") }
            .ifEmpty { listOf("kelywin.gw@okaxis") }

        val idx = rotationCounter.getAndIncrement() % upiPool.size
        val selectedUpi = upiPool[idx]

        val fallbackIntent = buildDynamicUpiIntent(
            upiId = selectedUpi,
            merchantName = config.merchantName,
            orderId = orderId,
            amount = amount
        )

        return Result.success(
            DynamicPaymentOrder(
                orderId = orderId,
                amount = amount,
                gatewayName = config.gatewayName.ifBlank { "Kelywin Dynamic UPI Gateway" },
                assignedUpiId = selectedUpi,
                upiIntentUri = fallbackIntent,
                isLiveGatewayApi = false,
                createdAtMillis = now,
                expiresAtMillis = expiresAt,
                isFamGateway = false
            )
        )
    }

    override suspend fun verifyFamGatewayOrder(orderId: String): Result<FamGatewayPollResult> {
        val config = getUpiQrConfig()
        val baseUrl = config.gatewayApiUrl.trim().trimEnd('/')
        if (!baseUrl.startsWith("http", ignoreCase = true)) {
            return Result.failure(IllegalStateException("FamGateway Backend URL is not configured."))
        }

        val idToken = runCatching {
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        }.getOrNull()
        if (idToken.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Please sign in again to verify payment."))
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val encodedOrder = URLEncoder.encode(orderId, "UTF-8")
                val conn = (URL("$baseUrl/api/famgateway/status?orderId=$encodedOrder").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Authorization", "Bearer $idToken")
                }
                val code = conn.responseCode
                val bodyText = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                val json = if (bodyText.isNotBlank()) JSONObject(bodyText) else JSONObject()
                if (code !in 200..299) {
                    throw IllegalStateException(json.optString("message").ifBlank { "Gateway verify HTTP $code" })
                }
                FamGatewayPollResult(
                    status = json.optString("status", "PENDING").uppercase(Locale.US),
                    orderId = json.optString("orderId", orderId),
                    amount = json.optDouble("amount", 0.0),
                    utrNumber = json.optString("utr", ""),
                    senderName = json.optString("senderName", ""),
                    bonusApplied = json.optDouble("bonusApplied", 0.0),
                    message = json.optString("message", "")
                )
            }
        }
    }

    private suspend fun createFamGatewayBackendOrder(
        backendBaseUrl: String,
        amount: Double
    ): Result<DynamicPaymentOrder> {
        val fbUser = FirebaseAuth.getInstance().currentUser
            ?: return Result.failure(IllegalStateException("Please sign in with Google to use FamGateway."))
        val idToken = runCatching { fbUser.getIdToken(false).await()?.token }.getOrNull()
        if (idToken.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Session expired. Please sign in again."))
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL("$backendBaseUrl/api/famgateway/create-order").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 12000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $idToken")
                }
                val payload = JSONObject().apply {
                    put("amount", amount)
                    put("customerName", fbUser.displayName ?: "Kelywin User")
                }.toString()
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val bodyText = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()
                val json = if (bodyText.isNotBlank()) JSONObject(bodyText) else JSONObject()

                if (code !in 200..299 || json.optString("status") != "success") {
                    throw IllegalStateException(
                        json.optString("message").ifBlank { "FamGateway server error (HTTP $code)" }
                    )
                }

                val orderId = json.optString("orderId").ifBlank {
                    throw IllegalStateException("Invalid orderId returned by FamGateway backend.")
                }
                val now = System.currentTimeMillis()
                val payable = json.optDouble("payableAmount", amount).takeIf { it > 0.0 } ?: amount
                val expiresAt = json.optLong("expiresAtMillis", now + 5 * 60 * 1000L)

                DynamicPaymentOrder(
                    orderId = orderId,
                    amount = payable,
                    gatewayName = "FamGateway Auto UPI",
                    assignedUpiId = json.optString("upiId", "FamPay UPI"),
                    upiIntentUri = json.optString("upiIntent"),
                    isLiveGatewayApi = true,
                    createdAtMillis = now,
                    expiresAtMillis = expiresAt,
                    checkoutUrl = json.optString("checkoutUrl"),
                    qrUrl = json.optString("qrUrl"),
                    isFamGateway = true
                )
            }
        }
    }

    override suspend fun submitUpiQrDeposit(
        userId: String,
        userPhone: String,
        amount: Double,
        utrNumber: String,
        orderId: String,
        assignedUpiId: String
    ): Result<PaymentGatewayResult> {
        val appConfig = appControlConfigDao.getConfig()?.toDomain()
        if (appConfig != null && !appConfig.enableDeposits) {
            return Result.failure(IllegalStateException("Deposits are temporarily paused by Admin."))
        }
        val config = appConfig?.upiQrConfig ?: UpiQrConfig()
        val cleanUtr = utrNumber.trim()
        val cleanOrderId = orderId.trim().ifBlank {
            "KWPG-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
        }

        if (amount < config.minDeposit) {
            return Result.failure(IllegalArgumentException("Minimum UPI deposit is ₹${config.minDeposit}."))
        }
        if (amount > config.maxDeposit) {
            return Result.failure(IllegalArgumentException("Maximum UPI deposit is ₹${config.maxDeposit}."))
        }
        if (cleanUtr.length < 6) {
            return Result.failure(IllegalArgumentException("Enter a valid UTR / Gateway Reference ID (at least 6 characters)."))
        }

        val now = System.currentTimeMillis()
        val reqId = "DEP-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
        val upiUsed = assignedUpiId.ifBlank {
            config.merchantUpiPool.split(",").firstOrNull()?.trim().orEmpty().ifBlank { "gateway-upi" }
        }
        val gwName = config.gatewayName.ifBlank { "Kelywin Dynamic UPI Gateway" }

        val depositDomain = DepositRequest(
            id = reqId,
            orderId = cleanOrderId,
            userId = userId,
            userPhone = userPhone,
            amount = amount,
            utrNumber = cleanUtr,
            upiIdUsed = upiUsed,
            gatewayName = gwName,
            status = RequestStatus.PENDING,
            bonusApplied = 0.0,
            adminNote = "",
            createdAtMillis = now,
            processedAtMillis = null
        )
        val txDomain = WalletTransaction(
            id = reqId,
            userId = userId,
            category = TransactionCategory.DEPOSIT,
            title = "Gateway UPI Deposit · ₹${amount.toInt()}",
            subtitle = "Order $cleanOrderId · UTR $cleanUtr · Pending Verification",
            amount = amount,
            status = TransactionStatus.PENDING_GATEWAY_INTEGRATION,
            createdAtMillis = now
        )

        if (userFirebaseRepository != null && userFirebaseRepository.currentFirebaseUid() == userId) {
            userFirebaseRepository.submitDepositCloud(depositDomain, txDomain)
                .onFailure { return Result.failure(it) }
        }

        depositRequestDao.upsert(
            DepositRequestEntity(
                id = depositDomain.id,
                orderId = depositDomain.orderId,
                userId = depositDomain.userId,
                userPhone = depositDomain.userPhone,
                amount = depositDomain.amount,
                utrNumber = depositDomain.utrNumber,
                upiIdUsed = depositDomain.upiIdUsed,
                gatewayName = depositDomain.gatewayName,
                status = depositDomain.status.name,
                bonusApplied = 0.0,
                adminNote = "",
                createdAtMillis = now,
                processedAtMillis = null
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
                createdAtMillis = txDomain.createdAtMillis
            )
        )

        val bonusHint = (appConfig?.firstDepositBonusAmount ?: 12.0).toInt()
        return Result.success(
            PaymentGatewayResult(
                status = "PENDING_VERIFICATION",
                referenceId = reqId,
                orderId = cleanOrderId,
                upiId = upiUsed,
                utrNumber = cleanUtr,
                amount = amount,
                message = "Gateway order $cleanOrderId submitted (UTR $cleanUtr). Once verified by Admin, ₹${String.format(Locale.US, "%.2f", amount)} (+₹$bonusHint on first deposit) credits automatically.",
                createdAtMillis = now
            )
        )
    }

    private fun buildDynamicUpiIntent(
        upiId: String,
        merchantName: String,
        orderId: String,
        amount: Double
    ): String {
        val pn = URLEncoder.encode(merchantName.ifBlank { "Kelywin" }, "UTF-8")
        val tn = URLEncoder.encode("Kelywin Order $orderId", "UTF-8")
        val amt = String.format(Locale.US, "%.2f", amount)
        return "upi://pay?pa=$upiId&pn=$pn&tr=$orderId&tn=$tn&am=$amt&cu=INR"
    }
}
