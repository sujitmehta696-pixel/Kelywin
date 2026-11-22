package com.example.adminapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.adminapp.data.AdminAuthState
import com.example.adminapp.ui.AdminDashboardScreen
import com.example.adminapp.ui.AdminGameControlScreen
import com.example.adminapp.ui.AdminGatewayAndDepositsScreen
import com.example.adminapp.ui.AdminManageHubScreen
import com.example.adminapp.ui.AdminWithdrawalsScreen
import com.example.shared.model.GameMode
import com.example.shared.model.RoomControlConfig
import com.example.shared.ui.components.GoogleSignInGateCard
import com.example.shared.ui.components.KelywinBrandMark
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyBorder
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyError
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelywinTheme
import com.example.shared.ui.theme.VipDarkBottom
import com.example.shared.ui.theme.VipDarkTop
import com.example.shared.ui.theme.VipGold

class AdminMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KelywinTheme {
                KelywinAdminRoot()
            }
        }
    }
}

@Composable
fun KelywinAdminRoot(viewModel: AdminViewModel = viewModel()) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val signInLoading by viewModel.googleSignInLoading.collectAsStateWithLifecycle()
    val signInError by viewModel.googleSignInError.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val exportedJson by viewModel.exportedJsonDialog.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.attemptSilentSignIn(context)
    }

    LaunchedEffect(feedback) {
        val msg = feedback
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    when (val state = authState) {
        AdminAuthState.Loading -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(VipDarkTop)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    KelywinBrandMark(size = 64.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    CircularProgressIndicator(color = VipGold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Verifying Admin Credentials...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        AdminAuthState.SignedOut -> {
            GoogleSignInGateCard(
                title = "KELYWIN ADMIN APK",
                subtitle = "Standalone Command Console · Connected to Shared Cloud Backend",
                isAdminTheme = true,
                isLoading = signInLoading,
                errorMessage = signInError,
                buttonLabel = "Sign in with Admin Google",
                onSignInClick = { viewModel.signInWithGoogle(context) }
            )
        }

        is AdminAuthState.Unauthorized -> {
            UnauthorizedAdminScreen(
                email = state.email,
                uid = state.uid,
                onCopyUid = {
                    copyToClipboard(context, "Admin UID", state.uid)
                    Toast.makeText(context, "UID copied", Toast.LENGTH_SHORT).show()
                },
                onSwitchAccount = { viewModel.signOut(context) }
            )
        }

        is AdminAuthState.Authorized -> {
            AuthorizedAdminConsole(
                adminUid = state.uid,
                adminEmail = state.email,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onSignOut = { viewModel.signOut(context) }
            )
        }
    }

    exportedJson?.let { json ->
        AlertDialog(
            onDismissRequest = viewModel::dismissExportDialog,
            title = { Text("Cloud Snapshot JSON", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = json,
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = KelyInkPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        copyToClipboard(context, "Kelywin Snapshot", json)
                        Toast.makeText(context, "Snapshot JSON copied!", Toast.LENGTH_SHORT).show()
                        viewModel.dismissExportDialog()
                    }
                ) {
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExportDialog) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun AuthorizedAdminConsole(
    adminUid: String,
    adminEmail: String,
    viewModel: AdminViewModel,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val selectedGameMode by viewModel.selectedGameMode.collectAsStateWithLifecycle()
    val roundStates by viewModel.roundStates.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
    val roomControls by viewModel.roomControls.collectAsStateWithLifecycle()
    val livePool by viewModel.liveRoundPool.collectAsStateWithLifecycle()
    val candidateScores by viewModel.candidateScores.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val deposits by viewModel.deposits.collectAsStateWithLifecycle()
    val withdrawals by viewModel.withdrawals.collectAsStateWithLifecycle()
    val passwordResets by viewModel.passwordResets.collectAsStateWithLifecycle()
    val roundLogs by viewModel.roundLogs.collectAsStateWithLifecycle()
    val adminMembers by viewModel.adminMembers.collectAsStateWithLifecycle()
    val famGatewaySecret by viewModel.famGatewaySecret.collectAsStateWithLifecycle()

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = KelyBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(VipDarkTop, VipDarkBottom)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KelywinBrandMark(size = 32.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "KELYWIN ADMIN",
                                color = VipGold,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = adminEmail,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = VipGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, VipGold.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    if (context.packageName == "com.aistudio.kelywin.rxmpvq") {
                                        Toast.makeText(
                                            context,
                                            "Previewing Admin in emulator. Ask to switch back to User App preview anytime.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.aistudio.kelywin.rxmpvq")
                                        if (launchIntent != null) {
                                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(launchIntent)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Install the standalone Kelywin User APK to launch it directly.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                .testTag("launch_user_apk_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Launch User App",
                                    tint = VipGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("User APK", color = VipGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("admin_signout_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sign Out",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple("Overview", Icons.Default.Dashboard, 0),
                    Triple("4-Mode", Icons.Default.AutoAwesome, 1),
                    Triple("UPI QR", Icons.Default.QrCode2, 2),
                    Triple("Payouts", Icons.Default.Payments, 3),
                    Triple("Manage", Icons.Default.AdminPanelSettings, 4)
                )
                navItems.forEach { (label, icon, idx) ->
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VipDarkTop,
                            selectedTextColor = VipDarkTop,
                            indicatorColor = VipGold.copy(alpha = 0.25f),
                            unselectedIconColor = KelyInkSecondary,
                            unselectedTextColor = KelyInkSecondary
                        ),
                        modifier = Modifier.testTag("admin_nav_tab_$idx")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> AdminDashboardScreen(
                    stats = stats,
                    appConfig = appConfig,
                    roundStates = roundStates,
                    roomControls = roomControls,
                    recentLogs = roundLogs,
                    onNavigateTab = { selectedTab = it },
                    onSelectGameRoom = { viewModel.selectGameMode(it) }
                )
                1 -> {
                    val rc = roomControls[selectedGameMode.id] ?: RoomControlConfig(modeId = selectedGameMode.id)
                    AdminGameControlScreen(
                        selectedMode = selectedGameMode,
                        roundState = roundStates[selectedGameMode],
                        roomControl = rc,
                        livePool = livePool,
                        candidateScores = candidateScores,
                        roundLogs = roundLogs,
                        onSelectMode = viewModel::selectGameMode,
                        onSetControlMode = { viewModel.setControlMode(selectedGameMode, it) },
                        onSetManualNumber = { num, nextPeriod -> viewModel.setManualOutcome(selectedGameMode, num, nextPeriod) },
                        onSetFallbackMode = { viewModel.setFallbackMode(selectedGameMode, it) },
                        onSetXinPattern = { viewModel.setXinPattern(selectedGameMode, it) },
                        onResetXinCycleNow = { pat -> viewModel.resetXinCycleToNow(selectedGameMode, pat) }
                    )
                }
                2 -> AdminGatewayAndDepositsScreen(
                    upiConfig = appConfig.upiQrConfig,
                    famGatewaySecret = famGatewaySecret,
                    firstDepositBonusAmount = appConfig.firstDepositBonusAmount,
                    deposits = deposits,
                    onSaveFamGatewayConfig = viewModel::saveFamGatewayConfig,
                    onSaveGatewayConfig = viewModel::saveGatewayConfig,
                    onApproveDeposit = { viewModel.approveDeposit(it) },
                    onRejectDeposit = { viewModel.rejectDeposit(it) }
                )
                3 -> AdminWithdrawalsScreen(
                    withdrawals = withdrawals,
                    onApproveWithdrawal = { viewModel.approveWithdrawal(it) },
                    onRejectWithdrawal = { viewModel.rejectWithdrawal(it) }
                )
                else -> AdminManageHubScreen(
                    users = users,
                    passwordResets = passwordResets,
                    appConfig = appConfig,
                    adminMembers = adminMembers,
                    currentAdminUid = adminUid,
                    onAdjustBalance = viewModel::adjustUserBalance,
                    onToggleBlocked = viewModel::toggleUserBlocked,
                    onResolvePasswordReset = viewModel::resolvePasswordReset,
                    onSaveRemoteAppControls = viewModel::saveRemoteAppControls,
                    onGrantAdmin = viewModel::grantAdminAccess,
                    onRevokeAdmin = viewModel::revokeAdminAccess,
                    onExportSnapshot = viewModel::exportSnapshot
                )
            }
        }
    }
}

@Composable
private fun UnauthorizedAdminScreen(
    email: String,
    uid: String,
    onCopyUid: () -> Unit,
    onSwitchAccount: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, KelyBorder),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockPerson,
                    contentDescription = "Access Denied",
                    tint = KelyError,
                    modifier = Modifier.size(52.dp)
                )
                Text(
                    text = "Admin Access Denied",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = KelyInkPrimary
                )
                Text(
                    text = "$email is not authorized in Firestore /admins allowlist. Ask the primary owner to grant access to your UID or switch to an authorized Admin Google account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KelyInkSecondary,
                    textAlign = TextAlign.Center
                )
                Surface(
                    color = KelyBackground,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCopyUid)
                ) {
                    Text(
                        text = "UID (tap to copy): $uid",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = KelyInkPrimary,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = onSwitchAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Sign Out & Switch Account", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}
