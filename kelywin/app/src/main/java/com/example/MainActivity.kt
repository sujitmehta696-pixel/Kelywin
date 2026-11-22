package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.adminapp.KelywinAdminRoot
import com.example.shared.model.GameMode
import com.example.shared.model.GameRoundState
import com.example.shared.model.RoundPeriodClock
import com.example.shared.model.TransactionCategory
import com.example.shared.ui.components.GoogleSignInGateCard
import com.example.shared.ui.components.KelywinCoralHeader
import com.example.shared.ui.components.WinLossResultDialog
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyCoralSoft
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelywinTheme
import com.example.ui.screens.BalanceScreen
import com.example.ui.screens.DepositScreen
import com.example.ui.screens.GameLowerTab
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WithdrawScreen

object KelywinRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val GAME = "game"
    const val BALANCE = "balance"
    const val DEPOSIT = "deposit"
    const val WITHDRAW = "withdraw"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
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
fun KelywinAppRoot(viewModel: KelywinViewModel = viewModel()) {
    val context = LocalContext.current
    val firebaseUser by viewModel.firebaseUser.collectAsStateWithLifecycle()
    val isGoogleAuthLoading by viewModel.isGoogleAuthLoading.collectAsStateWithLifecycle()
    val googleAuthError by viewModel.googleAuthError.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSessionInitialized by viewModel.isSessionInitialized.collectAsStateWithLifecycle()
    val rememberedCredentials by viewModel.rememberedCredentials.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeGameMode.collectAsStateWithLifecycle()
    val roundStates by viewModel.roundStates.collectAsStateWithLifecycle()
    val sec30History by viewModel.sec30History.collectAsStateWithLifecycle()
    val min1History by viewModel.min1History.collectAsStateWithLifecycle()
    val activeHistory by viewModel.activeHistory.collectAsStateWithLifecycle()
    val userBets by viewModel.userBets.collectAsStateWithLifecycle()
    val currentRoundBets by viewModel.currentRoundBets.collectAsStateWithLifecycle()
    val strategyTips by viewModel.strategyTips.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val payoutMethods by viewModel.payoutMethods.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val selectedMultiplier by viewModel.selectedMultiplier.collectAsStateWithLifecycle()
    val settlementPopup by viewModel.latestSettlementPopup.collectAsStateWithLifecycle()
    val depositReceipt by viewModel.latestDepositReceipt.collectAsStateWithLifecycle()
    val activePaymentOrder by viewModel.activePaymentOrder.collectAsStateWithLifecycle()
    val isCreatingGatewayOrder by viewModel.isCreatingGatewayOrder.collectAsStateWithLifecycle()
    val isVerifyingPayment by viewModel.isVerifyingPayment.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val recoveredPasswordInfo by viewModel.recoveredPasswordInfo.collectAsStateWithLifecycle()
    val recoveryError by viewModel.recoveryError.collectAsStateWithLifecycle()
    val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (!isSessionInitialized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KelyBackground)
        )
        return
    }

    // Mobile-first container (~430dp max width on tablets/foldables)
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFECEFF4))
    ) {
        if (firebaseUser == null) {
            Box(
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxSize()
            ) {
                GoogleSignInGateCard(
                    title = "KELYWIN",
                    subtitle = "24x7 Prediction Arena · Dynamic UPI Gateway",
                    isAdminTheme = false,
                    isLoading = isGoogleAuthLoading,
                    errorMessage = googleAuthError,
                    onSignInClick = { viewModel.signInWithGoogle(context) }
                )
            }
            return
        }

        if (appConfig.maintenanceMode) {
            Box(
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxSize()
            ) {
                MaintenanceModeScreen(
                    message = appConfig.maintenanceMessage,
                    onSignOutGoogle = { viewModel.signOutGoogle(context) {} }
                )
            }
            return
        }

        key(firebaseUser?.uid) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            var gameInitialLowerTab by remember { mutableStateOf(GameLowerTab.HISTORY) }

            val bottomItems = remember {
                listOf(
                    BottomNavItem(KelywinRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
                    BottomNavItem(KelywinRoutes.GAME, "Kelywin", Icons.Filled.Casino, Icons.Outlined.Casino),
                    BottomNavItem(KelywinRoutes.BALANCE, "Balance", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
                    BottomNavItem(KelywinRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
                )
            }

            val showBottomBar = currentRoute in bottomItems.map { it.route }

            val startDestination = remember(firebaseUser?.uid) {
                if (currentUser != null) KelywinRoutes.HOME else KelywinRoutes.LOGIN
            }

            // If user session loads from DataStore/Room after initial frame, navigate cleanly into Home
            LaunchedEffect(currentUser?.userId, currentRoute) {
                if (currentUser != null && (currentRoute == KelywinRoutes.LOGIN || currentRoute == KelywinRoutes.REGISTER)) {
                    navController.navigate(KelywinRoutes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            }

            Scaffold(
                modifier = Modifier
                    .widthIn(max = 430.dp)
                    .fillMaxSize(),
                containerColor = KelyBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 6.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            bottomItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (item.route == KelywinRoutes.GAME) {
                                            gameInitialLowerTab = GameLowerTab.HISTORY
                                        }
                                        navController.navigate(item.route) {
                                            popUpTo(KelywinRoutes.HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = KelyCoralPrimary,
                                        selectedTextColor = KelyCoralPrimary,
                                        indicatorColor = KelyCoralSoft,
                                        unselectedIconColor = KelyInkSecondary,
                                        unselectedTextColor = KelyInkSecondary
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${item.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(KelywinRoutes.LOGIN) {
                        LoginScreen(
                            rememberedCredentials = rememberedCredentials,
                            authError = authError,
                            recoveredPasswordInfo = recoveredPasswordInfo,
                            recoveryError = recoveryError,
                            onClearError = viewModel::clearAuthError,
                            onLoginPhone = { code, phone, pass, remember ->
                                viewModel.loginPhoneNoOtp(code, phone, pass, remember) {
                                    navController.navigate(KelywinRoutes.HOME) {
                                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                    }
                                }
                            },
                            onLoginEmail = { email, pass, remember ->
                                viewModel.loginEmailNoOtp(email, pass, remember) {
                                    navController.navigate(KelywinRoutes.HOME) {
                                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                    }
                                }
                            },
                            onNavigateRegister = {
                                navController.navigate(KelywinRoutes.REGISTER)
                            },
                            onRecoverWithSecretCode = viewModel::recoverPasswordWithSecretCode,
                            onClearRecovery = viewModel::clearRecoveredPassword,
                            onCopyRecoveredPassword = {
                                viewModel.notifyMessage("Password copied to clipboard.")
                            },
                            onSubmitForgotPasswordAdminRequest = viewModel::submitForgotPasswordAdminRequest
                        )
                    }

                    composable(KelywinRoutes.REGISTER) {
                        RegisterScreen(
                            enableRegistration = appConfig.enableRegistration,
                            authError = authError,
                            onClearError = viewModel::clearAuthError,
                            onRegister = { code, phone, pass, confirm, invite, accepted ->
                                viewModel.registerNoOtp(code, phone, pass, confirm, invite, accepted) {
                                    navController.navigate(KelywinRoutes.HOME) {
                                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                    }
                                }
                            },
                            onNavigateLogin = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(KelywinRoutes.HOME) {
                        HomeScreen(
                            user = currentUser,
                            announcementText = appConfig.announcementText,
                            firstDepositBonusAmount = appConfig.firstDepositBonusAmount,
                            roundStates = roundStates,
                            sec30History = sec30History,
                            min1History = min1History,
                            onLaunchGame = { mode ->
                                viewModel.setGameMode(mode)
                                gameInitialLowerTab = GameLowerTab.HISTORY
                                navController.navigate(KelywinRoutes.GAME) {
                                    popUpTo(KelywinRoutes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateBalance = {
                                navController.navigate(KelywinRoutes.BALANCE) {
                                    popUpTo(KelywinRoutes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateDeposit = { navController.navigate(KelywinRoutes.DEPOSIT) },
                            onNavigateWithdraw = { navController.navigate(KelywinRoutes.WITHDRAW) },
                            onNavigateProfile = {
                                navController.navigate(KelywinRoutes.PROFILE) {
                                    popUpTo(KelywinRoutes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(KelywinRoutes.GAME) {
                        val activeRound = roundStates[activeMode]
                            ?: RoundPeriodClock.computeRoundState(activeMode, System.currentTimeMillis())
                        GameScreen(
                            user = currentUser,
                            announcementText = appConfig.announcementText,
                            multipliers = appConfig.multipliers,
                            enableNumberBetting = appConfig.enableNumberBetting,
                            minBetAmount = appConfig.minBetAmount,
                            activeMode = activeMode,
                            roundState = activeRound,
                            history = activeHistory,
                            currentRoundBets = currentRoundBets,
                            allUserBets = userBets,
                            strategyTips = strategyTips,
                            selectedMultiplier = selectedMultiplier,
                            userSettings = userSettings,
                            initialLowerTab = gameInitialLowerTab,
                            onSelectMode = viewModel::setGameMode,
                            onSelectMultiplier = viewModel::setMultiplier,
                            onPlaceBet = viewModel::placeBet,
                            onToggleSound = {
                                viewModel.updateSettings(
                                    userSettings.copy(soundEffectsEnabled = !userSettings.soundEffectsEnabled)
                                )
                            },
                            onBackClick = {
                                navController.navigate(KelywinRoutes.HOME) {
                                    popUpTo(KelywinRoutes.HOME) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(KelywinRoutes.BALANCE) {
                        BalanceScreen(
                            user = currentUser,
                            transactions = transactions,
                            onNavigateDeposit = { navController.navigate(KelywinRoutes.DEPOSIT) },
                            onNavigateWithdraw = { navController.navigate(KelywinRoutes.WITHDRAW) }
                        )
                    }

                    composable(KelywinRoutes.DEPOSIT) {
                        DepositScreen(
                            user = currentUser,
                            upiQrConfig = appConfig.upiQrConfig,
                            firstDepositBonusAmount = appConfig.firstDepositBonusAmount,
                            enableDeposits = appConfig.enableDeposits,
                            activeOrder = activePaymentOrder,
                            isCreatingOrder = isCreatingGatewayOrder,
                            isVerifyingPayment = isVerifyingPayment,
                            recentDeposits = transactions.filter { it.category == TransactionCategory.DEPOSIT },
                            latestReceipt = depositReceipt,
                            onGenerateDynamicQr = viewModel::generateDynamicGatewayQr,
                            onCheckFamGatewayStatus = viewModel::checkFamGatewayPaymentNow,
                            onClearDynamicOrder = viewModel::clearDynamicGatewayOrder,
                            onSubmitUpiDeposit = viewModel::submitUpiQrDeposit,
                            onCopyUpiId = { upi -> viewModel.notifyMessage("UPI ID $upi copied.") },
                            onNotifyMessage = viewModel::notifyMessage,
                            onDismissReceipt = viewModel::dismissDepositReceipt,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable(KelywinRoutes.WITHDRAW) {
                        WithdrawScreen(
                            user = currentUser,
                            payoutMethods = payoutMethods,
                            withdrawalHistory = transactions.filter { it.category == TransactionCategory.WITHDRAWAL },
                            enableWithdrawals = appConfig.enableWithdrawals,
                            onSavePayoutMethod = viewModel::savePayoutMethodNoOtp,
                            onSelectDefaultMethod = viewModel::selectDefaultPayoutMethod,
                            onDeleteMethod = viewModel::deletePayoutMethod,
                            onSubmitWithdrawal = viewModel::submitWithdrawal,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable(KelywinRoutes.PROFILE) {
                        ProfileScreen(
                            user = currentUser,
                            totalBetsCount = userBets.size,
                            onCopyInviteCode = { code ->
                                viewModel.notifyMessage("Invite code $code copied.")
                            },
                            onCopySecretCode = { code ->
                                viewModel.notifyMessage("Secret Recovery Code $code copied. Keep it safe!")
                            },
                            onNavigateBalance = {
                                navController.navigate(KelywinRoutes.BALANCE) {
                                    popUpTo(KelywinRoutes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateDeposit = { navController.navigate(KelywinRoutes.DEPOSIT) },
                            onNavigateWithdraw = { navController.navigate(KelywinRoutes.WITHDRAW) },
                            onNavigateMyBets = {
                                gameInitialLowerTab = GameLowerTab.MY_BETS
                                navController.navigate(KelywinRoutes.GAME) {
                                    popUpTo(KelywinRoutes.HOME) { saveState = true }
                                    launchSingleTop = true
                                }
                            },
                            onNavigateSettings = { navController.navigate(KelywinRoutes.SETTINGS) }
                        )
                    }

                    composable(KelywinRoutes.SETTINGS) {
                        SettingsScreen(
                            user = currentUser,
                            googleEmail = firebaseUser?.email,
                            settings = userSettings,
                            onCopySecretCode = { code ->
                                viewModel.notifyMessage("Secret Recovery Code $code copied.")
                            },
                            onChangePasswordNoOtp = viewModel::changePasswordNoOtp,
                            onSaveEmail = viewModel::linkEmail,
                            onUpdateSettings = viewModel::updateSettings,
                            onLogout = {
                                viewModel.logout {
                                    navController.navigate(KelywinRoutes.LOGIN) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            onSwitchGoogleAccount = {
                                viewModel.signOutGoogle(context) {}
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }

            settlementPopup?.let { result ->
                WinLossResultDialog(
                    result = result,
                    onDismiss = viewModel::dismissSettlementPopup
                )
            }
        }
    }
}

@Composable
private fun MaintenanceModeScreen(
    message: String,
    onSignOutGoogle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
    ) {
        KelywinCoralHeader(showBackButton = false)
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        color = KelyCoralSoft,
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Maintenance",
                                tint = KelyCoralPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Text(
                        text = "Scheduled Maintenance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = KelyInkPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = message.ifBlank { "Kelywin servers are undergoing scheduled maintenance. Please check back shortly." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = KelyInkSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onSignOutGoogle,
                        colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Switch Google Account", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
