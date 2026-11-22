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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.UserAccount
import com.example.shared.model.UserSettings
import com.example.shared.ui.components.KelywinCoralHeader
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyBorder
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyCoralSoft
import com.example.shared.ui.theme.KelyDragonSoft
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyHeaderGradientBottom
import com.example.shared.ui.theme.KelyHeaderGradientTop
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelyRed
import java.util.Locale

@Composable
fun ProfileScreen(
    user: UserAccount?,
    totalBetsCount: Int,
    onCopyInviteCode: (String) -> Unit,
    onCopySecretCode: (String) -> Unit,
    onNavigateBalance: () -> Unit,
    onNavigateDeposit: () -> Unit,
    onNavigateWithdraw: () -> Unit,
    onNavigateMyBets: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
    ) {
        KelywinCoralHeader(
            showBackButton = false,
            rightContent = {
                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier.testTag("profile_settings_icon")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Coral Member Profile Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(KelyHeaderGradientTop, KelyHeaderGradientBottom))
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Member Avatar",
                                            tint = KelyCoralPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user?.displayPhone ?: "Kelywin Member",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Kelywin ID: ${user?.kwId ?: "---"}",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontFamily = JetBrainsMono,
                                        fontSize = 12.sp
                                    )
                                    if (!user?.email.isNullOrBlank()) {
                                        Text(
                                            text = user?.email ?: "",
                                            color = Color.White.copy(alpha = 0.85f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Invite Code Copy Pill
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Invite Code",
                                            color = Color.White.copy(alpha = 0.85f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = user?.inviteCode ?: "---",
                                            color = Color.White,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                val code = user?.inviteCode.orEmpty()
                                                if (code.isNotEmpty()) {
                                                    clipboard.setText(AnnotatedString(code))
                                                    onCopyInviteCode(code)
                                                }
                                            }
                                            .testTag("copy_invite_code_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy", color = KelyCoralPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fixed Secret Recovery Code Pill
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Fixed Secret Recovery Code",
                                            color = Color.White.copy(alpha = 0.85f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = user?.secretCode?.ifBlank { "Generating..." } ?: "---",
                                            color = Color.White,
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                val code = user?.secretCode.orEmpty()
                                                if (code.isNotEmpty()) {
                                                    clipboard.setText(AnnotatedString(code))
                                                    onCopySecretCode(code)
                                                }
                                            }
                                            .testTag("profile_copy_secret_code")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Key, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy Key", color = KelyCoralPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Wallet Mini Summary
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
                            Text("Wallet Balance", style = MaterialTheme.typography.bodySmall, color = KelyInkSecondary)
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", user?.balance ?: 0.0)}",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = KelyInkPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Bets", style = MaterialTheme.typography.bodySmall, color = KelyInkSecondary)
                            Text(
                                text = totalBetsCount.toString(),
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = KelyCoralPrimary
                            )
                        }
                    }
                }
            }

            // Navigation Menu
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ProfileMenuRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Balance & Ledger",
                            subtitle = "View deposits, bets, wins & withdrawals",
                            onClick = onNavigateBalance,
                            modifier = Modifier.testTag("profile_menu_balance")
                        )
                        HorizontalDivider(color = KelyBorder, thickness = 0.6.dp)
                        ProfileMenuRow(
                            icon = Icons.Default.AddCard,
                            title = "Deposit Funds",
                            subtitle = "Gateway-ready recharge channels",
                            onClick = onNavigateDeposit,
                            modifier = Modifier.testTag("profile_menu_deposit")
                        )
                        HorizontalDivider(color = KelyBorder, thickness = 0.6.dp)
                        ProfileMenuRow(
                            icon = Icons.Default.Payments,
                            title = "Withdraw Funds",
                            subtitle = "Bind UPI or Bank without OTP",
                            onClick = onNavigateWithdraw,
                            modifier = Modifier.testTag("profile_menu_withdraw")
                        )
                        HorizontalDivider(color = KelyBorder, thickness = 0.6.dp)
                        ProfileMenuRow(
                            icon = Icons.Default.ReceiptLong,
                            title = "My Bets",
                            subtitle = "Track live & settled round bets",
                            onClick = onNavigateMyBets,
                            modifier = Modifier.testTag("profile_menu_my_bets")
                        )
                        HorizontalDivider(color = KelyBorder, thickness = 0.6.dp)
                        ProfileMenuRow(
                            icon = Icons.Default.Settings,
                            title = "Settings & Password",
                            subtitle = "Change password (No OTP), alerts & display",
                            onClick = onNavigateSettings,
                            modifier = Modifier.testTag("profile_menu_settings")
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Surface(
            color = KelyCoralSoft,
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = KelyInkPrimary, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = KelyInkSecondary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = KelyInkSecondary)
    }
}

@Composable
fun SettingsScreen(
    user: UserAccount?,
    googleEmail: String?,
    settings: UserSettings,
    onCopySecretCode: (String) -> Unit,
    onChangePasswordNoOtp: (current: String, newPass: String, confirmPass: String, onDone: () -> Unit) -> Unit,
    onSaveEmail: (String) -> Unit,
    onUpdateSettings: (UserSettings) -> Unit,
    onLogout: () -> Unit,
    onSwitchGoogleAccount: () -> Unit,
    onBackClick: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var emailInput by remember(user?.email) { mutableStateOf(user?.email ?: "") }

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
            // Fixed Secret Recovery Code Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = KelyCoralPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fixed Secret Recovery Code", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Keep this fixed key safe. If you ever forget your login password, enter your Kelywin ID / phone / email + this Secret Code on Forgot Password to recover your real password without OTP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KelyInkSecondary
                        )
                        Surface(
                            color = KelyDragonSoft,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "KW ID: ${user?.kwId ?: "---"}",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black.copy(alpha = 0.78f)
                                    )
                                    Text(
                                        text = user?.secretCode?.ifBlank { "Log in once to initialize" } ?: "---",
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Color.Black,
                                        modifier = Modifier.testTag("settings_secret_code_text")
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val code = user?.secretCode.orEmpty()
                                        if (code.isNotEmpty()) {
                                            clipboard.setText(AnnotatedString(code))
                                            onCopySecretCode(code)
                                        }
                                    },
                                    modifier = Modifier.testTag("copy_secret_code_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy secret code", tint = Color.Black)
                                }
                            }
                        }
                    }
                }
            }

            // Change Password (No OTP)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = KelyCoralPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Password (No OTP)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = currentPass,
                            onValueChange = { currentPass = it },
                            label = { Text("Current Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KelyCoralPrimary, unfocusedBorderColor = KelyBorder),
                            modifier = Modifier.fillMaxWidth().testTag("settings_current_password")
                        )
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KelyCoralPrimary, unfocusedBorderColor = KelyBorder),
                            modifier = Modifier.fillMaxWidth().testTag("settings_new_password")
                        )
                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KelyCoralPrimary, unfocusedBorderColor = KelyBorder),
                            modifier = Modifier.fillMaxWidth().testTag("settings_confirm_password")
                        )
                        Button(
                            onClick = {
                                onChangePasswordNoOtp(currentPass, newPass, confirmPass) {
                                    currentPass = ""
                                    newPass = ""
                                    confirmPass = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_change_password_button")
                        ) {
                            Text("Update Password Directly", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Link Email for Email Tab Login
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = KelyCoralPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recovery / Login Email", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("you@example.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = KelyCoralPrimary, unfocusedBorderColor = KelyBorder),
                            modifier = Modifier.fillMaxWidth().testTag("settings_email_input")
                        )
                        Button(
                            onClick = { onSaveEmail(emailInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = KelyGreen),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("settings_save_email_button")
                        ) {
                            Text("Save Email", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Preferences
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Game & Display Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        SettingSwitchRow(
                            icon = Icons.Default.NotificationsActive,
                            title = "Win / Loss Result Popup",
                            subtitle = "Show settlement card when your round finishes",
                            checked = settings.showResultPopup,
                            onCheckedChange = { onUpdateSettings(settings.copy(showResultPopup = it)) },
                            testTag = "setting_toggle_popup"
                        )
                        HorizontalDivider(color = KelyBorder, thickness = 0.6.dp)
                        SettingSwitchRow(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            title = "Sound Alerts",
                            subtitle = "Enable game feedback indicators",
                            checked = settings.soundEffectsEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(soundEffectsEnabled = it)) },
                            testTag = "setting_toggle_sound"
                        )
                        HorizontalDivider(color = KelyBorder, thickness = 0.6.dp)
                        SettingSwitchRow(
                            icon = Icons.Default.ViewCompact,
                            title = "Compact Number Balls",
                            subtitle = "Smaller 0–9 grid for dense viewing",
                            checked = settings.compactGridMode,
                            onCheckedChange = { onUpdateSettings(settings.copy(compactGridMode = it)) },
                            testTag = "setting_toggle_compact"
                        )
                    }
                }
            }

            // Cloud Sync Info & Google Account Switch
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = KelyGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Cloud Sync Active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = googleEmail?.ifBlank { "Google Account Connected" } ?: "Google Account Connected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KelyInkSecondary
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onSwitchGoogleAccount,
                            border = BorderStroke(1.dp, KelyBorder),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("settings_switch_google_button")
                        ) {
                            Text("Switch Google Account", color = KelyInkPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Logout
            item {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = KelyRed),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = KelyInkPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = KelyInkSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KelyCoralPrimary),
            modifier = Modifier.testTag(testTag)
        )
    }
}
