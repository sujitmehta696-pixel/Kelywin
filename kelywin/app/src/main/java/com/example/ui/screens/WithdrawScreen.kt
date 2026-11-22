package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.PayoutMethod
import com.example.shared.model.PayoutMethodType
import com.example.shared.model.UserAccount
import com.example.shared.model.WalletTransaction
import com.example.shared.ui.components.KelywinCoralHeader
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyBorder
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyCoralSoft
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyInkMuted
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelyRed
import com.example.shared.ui.theme.KelyRedSoft
import com.example.shared.ui.theme.KelySurfaceMuted
import java.util.Locale

@Composable
fun WithdrawScreen(
    user: UserAccount?,
    payoutMethods: List<PayoutMethod>,
    withdrawalHistory: List<WalletTransaction>,
    enableWithdrawals: Boolean = true,
    onSavePayoutMethod: (
        existingId: String?,
        type: PayoutMethodType,
        label: String,
        holder: String,
        identifier: String,
        ifsc: String,
        makeDefault: Boolean,
        onDone: () -> Unit
    ) -> Unit,
    onSelectDefaultMethod: (String) -> Unit,
    onDeleteMethod: (String) -> Unit,
    onSubmitWithdrawal: (Double, PayoutMethod, () -> Unit) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedMethodId by remember(payoutMethods) {
        mutableStateOf(payoutMethods.firstOrNull { it.isDefault }?.id ?: payoutMethods.firstOrNull()?.id)
    }
    var amountInput by remember { mutableStateOf("") }
    var editingMethod by remember { mutableStateOf<PayoutMethod?>(null) }
    var showBindDialog by remember { mutableStateOf(false) }

    LaunchedEffect(payoutMethods) {
        if (selectedMethodId == null || payoutMethods.none { it.id == selectedMethodId }) {
            selectedMethodId = payoutMethods.firstOrNull { it.isDefault }?.id ?: payoutMethods.firstOrNull()?.id
        }
    }

    val selectedMethod = payoutMethods.find { it.id == selectedMethodId }
    val availableBalance = user?.balance ?: 0.0

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
            // Balance Summary
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
                            Text("Withdrawable Balance", style = MaterialTheme.typography.bodySmall, color = KelyInkSecondary)
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", availableBalance)}",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = KelyInkPrimary
                            )
                        }
                        Surface(
                            color = KelyCoralSoft,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    editingMethod = null
                                    showBindDialog = true
                                }
                                .testTag("add_payout_method_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add UPI/Bank", color = KelyCoralPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (!enableWithdrawals) {
                item {
                    Surface(
                        color = KelyRedSoft,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Withdrawals are temporarily paused by Admin. Please check back later.",
                            color = KelyRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Saved Payout Methods (Direct No-OTP Binding)
            item {
                Text(
                    text = "Saved UPI & Bank Accounts (No OTP)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KelyInkPrimary
                )
            }

            if (payoutMethods.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, KelyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                editingMethod = null
                                showBindDialog = true
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(34.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No UPI or Bank Account Added", fontWeight = FontWeight.Bold, color = KelyInkPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap to bind your UPI ID or Bank Account directly without any OTP.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KelyInkSecondary
                            )
                        }
                    }
                }
            } else {
                items(payoutMethods, key = { it.id }) { method ->
                    val isSelected = method.id == selectedMethodId
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(if (isSelected) 1.6.dp else 1.dp, if (isSelected) KelyCoralPrimary else KelyBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedMethodId = method.id
                                onSelectDefaultMethod(method.id)
                            }
                            .testTag("payout_card_${method.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Select payout method",
                                tint = if (isSelected) KelyCoralPrimary else KelyInkMuted
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                color = KelyCoralSoft,
                                shape = CircleShape,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (method.type == PayoutMethodType.UPI) Icons.Default.QrCode2 else Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = KelyCoralPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${method.label} (${method.type.label})", fontWeight = FontWeight.Bold, color = KelyInkPrimary)
                                Text(method.accountHolder, style = MaterialTheme.typography.bodySmall, color = KelyInkSecondary)
                                Text(
                                    text = if (method.ifscCode.isNotBlank()) "${method.accountIdentifier} · ${method.ifscCode}" else method.accountIdentifier,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 12.sp,
                                    color = KelyInkPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingMethod = method
                                    showBindDialog = true
                                },
                                modifier = Modifier.testTag("edit_payout_${method.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = KelyInkSecondary)
                            }
                            IconButton(
                                onClick = { onDeleteMethod(method.id) },
                                modifier = Modifier.testTag("delete_payout_${method.id}")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = KelyInkSecondary)
                            }
                        }
                    }
                }
            }

            // Withdrawal Form
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Withdrawal Amount", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = { amountInput = availableBalance.toInt().toString() },
                                modifier = Modifier.testTag("withdraw_all_button")
                            ) {
                                Text("All", color = KelyCoralPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                            placeholder = { Text("Min ₹100") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KelyCoralPrimary,
                                unfocusedBorderColor = KelyBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_amount_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val amt = amountInput.toDoubleOrNull() ?: 0.0
                                if (selectedMethod != null) {
                                    onSubmitWithdrawal(amt, selectedMethod) {
                                        amountInput = ""
                                    }
                                }
                            },
                            enabled = enableWithdrawals && selectedMethod != null && (amountInput.toDoubleOrNull() ?: 0.0) >= 100.0,
                            colors = ButtonDefaults.buttonColors(containerColor = KelyGreen),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("withdraw_submit_button")
                        ) {
                            Text("Confirm Withdraw", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (withdrawalHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Withdrawal History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KelyInkPrimary
                    )
                }
                items(withdrawalHistory.take(10), key = { it.id }) { tx ->
                    TransactionRowCard(tx = tx)
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }

    if (showBindDialog) {
        BindPayoutMethodDialog(
            existing = editingMethod,
            onDismiss = { showBindDialog = false },
            onSave = { type, label, holder, identifier, ifsc ->
                onSavePayoutMethod(
                    editingMethod?.id,
                    type,
                    label,
                    holder,
                    identifier,
                    ifsc,
                    true
                ) {
                    showBindDialog = false
                }
            }
        )
    }
}

@Composable
private fun BindPayoutMethodDialog(
    existing: PayoutMethod?,
    onDismiss: () -> Unit,
    onSave: (
        type: PayoutMethodType,
        label: String,
        holder: String,
        identifier: String,
        ifsc: String
    ) -> Unit
) {
    var type by remember(existing) { mutableStateOf(existing?.type ?: PayoutMethodType.UPI) }
    var label by remember(existing) { mutableStateOf(existing?.label ?: "") }
    var holder by remember(existing) { mutableStateOf(existing?.accountHolder ?: "") }
    var identifier by remember(existing) { mutableStateOf(existing?.accountIdentifier ?: "") }
    var ifsc by remember(existing) { mutableStateOf(existing?.ifscCode ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = if (existing == null) "Bind Payout Account (No OTP)" else "Edit Payout Account",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PayoutMethodType.entries.forEach { option ->
                        val active = type == option
                        Surface(
                            color = if (active) KelyCoralPrimary else KelySurfaceMuted,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable { type = option }
                                .testTag("bind_type_${option.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                Text(
                                    text = option.label,
                                    color = if (active) Color.White else KelyInkSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(if (type == PayoutMethodType.UPI) "App / Label (e.g. GPay)" else "Bank Name (e.g. HDFC)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("bind_label_input")
                )
                OutlinedTextField(
                    value = holder,
                    onValueChange = { holder = it },
                    label = { Text("Account Holder Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("bind_holder_input")
                )
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text(if (type == PayoutMethodType.UPI) "UPI ID (e.g. name@okaxis)" else "Account Number") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("bind_identifier_input")
                )
                if (type == PayoutMethodType.BANK) {
                    OutlinedTextField(
                        value = ifsc,
                        onValueChange = { ifsc = it.uppercase(Locale.US) },
                        label = { Text("IFSC Code") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("bind_ifsc_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(type, label, holder, identifier, ifsc) },
                colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("save_payout_button")
            ) {
                Text("Save Directly", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = KelyInkSecondary)
            }
        }
    )
}
