package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.DynamicPaymentOrder
import com.example.shared.model.PaymentGatewayResult
import com.example.shared.model.TransactionCategory
import com.example.shared.model.TransactionStatus
import com.example.shared.model.UpiQrConfig
import com.example.shared.model.UserAccount
import com.example.shared.model.WalletTransaction
import com.example.shared.ui.components.KelywinCoralHeader
import com.example.shared.ui.components.UpiQrCodeCanvas
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyBorder
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyCoralSoft
import com.example.shared.ui.theme.KelyDragon
import com.example.shared.ui.theme.KelyDragonSoft
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyGreenSoft
import com.example.shared.ui.theme.KelyHeaderGradientBottom
import com.example.shared.ui.theme.KelyHeaderGradientTop
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelyRed
import com.example.shared.ui.theme.KelyRedSoft
import com.example.shared.ui.theme.KelySurfaceMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun BalanceScreen(
    user: UserAccount?,
    transactions: List<WalletTransaction>,
    onNavigateDeposit: () -> Unit,
    onNavigateWithdraw: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<TransactionCategory?>(null) }

    val filteredTx = remember(transactions, selectedCategory) {
        if (selectedCategory == null) transactions
        else transactions.filter { it.category == selectedCategory }
    }

    val totalWins = remember(transactions) {
        transactions.filter { it.category == TransactionCategory.WIN }.sumOf { it.amount }
    }
    val totalBets = remember(transactions) {
        transactions.filter { it.category == TransactionCategory.BET }.sumOf { kotlin.math.abs(it.amount) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
    ) {
        KelywinCoralHeader(showBackButton = false)

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(listOf(KelyHeaderGradientTop, KelyHeaderGradientBottom))
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "Available Balance",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", user?.balance ?: 0.0)}",
                                color = Color.White,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Total Wins", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = "₹${String.format(Locale.US, "%.2f", totalWins)}",
                                            color = Color.White,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Total Bets", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = "₹${String.format(Locale.US, "%.2f", totalBets)}",
                                            color = Color.White,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onNavigateDeposit,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("balance_deposit_button")
                                ) {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("UPI QR Pay", color = KelyCoralPrimary, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onNavigateWithdraw,
                                    colors = ButtonDefaults.buttonColors(containerColor = KelyGreen),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("balance_withdraw_button")
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Withdraw", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChipPill(
                            label = "All",
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            modifier = Modifier.testTag("filter_tx_all")
                        )
                    }
                    items(TransactionCategory.entries) { cat ->
                        FilterChipPill(
                            label = cat.label,
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            modifier = Modifier.testTag("filter_tx_${cat.name.lowercase()}")
                        )
                    }
                }
            }

            if (filteredTx.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(38.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No transactions yet", fontWeight = FontWeight.SemiBold, color = KelyInkPrimary)
                        }
                    }
                }
            } else {
                items(filteredTx, key = { it.id }) { tx ->
                    TransactionRowCard(tx = tx)
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

/**
 * DepositScreen — Dynamic UPI Payment Gateway QR (NO static/fixed QR code).
 * Every transaction generates a fresh dynamic gateway orderId + amount-locked QR & UPI Intent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DepositScreen(
    user: UserAccount?,
    upiQrConfig: UpiQrConfig,
    firstDepositBonusAmount: Double,
    enableDeposits: Boolean = true,
    activeOrder: DynamicPaymentOrder?,
    isCreatingOrder: Boolean = false,
    isVerifyingPayment: Boolean = false,
    recentDeposits: List<WalletTransaction>,
    latestReceipt: PaymentGatewayResult?,
    onGenerateDynamicQr: (Double) -> Unit,
    onCheckFamGatewayStatus: () -> Unit = {},
    onClearDynamicOrder: () -> Unit,
    onSubmitUpiDeposit: (Double, String, () -> Unit) -> Unit,
    onCopyUpiId: (String) -> Unit,
    onNotifyMessage: (String) -> Unit,
    onDismissReceipt: () -> Unit,
    onBackClick: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var amountInput by remember { mutableStateOf("500") }
    var utrInput by remember { mutableStateOf("") }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val presets = listOf(100, 300, 500, 1000, 5000)

    LaunchedEffect(activeOrder?.orderId) {
        while (activeOrder != null) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
    ) {
        KelywinCoralHeader(
            showBackButton = true,
            onBackClick = onBackClick
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column {
                            Text("Current Wallet Balance", style = MaterialTheme.typography.bodySmall, color = KelyInkSecondary)
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", user?.balance ?: 0.0)}",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = KelyInkPrimary
                            )
                        }
                        Surface(color = KelyGreenSoft, shape = RoundedCornerShape(50)) {
                            Text(
                                text = "Dynamic UPI Gateway",
                                color = KelyGreen,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            if (user != null && !user.hasReceivedFirstDepositBonus) {
                item {
                    Surface(
                        color = KelyDragonSoft,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "First Deposit Offer: Get +₹${firstDepositBonusAmount.toInt()} bonus automatically on your first approved UPI gateway deposit!",
                                color = Color.Black,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (!enableDeposits) {
                item {
                    Surface(
                        color = KelyRedSoft,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "UPI Gateway deposits are temporarily paused by Admin. Please try again shortly.",
                            color = KelyRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (activeOrder == null) {
                // Step 1: Enter Amount & Create Dynamic Gateway Order (No static QR shown!)
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = KelyCoralPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(upiQrConfig.gatewayName, fontWeight = FontWeight.ExtraBold, color = KelyInkPrimary)
                                    Text(
                                        text = "Fresh unique QR & order ID generated per transaction",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KelyInkSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Select Deposit Amount", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presets.forEach { preset ->
                                    val active = amountInput == preset.toString()
                                    Surface(
                                        color = if (active) KelyCoralPrimary else KelySurfaceMuted,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { amountInput = preset.toString() }
                                            .testTag("deposit_preset_$preset")
                                    ) {
                                        Text(
                                            text = "₹$preset",
                                            color = if (active) Color.White else KelyInkPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Deposit Amount (₹${upiQrConfig.minDeposit} – ₹${upiQrConfig.maxDeposit})") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = KelyCoralPrimary,
                                    unfocusedBorderColor = KelyBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("deposit_amount_input")
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val amt = amountInput.toDoubleOrNull() ?: 0.0
                                    onGenerateDynamicQr(amt)
                                },
                                enabled = enableDeposits && !isCreatingOrder,
                                colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("generate_gateway_qr_button")
                            ) {
                                if (isCreatingOrder) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Creating Gateway Order...", color = Color.White, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val isLiveMode = upiQrConfig.gatewayApiUrl.startsWith("http", ignoreCase = true)
                                    Text(
                                        text = if (isLiveMode) "Pay via FamGateway (No UTR)" else "Generate Dynamic Gateway QR",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Step 2: Live Dynamic Gateway Order QR Checkout
                val remainingSec = ((activeOrder.expiresAtMillis - nowMillis) / 1000L).coerceAtLeast(0L)
                val mm = (remainingSec / 60).toString().padStart(2, '0')
                val ss = (remainingSec % 60).toString().padStart(2, '0')
                val isExpired = remainingSec == 0L

                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(activeOrder.gatewayName, fontWeight = FontWeight.ExtraBold, color = KelyInkPrimary)
                                    Text(
                                        text = "Order: ${activeOrder.orderId}",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 11.sp,
                                        color = KelyInkSecondary
                                    )
                                }
                                Surface(
                                    color = if (isExpired) KelyRedSoft else KelyDragonSoft,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (isExpired) "EXPIRED" else "$mm:$ss",
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpired) KelyRed else Color.Black,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Pay Exact Amount: ₹${String.format(Locale.US, "%.2f", activeOrder.amount)}",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = KelyCoralPrimary
                            )
                            Text(
                                text = if (activeOrder.isLiveGatewayApi) "Live Gateway API Session" else "Dynamic Rotating UPI Order",
                                style = MaterialTheme.typography.labelSmall,
                                color = KelyGreen,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            UpiQrCodeCanvas(upiPayload = activeOrder.upiIntentUri, size = 186.dp)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        runCatching {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeOrder.upiIntentUri))
                                            context.startActivity(intent)
                                        }.onFailure {
                                            onNotifyMessage("No UPI app detected on device. Scan QR or copy UPI ID below.")
                                        }
                                    },
                                    enabled = !isExpired,
                                    colors = ButtonDefaults.buttonColors(containerColor = KelyGreen),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1.2f).height(44.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open UPI App", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(activeOrder.assignedUpiId))
                                        onCopyUpiId(activeOrder.assignedUpiId)
                                    },
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, KelyCoralPrimary),
                                    modifier = Modifier.weight(1f).height(44.dp).testTag("copy_upi_id_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy UPI", color = KelyCoralPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gateway VPA: ${activeOrder.assignedUpiId}",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = KelyInkSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (activeOrder.isFamGateway) {
                                // Without-UTR FamGateway Automatic Verification UI
                                Surface(
                                    color = KelyGreenSoft,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = KelyGreen,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "No UTR needed! Auto-verifying FamPay credit every 3.5s. Balance adds automatically once paid (₹0 added if failed).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = KelyGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (activeOrder.checkoutUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activeOrder.checkoutUrl)))
                                            }.onFailure {
                                                onNotifyMessage("Unable to open checkout browser.")
                                            }
                                        },
                                        enabled = !isExpired,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, KelyCoralPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("open_famgateway_checkout_button")
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Hosted FamGateway Page", color = KelyCoralPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            utrInput = ""
                                            onClearDynamicOrder()
                                        },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(0.85f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = onCheckFamGatewayStatus,
                                        enabled = !isVerifyingPayment,
                                        colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1.45f)
                                            .height(48.dp)
                                            .testTag("verify_famgateway_status_button")
                                    ) {
                                        if (isVerifyingPayment) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Checking...", color = Color.White, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("I've Paid · Verify Now", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = utrInput,
                                    onValueChange = { utrInput = it },
                                    label = { Text("UTR / Gateway Reference Number") },
                                    placeholder = { Text("Enter 12-digit UTR / Ref ID after paying") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = KelyCoralPrimary,
                                        unfocusedBorderColor = KelyBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("deposit_utr_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            utrInput = ""
                                            onClearDynamicOrder()
                                        },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(0.9f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("New QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            onSubmitUpiDeposit(activeOrder.amount, utrInput) {
                                                utrInput = ""
                                            }
                                        },
                                        enabled = enableDeposits && !isExpired,
                                        colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp)
                                            .testTag("deposit_submit_button")
                                    ) {
                                        Text("Verify & Submit", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (recentDeposits.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent UPI Gateway Deposits",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KelyInkPrimary
                    )
                }
                items(recentDeposits.take(8), key = { it.id }) { tx ->
                    TransactionRowCard(tx = tx)
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }

    if (latestReceipt != null) {
        val isSuccess = latestReceipt.status.equals("SUCCESS", ignoreCase = true)
        val isFailed = latestReceipt.status.equals("FAILED", ignoreCase = true)
        val iconVector = when {
            isSuccess -> Icons.Default.CheckCircle
            isFailed -> Icons.Default.ErrorOutline
            else -> Icons.Default.HourglassBottom
        }
        val iconTint = when {
            isSuccess -> KelyGreen
            isFailed -> KelyRed
            else -> KelyDragon
        }
        val dialogTitle = when {
            isSuccess -> "Payment Verified & Credited!"
            isFailed -> "Payment Failed / Expired"
            else -> "Gateway Deposit Submitted"
        }
        val statusBg = when {
            isSuccess -> KelyGreenSoft
            isFailed -> KelyRedSoft
            else -> KelyDragonSoft
        }
        val statusFg = when {
            isSuccess -> KelyGreen
            isFailed -> KelyRed
            else -> Color.Black
        }

        AlertDialog(
            onDismissRequest = onDismissReceipt,
            containerColor = Color.White,
            icon = {
                Icon(iconVector, contentDescription = null, tint = iconTint, modifier = Modifier.size(40.dp))
            },
            title = {
                Text(dialogTitle, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = statusBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Status: ${latestReceipt.status}",
                            color = statusFg,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text("Order ID: ${latestReceipt.orderId}", fontFamily = JetBrainsMono, fontSize = 12.sp)
                    Text("UPI VPA: ${latestReceipt.upiId}", fontFamily = JetBrainsMono, fontSize = 12.sp)
                    Text("Bank UTR: ${latestReceipt.utrNumber}", fontFamily = JetBrainsMono, fontSize = 12.sp)
                    Text("Amount: ₹${String.format(Locale.US, "%.2f", latestReceipt.amount)}", fontWeight = FontWeight.Bold)
                    Text(
                        text = latestReceipt.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = KelyInkSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissReceipt,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSuccess) KelyGreen else KelyCoralPrimary),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Done", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) KelyCoralPrimary else Color.White,
        shape = RoundedCornerShape(50),
        shadowElevation = 1.dp,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else KelyInkSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun TransactionRowCard(tx: WalletTransaction) {
    val isCredit = tx.amount >= 0
    val fmt = SimpleDateFormat("MMM dd, HH:mm", Locale.US)

    val badgeColor = when (tx.status) {
        TransactionStatus.COMPLETED, TransactionStatus.APPROVED -> KelyGreen
        TransactionStatus.PENDING_GATEWAY_INTEGRATION -> Color.Black
        TransactionStatus.SUBMITTED -> KelyCoralPrimary
        TransactionStatus.REJECTED -> KelyRed
    }
    val badgeBg = when (tx.status) {
        TransactionStatus.COMPLETED, TransactionStatus.APPROVED -> KelyGreenSoft
        TransactionStatus.PENDING_GATEWAY_INTEGRATION -> KelyDragonSoft
        TransactionStatus.SUBMITTED -> KelyCoralSoft
        TransactionStatus.REJECTED -> KelyRedSoft
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                color = if (isCredit) KelyGreenSoft else KelyRedSoft,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isCredit) KelyGreen else KelyRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, fontWeight = FontWeight.Bold, color = KelyInkPrimary, style = MaterialTheme.typography.titleSmall)
                Text(tx.subtitle, style = MaterialTheme.typography.bodySmall, color = KelyInkSecondary)
                Text(fmt.format(Date(tx.createdAtMillis)), style = MaterialTheme.typography.labelSmall, color = KelyInkSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                val sign = if (isCredit) "+" else "-"
                Text(
                    text = "${sign}₹${String.format(Locale.US, "%.2f", kotlin.math.abs(tx.amount))}",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) KelyGreen else KelyRed,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(color = badgeBg, shape = RoundedCornerShape(50)) {
                    Text(
                        text = tx.status.label,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
