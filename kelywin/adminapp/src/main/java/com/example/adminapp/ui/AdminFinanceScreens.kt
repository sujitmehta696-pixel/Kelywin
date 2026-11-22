package com.example.adminapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.shared.model.AdminGatewaySecret
import com.example.shared.model.DepositRequest
import com.example.shared.model.RequestStatus
import com.example.shared.model.UpiQrConfig
import com.example.shared.model.WithdrawalRequest
import com.example.shared.ui.components.UpiQrCodeCanvas
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyBorder
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyError
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyGreenSoft
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelyWarning
import com.example.shared.ui.theme.VipDarkTop
import com.example.shared.ui.theme.VipGold
import java.util.Locale

@Composable
fun AdminGatewayAndDepositsScreen(
    upiConfig: UpiQrConfig,
    famGatewaySecret: AdminGatewaySecret,
    firstDepositBonusAmount: Double,
    deposits: List<DepositRequest>,
    onSaveFamGatewayConfig: (apiKey: String, backendUrl: String, enabled: Boolean) -> Unit,
    onSaveGatewayConfig: (UpiQrConfig) -> Unit,
    onApproveDeposit: (String) -> Unit,
    onRejectDeposit: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var famApiKey by remember { mutableStateOf(famGatewaySecret.apiKey) }
    var famBackendUrl by remember { mutableStateOf(famGatewaySecret.backendUrl.ifBlank { upiConfig.gatewayApiUrl }) }
    var famEnabled by remember { mutableStateOf(famGatewaySecret.enabled) }
    var showSecretKey by remember { mutableStateOf(false) }

    var gatewayName by remember { mutableStateOf(upiConfig.gatewayName) }
    var gatewayApiUrl by remember { mutableStateOf(upiConfig.gatewayApiUrl) }
    var merchantKeyId by remember { mutableStateOf(upiConfig.merchantKeyId) }
    var merchantUpiPool by remember { mutableStateOf(upiConfig.merchantUpiPool) }
    var merchantName by remember { mutableStateOf(upiConfig.merchantName) }
    var qrLabel by remember { mutableStateOf(upiConfig.qrLabel) }
    var minDeposit by remember { mutableStateOf(upiConfig.minDeposit.toString()) }
    var maxDeposit by remember { mutableStateOf(upiConfig.maxDeposit.toString()) }

    LaunchedEffect(famGatewaySecret) {
        famApiKey = famGatewaySecret.apiKey
        if (famGatewaySecret.backendUrl.isNotBlank()) {
            famBackendUrl = famGatewaySecret.backendUrl
        }
        famEnabled = famGatewaySecret.enabled
    }

    LaunchedEffect(upiConfig) {
        gatewayName = upiConfig.gatewayName
        gatewayApiUrl = upiConfig.gatewayApiUrl
        if (famBackendUrl.isBlank() && upiConfig.gatewayApiUrl.isNotBlank()) {
            famBackendUrl = upiConfig.gatewayApiUrl
        }
        merchantKeyId = upiConfig.merchantKeyId
        merchantUpiPool = upiConfig.merchantUpiPool
        merchantName = upiConfig.merchantName
        qrLabel = upiConfig.qrLabel
        minDeposit = upiConfig.minDeposit.toString()
        maxDeposit = upiConfig.maxDeposit.toString()
    }

    val isFamLive = famEnabled && famBackendUrl.startsWith("http", ignoreCase = true) && famApiKey.isNotBlank()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
    ) {
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, if (isFamLive) KelyGreen.copy(alpha = 0.55f) else VipGold),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = KelyCoralPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "FamGateway Auto-UPI (No UTR)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KelyInkPrimary
                                )
                                Text(
                                    text = "Secret key stays Admin & Backend only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KelyInkSecondary
                                )
                            }
                        }
                        Surface(
                            color = if (isFamLive) KelyGreenSoft else KelyWarning.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (isFamLive) "LIVE AUTO" else "NEEDS SETUP",
                                color = if (isFamLive) KelyGreen else KelyWarning,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Enable Auto Without-UTR Deposit",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = KelyInkPrimary
                        )
                        Switch(
                            checked = famEnabled,
                            onCheckedChange = { famEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KelyGreen)
                        )
                    }

                    OutlinedTextField(
                        value = famBackendUrl,
                        onValueChange = { famBackendUrl = it },
                        label = { Text("Backend Server URL (Gateway URL)") },
                        placeholder = { Text("https://kelywin-backend.onrender.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("famgateway_backend_url_input")
                    )

                    OutlinedTextField(
                        value = famApiKey,
                        onValueChange = { famApiKey = it },
                        label = { Text("FamGateway Secret API Key") },
                        placeholder = { Text("Paste key from famgateway.in/api-keys.php") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = KelyCoralPrimary) },
                        visualTransformation = if (showSecretKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showSecretKey = !showSecretKey }) {
                                Icon(
                                    imageVector = if (showSecretKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showSecretKey) "Hide Secret Key" else "Show Secret Key"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("famgateway_secret_key_input")
                    )

                    val cleanPreviewUrl = famBackendUrl.trim().trimEnd('/')
                    val webhookPreview = if (cleanPreviewUrl.startsWith("http", ignoreCase = true)) {
                        "$cleanPreviewUrl/famgateway-webhook"
                    } else {
                        "https://your-backend-url/famgateway-webhook"
                    }
                    Surface(
                        color = KelyBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, KelyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(webhookPreview))
                                Toast.makeText(context, "Webhook URL copied!", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FamGateway Webhook URL (tap to copy)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KelyInkSecondary
                                )
                                Text(
                                    text = webhookPreview,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = KelyInkPrimary
                                )
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Webhook URL", tint = KelyCoralPrimary)
                        }
                    }

                    Button(
                        onClick = {
                            onSaveFamGatewayConfig(famApiKey, famBackendUrl, famEnabled)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_famgateway_secret_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save FamGateway Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, KelyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, tint = KelyCoralPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dynamic UPI Gateway Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = KelyInkPrimary
                        )
                    }
                    Text(
                        text = "Configure your dynamic UPI QR pool or external merchant gateway endpoint. Syncs immediately to the User APK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KelyInkSecondary
                    )

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val firstUpi = merchantUpiPool.split(",", "\n", ";").firstOrNull()?.trim().orEmpty().ifBlank { "kelywin@okaxis" }
                        UpiQrCodeCanvas(
                            upiPayload = "upi://pay?pa=$firstUpi&pn=$merchantName&am=${minDeposit}&cu=INR",
                            size = 116.dp
                        )
                    }

                    OutlinedTextField(
                        value = gatewayName,
                        onValueChange = { gatewayName = it },
                        label = { Text("Gateway Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gatewayApiUrl,
                        onValueChange = { gatewayApiUrl = it },
                        label = { Text("Gateway Create-QR API URL (Optional)") },
                        placeholder = { Text("https://gateway.yourdomain.com/v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = merchantKeyId,
                        onValueChange = { merchantKeyId = it },
                        label = { Text("Merchant Key ID (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = merchantUpiPool,
                        onValueChange = { merchantUpiPool = it },
                        label = { Text("Rotating Merchant UPI Pool (comma-separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = merchantName,
                        onValueChange = { merchantName = it },
                        label = { Text("Payee Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = qrLabel,
                        onValueChange = { qrLabel = it },
                        label = { Text("Gateway Note / Instruction") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = minDeposit,
                            onValueChange = { minDeposit = it.filter(Char::isDigit) },
                            label = { Text("Min ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxDeposit,
                            onValueChange = { maxDeposit = it.filter(Char::isDigit) },
                            label = { Text("Max ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            onSaveGatewayConfig(
                                UpiQrConfig(
                                    gatewayName = gatewayName.trim(),
                                    gatewayApiUrl = gatewayApiUrl.trim(),
                                    merchantKeyId = merchantKeyId.trim(),
                                    merchantUpiPool = merchantUpiPool.trim(),
                                    merchantName = merchantName.trim(),
                                    qrLabel = qrLabel.trim(),
                                    minDeposit = (minDeposit.toIntOrNull() ?: 100).coerceAtLeast(1),
                                    maxDeposit = (maxDeposit.toIntOrNull() ?: 50000).coerceAtLeast(100)
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VipDarkTop),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_gateway_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = VipGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Sync Gateway QR", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "UPI Deposit Queue (${deposits.count { it.status == RequestStatus.PENDING }} Pending · +₹${firstDepositBonusAmount.toInt()} 1st Bonus)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = KelyInkPrimary
            )
        }

        if (deposits.isEmpty()) {
            item {
                EmptyFinanceCard("No UPI deposit requests yet.")
            }
        } else {
            items(deposits, key = { it.id }) { dep ->
                val statusColor = when (dep.status) {
                    RequestStatus.PENDING -> KelyWarning
                    RequestStatus.APPROVED -> KelyGreen
                    RequestStatus.REJECTED -> KelyError
                }
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (dep.status == RequestStatus.PENDING) KelyWarning.copy(alpha = 0.6f) else KelyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", dep.amount)}",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = KelyInkPrimary
                                )
                                Text(
                                    text = "${dep.userPhone.ifBlank { dep.userId }} · ${dep.orderId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KelyInkSecondary
                                )
                            }
                            Surface(color = statusColor.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                                Text(
                                    text = dep.status.label,
                                    color = statusColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        val utrDisplay = dep.utrNumber.ifBlank { "Auto-verify (FamGateway · No UTR)" }
                        val upiDisplay = dep.upiIdUsed.ifBlank { dep.gatewayName }
                        Text(
                            text = "UTR: $utrDisplay · UPI: $upiDisplay",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = KelyInkPrimary
                        )

                        if (dep.bonusApplied > 0.0) {
                            Text(
                                text = "1st-Deposit Bonus Applied: +₹${dep.bonusApplied.toInt()}",
                                color = KelyGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (dep.status == RequestStatus.PENDING) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { onRejectDeposit(dep.id) },
                                    border = BorderStroke(1.dp, KelyError),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("reject_deposit_${dep.id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = KelyError)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject", color = KelyError, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onApproveDeposit(dep.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = KelyGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(44.dp)
                                        .testTag("approve_deposit_${dep.id}")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve + Credit", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminWithdrawalsScreen(
    withdrawals: List<WithdrawalRequest>,
    onApproveWithdrawal: (String) -> Unit,
    onRejectWithdrawal: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
    ) {
        item {
            Text(
                text = "Withdrawal Payout Queue (${withdrawals.count { it.status == RequestStatus.PENDING }} Pending)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = KelyInkPrimary
            )
        }

        if (withdrawals.isEmpty()) {
            item {
                EmptyFinanceCard("No withdrawal requests submitted yet.")
            }
        } else {
            items(withdrawals, key = { it.id }) { wd ->
                val statusColor = when (wd.status) {
                    RequestStatus.PENDING -> KelyWarning
                    RequestStatus.APPROVED -> KelyGreen
                    RequestStatus.REJECTED -> KelyError
                }
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (wd.status == RequestStatus.PENDING) KelyCoralPrimary.copy(alpha = 0.55f) else KelyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "₹${String.format(Locale.US, "%.2f", wd.amount)}",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = KelyInkPrimary
                                )
                                Text(
                                    text = "${wd.userPhone.ifBlank { wd.userId }} · ${wd.methodType.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KelyInkSecondary
                                )
                            }
                            Surface(color = statusColor.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                                Text(
                                    text = wd.status.label,
                                    color = statusColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "${wd.accountHolder} · ${wd.accountIdentifier}${if (wd.ifscCode.isNotBlank()) " · IFSC ${wd.ifscCode}" else ""}",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = KelyInkPrimary
                        )

                        if (wd.status == RequestStatus.PENDING) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { onRejectWithdrawal(wd.id) },
                                    border = BorderStroke(1.dp, KelyError),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("reject_withdrawal_${wd.id}")
                                ) {
                                    Text("Reject & Refund", color = KelyError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { onApproveWithdrawal(wd.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = KelyGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(44.dp)
                                        .testTag("approve_withdrawal_${wd.id}")
                                ) {
                                    Text("Mark Paid", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFinanceCard(message: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, KelyBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            color = KelyInkSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(20.dp)
        )
    }
}
