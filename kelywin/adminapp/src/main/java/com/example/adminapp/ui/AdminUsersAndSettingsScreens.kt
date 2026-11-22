package com.example.adminapp.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.shared.model.AdminMember
import com.example.shared.model.AppControlConfig
import com.example.shared.model.PasswordResetRequest
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.RequestStatus
import com.example.shared.model.UserAccount
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyBorder
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyError
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelySurfaceMuted
import com.example.shared.ui.theme.KelyViolet
import com.example.shared.ui.theme.KelyWarning
import com.example.shared.ui.theme.VipDarkTop
import com.example.shared.ui.theme.VipGold
import java.util.Locale

@Composable
fun AdminManageHubScreen(
    users: List<UserAccount>,
    passwordResets: List<PasswordResetRequest>,
    appConfig: AppControlConfig,
    adminMembers: List<AdminMember>,
    currentAdminUid: String,
    onAdjustBalance: (userId: String, delta: Double, note: String) -> Unit,
    onToggleBlocked: (userId: String) -> Unit,
    onResolvePasswordReset: (resetId: String, newPassword: String, channel: String, approve: Boolean) -> Unit,
    onSaveRemoteAppControls: (
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
    ) -> Unit,
    onGrantAdmin: (uid: String, email: String) -> Unit,
    onRevokeAdmin: (uid: String) -> Unit,
    onExportSnapshot: () -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        "Users (${users.size})",
        "Resets (${passwordResets.count { it.status == RequestStatus.PENDING }})",
        "Remote App",
        "Admins"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            subTabs.forEachIndexed { index, label ->
                val active = index == selectedSubTab
                Surface(
                    color = if (active) VipDarkTop else KelySurfaceMuted,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { selectedSubTab = index }
                        .testTag("manage_subtab_$index")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            color = if (active) VipGold else KelyInkPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        when (selectedSubTab) {
            0 -> UsersDirectorySubTab(
                users = users,
                onAdjustBalance = onAdjustBalance,
                onToggleBlocked = onToggleBlocked
            )
            1 -> PasswordResetsSubTab(
                resets = passwordResets,
                onResolve = onResolvePasswordReset
            )
            2 -> RemoteAppControlsSubTab(
                appConfig = appConfig,
                onSave = onSaveRemoteAppControls,
                onExportSnapshot = onExportSnapshot
            )
            else -> AdminAllowlistSubTab(
                members = adminMembers,
                currentAdminUid = currentAdminUid,
                onGrantAdmin = onGrantAdmin,
                onRevokeAdmin = onRevokeAdmin
            )
        }
    }
}

@Composable
private fun UsersDirectorySubTab(
    users: List<UserAccount>,
    onAdjustBalance: (String, Double, String) -> Unit,
    onToggleBlocked: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var adjustTarget by remember { mutableStateOf<UserAccount?>(null) }

    val filtered = remember(users, query) {
        val q = query.trim().lowercase(Locale.US)
        if (q.isEmpty()) users
        else users.filter {
            it.kwId.lowercase(Locale.US).contains(q) ||
                it.phoneNumber.contains(q) ||
                it.email.lowercase(Locale.US).contains(q) ||
                it.secretCode.lowercase(Locale.US).contains(q)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search phone, email, KW ID, or Secret Code") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(filtered, key = { it.userId }) { user ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (user.isBlocked) KelyError else KelyBorder),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = KelyCoralPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${user.kwId} · ${user.displayPhone}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KelyInkPrimary,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            if (user.email.isNotBlank()) {
                                Text(user.email, fontSize = 12.sp, color = KelyInkSecondary)
                            }
                            Text(
                                text = "Secret Code: ${user.secretCode.ifBlank { "Pending login sync" }}",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KelyViolet
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", user.balance)}",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = KelyGreen
                            )
                            Text(
                                text = if (user.isBlocked) "BLOCKED" else "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (user.isBlocked) KelyError else KelyGreen
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { adjustTarget = user },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Adjust ₹", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onToggleBlocked(user.userId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user.isBlocked) KelyGreen else KelyError
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (user.isBlocked) "Unblock" else "Block", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    adjustTarget?.let { target ->
        var amountStr by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("Admin wallet adjustment") }
        AlertDialog(
            onDismissRequest = { adjustTarget = null },
            title = { Text("Adjust Balance · ${target.kwId}", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Current Balance: ₹${String.format(Locale.US, "%.2f", target.balance)}",
                        fontFamily = JetBrainsMono,
                        color = KelyInkSecondary
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (e.g. 500 or -200)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Ledger Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val delta = amountStr.toDoubleOrNull() ?: 0.0
                        onAdjustBalance(target.userId, delta, note)
                        adjustTarget = null
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { adjustTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PasswordResetsSubTab(
    resets: List<PasswordResetRequest>,
    onResolve: (resetId: String, newPassword: String, channel: String, approve: Boolean) -> Unit
) {
    var activeReq by remember { mutableStateOf<PasswordResetRequest?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (resets.isEmpty()) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, KelyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No password reset requests right now.",
                        color = KelyInkSecondary,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(resets, key = { it.id }) { req ->
                val color = when (req.status) {
                    RequestStatus.PENDING -> KelyWarning
                    RequestStatus.APPROVED -> KelyGreen
                    RequestStatus.REJECTED -> KelyError
                }
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, KelyBorder),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = KelyViolet)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(req.identifier, fontWeight = FontWeight.ExtraBold, color = KelyInkPrimary)
                            }
                            Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                                Text(
                                    text = req.status.label,
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (req.status == RequestStatus.PENDING) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onResolve(req.id, "", "Admin Rejected", false) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reject", color = KelyError)
                                }
                                Button(
                                    onClick = { activeReq = req },
                                    colors = ButtonDefaults.buttonColors(containerColor = KelyViolet),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Text("Set New Password", color = Color.White)
                                }
                            }
                        } else if (req.assignedPassword.isNotBlank()) {
                            Text(
                                text = "Assigned Password: ${req.assignedPassword} (${req.deliveryChannel})",
                                fontFamily = JetBrainsMono,
                                fontSize = 12.sp,
                                color = KelyInkSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    activeReq?.let { req ->
        var newPass by remember { mutableStateOf("") }
        var channel by remember { mutableStateOf("WhatsApp / Direct Support") }
        AlertDialog(
            onDismissRequest = { activeReq = null },
            title = { Text("Reset Password for ${req.identifier}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password (min 6 chars)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = channel,
                        onValueChange = { channel = it },
                        label = { Text("Delivery Channel Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResolve(req.id, newPass, channel, true)
                        activeReq = null
                    }
                ) {
                    Text("Approve & Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeReq = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RemoteAppControlsSubTab(
    appConfig: AppControlConfig,
    onSave: (
        String, Boolean, String, Boolean, Boolean, Boolean, Boolean, Double, Double, PayoutMultipliers
    ) -> Unit,
    onExportSnapshot: () -> Unit
) {
    var announcement by remember { mutableStateOf(appConfig.announcementText) }
    var maintenanceMode by remember { mutableStateOf(appConfig.maintenanceMode) }
    var maintenanceMessage by remember { mutableStateOf(appConfig.maintenanceMessage) }
    var enableRegistration by remember { mutableStateOf(appConfig.enableRegistration) }
    var enableDeposits by remember { mutableStateOf(appConfig.enableDeposits) }
    var enableWithdrawals by remember { mutableStateOf(appConfig.enableWithdrawals) }
    var enableNumberBetting by remember { mutableStateOf(appConfig.enableNumberBetting) }
    var minBet by remember { mutableStateOf(appConfig.minBetAmount.toInt().toString()) }
    var bonusAmt by remember { mutableStateOf(appConfig.firstDepositBonusAmount.toInt().toString()) }
    var greenRedMult by remember { mutableStateOf(appConfig.multipliers.greenRedMultiplier.toString()) }
    var violetMult by remember { mutableStateOf(appConfig.multipliers.violetMultiplier.toString()) }
    var numberMult by remember { mutableStateOf(appConfig.multipliers.numberMultiplier.toString()) }
    var dtMult by remember { mutableStateOf(appConfig.multipliers.dragonTigerMultiplier.toString()) }

    LaunchedEffect(appConfig) {
        announcement = appConfig.announcementText
        maintenanceMode = appConfig.maintenanceMode
        maintenanceMessage = appConfig.maintenanceMessage
        enableRegistration = appConfig.enableRegistration
        enableDeposits = appConfig.enableDeposits
        enableWithdrawals = appConfig.enableWithdrawals
        enableNumberBetting = appConfig.enableNumberBetting
        minBet = appConfig.minBetAmount.toInt().toString()
        bonusAmt = appConfig.firstDepositBonusAmount.toInt().toString()
        greenRedMult = appConfig.multipliers.greenRedMultiplier.toString()
        violetMult = appConfig.multipliers.violetMultiplier.toString()
        numberMult = appConfig.multipliers.numberMultiplier.toString()
        dtMult = appConfig.multipliers.dragonTigerMultiplier.toString()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (maintenanceMode) KelyError else KelyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Remote User APK Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = KelyInkPrimary
                    )

                    RemoteSwitchRow("Maintenance Mode (Lock User App)", maintenanceMode) { maintenanceMode = it }
                    if (maintenanceMode) {
                        OutlinedTextField(
                            value = maintenanceMessage,
                            onValueChange = { maintenanceMessage = it },
                            label = { Text("Maintenance Banner Message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    RemoteSwitchRow("Allow New Registrations", enableRegistration) { enableRegistration = it }
                    RemoteSwitchRow("Allow UPI Deposits", enableDeposits) { enableDeposits = it }
                    RemoteSwitchRow("Allow Withdrawals", enableWithdrawals) { enableWithdrawals = it }
                    RemoteSwitchRow("Enable 0–9 Number Betting", enableNumberBetting) { enableNumberBetting = it }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minBet,
                            onValueChange = { minBet = it.filter(Char::isDigit) },
                            label = { Text("Min Bet (₹1+)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bonusAmt,
                            onValueChange = { bonusAmt = it.filter(Char::isDigit) },
                            label = { Text("1st Deposit Bonus ₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = greenRedMult,
                            onValueChange = { greenRedMult = it },
                            label = { Text("Green/Red X") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = violetMult,
                            onValueChange = { violetMult = it },
                            label = { Text("Violet X") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = numberMult,
                            onValueChange = { numberMult = it },
                            label = { Text("0–9 Ball X") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dtMult,
                            onValueChange = { dtMult = it },
                            label = { Text("Dragon/Tiger X") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = announcement,
                        onValueChange = { announcement = it },
                        label = { Text("Live Announcement Banner") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSave(
                                announcement,
                                maintenanceMode,
                                maintenanceMessage,
                                enableRegistration,
                                enableDeposits,
                                enableWithdrawals,
                                enableNumberBetting,
                                (minBet.toDoubleOrNull() ?: 1.0).coerceAtLeast(1.0),
                                (bonusAmt.toDoubleOrNull() ?: 12.0).coerceAtLeast(0.0),
                                PayoutMultipliers(
                                    greenRedMultiplier = greenRedMult.toDoubleOrNull() ?: 2.0,
                                    violetMultiplier = violetMult.toDoubleOrNull() ?: 4.5,
                                    numberMultiplier = numberMult.toDoubleOrNull() ?: 9.0,
                                    dragonTigerMultiplier = dtMult.toDoubleOrNull() ?: 2.0,
                                    platformFeeRate = appConfig.multipliers.platformFeeRate
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_remote_controls_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Publish Changes to User APK", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onExportSnapshot,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Cloud JSON Snapshot", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAllowlistSubTab(
    members: List<AdminMember>,
    currentAdminUid: String,
    onGrantAdmin: (String, String) -> Unit,
    onRevokeAdmin: (String) -> Unit
) {
    var uidInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, KelyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddModerator, contentDescription = null, tint = KelyCoralPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grant Admin APK Access (/admins)",
                            fontWeight = FontWeight.ExtraBold,
                            color = KelyInkPrimary,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    OutlinedTextField(
                        value = uidInput,
                        onValueChange = { uidInput = it },
                        label = { Text("Firebase Auth UID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Google Account Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onGrantAdmin(uidInput, emailInput)
                            uidInput = ""
                            emailInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VipDarkTop),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VipGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Admin Member", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items(members, key = { it.uid }) { admin ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, KelyBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(admin.email, fontWeight = FontWeight.ExtraBold, color = KelyInkPrimary)
                        Text(
                            text = "UID: ${admin.uid}",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = KelyInkSecondary
                        )
                    }
                    if (admin.uid != currentAdminUid) {
                        TextButton(onClick = { onRevokeAdmin(admin.uid) }) {
                            Text("Revoke", color = KelyError, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(color = KelyGreen.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                            Text(
                                text = "YOU",
                                color = KelyGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, color = KelyInkPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
