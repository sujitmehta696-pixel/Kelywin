package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.RememberedCredentials
import com.example.services.RecoveredPasswordInfo
import com.example.shared.model.AuthTab
import com.example.shared.ui.components.KelywinCoralHeader
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyBorder
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyError
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyGreenSoft
import com.example.shared.ui.theme.KelyInkMuted
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelyRed
import com.example.shared.ui.theme.KelyRedSoft

private val CountryCodes = listOf("+91", "+1", "+44", "+971", "+65")

@Composable
fun LoginScreen(
    rememberedCredentials: RememberedCredentials,
    authError: String?,
    recoveredPasswordInfo: RecoveredPasswordInfo?,
    recoveryError: String?,
    onClearError: () -> Unit,
    onLoginPhone: (countryCode: String, phone: String, password: String, remember: Boolean) -> Unit,
    onLoginEmail: (email: String, password: String, remember: Boolean) -> Unit,
    onNavigateRegister: () -> Unit,
    onRecoverWithSecretCode: (identifier: String, secretCode: String) -> Unit,
    onClearRecovery: () -> Unit,
    onCopyRecoveredPassword: (String) -> Unit,
    onSubmitForgotPasswordAdminRequest: (identifier: String, onDone: () -> Unit) -> Unit
) {
    var activeTab by remember { mutableStateOf(AuthTab.PHONE) }
    var countryCode by remember { mutableStateOf("+91") }
    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberPassword by remember { mutableStateOf(true) }
    var showCountryMenu by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    LaunchedEffect(rememberedCredentials) {
        if (rememberedCredentials.remember && rememberedCredentials.identifier.isNotBlank()) {
            countryCode = rememberedCredentials.countryCode
            if (rememberedCredentials.identifier.contains("@")) {
                activeTab = AuthTab.EMAIL
                emailInput = rememberedCredentials.identifier
            } else {
                activeTab = AuthTab.PHONE
                phoneInput = rememberedCredentials.identifier
            }
            passwordInput = rememberedCredentials.password
            rememberPassword = rememberedCredentials.remember
        }
    }

    val isFormValid = when (activeTab) {
        AuthTab.PHONE -> phoneInput.isNotBlank() && passwordInput.isNotBlank()
        AuthTab.EMAIL -> emailInput.isNotBlank() && passwordInput.isNotBlank()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        KelywinCoralHeader(
            showBackButton = false,
            extraBottomContent = {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = stringResource(R.string.login_title),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        )

        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                AuthTabButton(
                    title = stringResource(R.string.tab_phone_number),
                    icon = Icons.Default.PhoneAndroid,
                    selected = activeTab == AuthTab.PHONE,
                    onClick = {
                        onClearError()
                        activeTab = AuthTab.PHONE
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("login_tab_phone")
                )
                AuthTabButton(
                    title = stringResource(R.string.tab_email),
                    icon = Icons.Default.Email,
                    selected = activeTab == AuthTab.EMAIL,
                    onClick = {
                        onClearError()
                        activeTab = AuthTab.EMAIL
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("login_tab_email")
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            if (activeTab == AuthTab.PHONE) {
                FieldLabel(icon = Icons.Default.PhoneAndroid, label = stringResource(R.string.label_phone_number))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .height(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showCountryMenu = true }
                                .testTag("login_country_code")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(countryCode, fontWeight = FontWeight.SemiBold, color = KelyInkPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select country code", tint = KelyInkSecondary)
                            }
                        }
                        DropdownMenu(
                            expanded = showCountryMenu,
                            onDismissRequest = { showCountryMenu = false }
                        ) {
                            CountryCodes.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(code) },
                                    onClick = {
                                        countryCode = code
                                        showCountryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    KelyTextField(
                        value = phoneInput,
                        onValueChange = {
                            onClearError()
                            phoneInput = it.filter { ch -> ch.isDigit() }
                        },
                        placeholder = stringResource(R.string.placeholder_phone_number),
                        keyboardType = KeyboardType.Phone,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("login_phone_input")
                    )
                }
            } else {
                FieldLabel(icon = Icons.Default.Email, label = stringResource(R.string.label_email))
                Spacer(modifier = Modifier.height(10.dp))
                KelyTextField(
                    value = emailInput,
                    onValueChange = {
                        onClearError()
                        emailInput = it
                    },
                    placeholder = stringResource(R.string.placeholder_email),
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            FieldLabel(icon = Icons.Default.Lock, label = stringResource(R.string.label_password))
            Spacer(modifier = Modifier.height(10.dp))
            KelyPasswordField(
                value = passwordInput,
                onValueChange = {
                    onClearError()
                    passwordInput = it
                },
                placeholder = stringResource(R.string.placeholder_password),
                visible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { rememberPassword = !rememberPassword }
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                        .testTag("remember_password_checkbox")
                ) {
                    Icon(
                        imageVector = if (rememberPassword) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Remember password",
                        tint = if (rememberPassword) KelyCoralPrimary else KelyInkMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.remember_password),
                        style = MaterialTheme.typography.bodyMedium,
                        color = KelyInkSecondary
                    )
                }

                TextButton(
                    onClick = {
                        onClearRecovery()
                        showForgotDialog = true
                    },
                    modifier = Modifier.testTag("forgot_password_button")
                ) {
                    Text(
                        text = stringResource(R.string.forgot_password),
                        color = KelyCoralPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = KelyError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError,
                        color = KelyError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (activeTab == AuthTab.PHONE) {
                        onLoginPhone(countryCode, phoneInput, passwordInput, rememberPassword)
                    } else {
                        onLoginEmail(emailInput, passwordInput, rememberPassword)
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KelyCoralPrimary,
                    disabledContainerColor = Color(0xFFCED3DD)
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_submit_button")
            ) {
                Text(
                    text = stringResource(R.string.action_login),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    onClearError()
                    onNavigateRegister()
                },
                border = BorderStroke(1.2.dp, KelyCoralPrimary),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("go_to_register_button")
            ) {
                Text(
                    text = stringResource(R.string.action_register),
                    color = KelyCoralPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showForgotDialog) {
        ForgotPasswordSecretCodeDialog(
            initialIdentifier = if (activeTab == AuthTab.PHONE) phoneInput else emailInput,
            recoveredInfo = recoveredPasswordInfo,
            recoveryError = recoveryError,
            onDismiss = {
                onClearRecovery()
                showForgotDialog = false
            },
            onVerifySecretCode = onRecoverWithSecretCode,
            onCopyPassword = onCopyRecoveredPassword,
            onLoginWithRecovered = { info ->
                showForgotDialog = false
                onClearRecovery()
                if (info.isEmail) {
                    activeTab = AuthTab.EMAIL
                    emailInput = info.loginIdentifier
                    passwordInput = info.recoveredPassword
                    onLoginEmail(info.loginIdentifier, info.recoveredPassword, true)
                } else {
                    activeTab = AuthTab.PHONE
                    countryCode = info.countryCode
                    phoneInput = info.loginIdentifier
                    passwordInput = info.recoveredPassword
                    onLoginPhone(info.countryCode, info.loginIdentifier, info.recoveredPassword, true)
                }
            },
            onSendToAdminQueue = { id ->
                onSubmitForgotPasswordAdminRequest(id) {
                    onClearRecovery()
                    showForgotDialog = false
                }
            }
        )
    }
}

@Composable
fun RegisterScreen(
    enableRegistration: Boolean = true,
    authError: String?,
    onClearError: () -> Unit,
    onRegister: (
        countryCode: String,
        phone: String,
        password: String,
        confirmPassword: String,
        inviteCode: String,
        acceptedPrivacy: Boolean
    ) -> Unit,
    onNavigateLogin: () -> Unit
) {
    var countryCode by remember { mutableStateOf("+91") }
    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("3782622324465") }
    var passVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var acceptedPrivacy by remember { mutableStateOf(true) }
    var showCountryMenu by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        KelywinCoralHeader(
            showBackButton = true,
            onBackClick = onNavigateLogin
        )

        Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = KelyCoralPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.register_header),
                    color = KelyCoralPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(2.5.dp)
                        .background(KelyCoralPrimary)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            if (!enableRegistration) {
                Surface(
                    color = KelyRedSoft,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) {
                    Text(
                        text = "New user registrations are temporarily paused by Admin.",
                        color = KelyRed,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            FieldLabel(icon = Icons.Default.PhoneAndroid, label = stringResource(R.string.label_phone_number))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showCountryMenu = true }
                            .testTag("register_country_code")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(countryCode, fontWeight = FontWeight.SemiBold, color = KelyInkPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select country code", tint = KelyInkSecondary)
                        }
                    }
                    DropdownMenu(
                        expanded = showCountryMenu,
                        onDismissRequest = { showCountryMenu = false }
                    ) {
                        CountryCodes.forEach { code ->
                            DropdownMenuItem(
                                text = { Text(code) },
                                onClick = {
                                    countryCode = code
                                    showCountryMenu = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                KelyTextField(
                    value = phoneInput,
                    onValueChange = {
                        onClearError()
                        phoneInput = it.filter { ch -> ch.isDigit() }
                    },
                    placeholder = stringResource(R.string.placeholder_phone_number),
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("register_phone_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FieldLabel(icon = Icons.Default.Lock, label = stringResource(R.string.label_set_password))
            Spacer(modifier = Modifier.height(8.dp))
            KelyPasswordField(
                value = passwordInput,
                onValueChange = {
                    onClearError()
                    passwordInput = it
                },
                placeholder = stringResource(R.string.placeholder_set_password),
                visible = passVisible,
                onToggleVisibility = { passVisible = !passVisible },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_password_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            FieldLabel(icon = Icons.Default.Lock, label = stringResource(R.string.label_confirm_password))
            Spacer(modifier = Modifier.height(8.dp))
            KelyPasswordField(
                value = confirmPasswordInput,
                onValueChange = {
                    onClearError()
                    confirmPasswordInput = it
                },
                placeholder = stringResource(R.string.placeholder_confirm_password),
                visible = confirmVisible,
                onToggleVisibility = { confirmVisible = !confirmVisible },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            FieldLabel(icon = Icons.Default.CardGiftcard, label = stringResource(R.string.label_invite_code))
            Spacer(modifier = Modifier.height(8.dp))
            KelyTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                placeholder = "Optional invite code",
                keyboardType = KeyboardType.Text,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_invite_code_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { acceptedPrivacy = !acceptedPrivacy },
                    modifier = Modifier.testTag("register_privacy_checkbox")
                ) {
                    Icon(
                        imageVector = if (acceptedPrivacy) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Accept Privacy Agreement",
                        tint = if (acceptedPrivacy) KelyCoralPrimary else KelyInkMuted
                    )
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = KelyInkSecondary)) {
                            append(stringResource(R.string.privacy_prefix))
                        }
                        withStyle(SpanStyle(color = KelyCoralPrimary, fontWeight = FontWeight.SemiBold)) {
                            append(stringResource(R.string.privacy_link))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { showPrivacyDialog = true }
                )
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = KelyError.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authError,
                        color = KelyError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    onRegister(
                        countryCode,
                        phoneInput,
                        passwordInput,
                        confirmPasswordInput,
                        inviteCode,
                        acceptedPrivacy
                    )
                },
                enabled = enableRegistration,
                colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("register_submit_button")
            ) {
                Text(
                    text = stringResource(R.string.action_register),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    onClearError()
                    onNavigateLogin()
                },
                border = BorderStroke(1.2.dp, KelyCoralPrimary),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("register_go_to_login_button")
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = KelyInkSecondary)) {
                            append(stringResource(R.string.have_account_prefix))
                        }
                        withStyle(SpanStyle(color = KelyCoralPrimary, fontWeight = FontWeight.Bold)) {
                            append(stringResource(R.string.have_account_login))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = Color.White,
            title = { Text("Kelywin Privacy Agreement", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your Kelywin account credentials, wallet ledger, and saved payout preferences are stored securely without requiring SMS OTP verification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KelyInkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun ForgotPasswordSecretCodeDialog(
    initialIdentifier: String,
    recoveredInfo: RecoveredPasswordInfo?,
    recoveryError: String?,
    onDismiss: () -> Unit,
    onVerifySecretCode: (identifier: String, secretCode: String) -> Unit,
    onCopyPassword: (String) -> Unit,
    onLoginWithRecovered: (RecoveredPasswordInfo) -> Unit,
    onSendToAdminQueue: (identifier: String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var identifier by remember { mutableStateOf(initialIdentifier) }
    var secretCodeInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, contentDescription = null, tint = KelyCoralPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secret Code Recovery", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter your Phone, Email, or Kelywin ID along with your fixed Secret Recovery Code from Settings to reveal your real login password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KelyInkSecondary
                )
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Phone, Email, or KW ID") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("forgot_identifier_input")
                )
                OutlinedTextField(
                    value = secretCodeInput,
                    onValueChange = { secretCodeInput = it.uppercase() },
                    label = { Text("Fixed Secret Code (KW-XXXX-YYYY)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("forgot_secret_code_input")
                )

                if (!recoveryError.isNullOrBlank()) {
                    Surface(
                        color = KelyError.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = recoveryError,
                            color = KelyError,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (recoveredInfo != null) {
                    Surface(
                        color = KelyGreenSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("recovered_password_card")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Password Recovered (${recoveredInfo.kwId})",
                                color = KelyGreen,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "Account: ${recoveredInfo.loginIdentifier}",
                                style = MaterialTheme.typography.bodySmall,
                                color = KelyInkPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = recoveredInfo.recoveredPassword,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = KelyInkPrimary,
                                    modifier = Modifier.testTag("recovered_password_text")
                                )
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(recoveredInfo.recoveredPassword))
                                        onCopyPassword(recoveredInfo.recoveredPassword)
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy password", tint = KelyGreen)
                                }
                            }
                            Button(
                                onClick = { onLoginWithRecovered(recoveredInfo) },
                                colors = ButtonDefaults.buttonColors(containerColor = KelyGreen),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth().testTag("login_with_recovered_button")
                            ) {
                                Text("Log In Now", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { onSendToAdminQueue(identifier) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Or Request Admin Reset", color = KelyCoralPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerifySecretCode(identifier, secretCodeInput) },
                colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("recover_password_submit_button")
            ) {
                Text("Reveal Password", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = KelyInkSecondary)
            }
        }
    )
}

@Composable
private fun AuthTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(top = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (selected) KelyCoralPrimary else KelyInkMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = if (selected) KelyCoralPrimary else KelyInkSecondary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(if (selected) KelyCoralPrimary else KelyBorder)
        )
    }
}

@Composable
private fun FieldLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KelyCoralPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = KelyInkPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun KelyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = KelyInkMuted, fontSize = 14.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = KelyCoralPrimary,
            unfocusedBorderColor = Color.Transparent
        ),
        modifier = modifier
    )
}

@Composable
private fun KelyPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = KelyInkMuted, fontSize = 14.sp) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = KelyInkMuted
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = KelyCoralPrimary,
            unfocusedBorderColor = Color.Transparent
        ),
        modifier = modifier
    )
}
