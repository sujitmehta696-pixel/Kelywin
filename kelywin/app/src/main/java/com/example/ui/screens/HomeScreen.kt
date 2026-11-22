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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.BetColor
import com.example.shared.model.DragonTigerSide
import com.example.shared.model.GameMode
import com.example.shared.model.GameRoundState
import com.example.shared.model.PeriodHistoryItem
import com.example.shared.model.UserAccount
import com.example.shared.ui.components.AnnouncementBar
import com.example.shared.ui.components.KelywinCoralHeader
import com.example.shared.ui.components.NumberBall
import com.example.shared.ui.components.OutcomeChip
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyCoralDark
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyDragon
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyHeaderGradientBottom
import com.example.shared.ui.theme.KelyHeaderGradientTop
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelyTiger
import com.example.shared.ui.theme.VipDarkBottom
import com.example.shared.ui.theme.VipDarkTop
import com.example.shared.ui.theme.VipGold
import java.util.Locale

@Composable
fun HomeScreen(
    user: UserAccount?,
    announcementText: String,
    firstDepositBonusAmount: Double,
    roundStates: Map<GameMode, GameRoundState>,
    sec30History: List<PeriodHistoryItem>,
    min1History: List<PeriodHistoryItem>,
    onLaunchGame: (GameMode) -> Unit,
    onNavigateBalance: () -> Unit,
    onNavigateDeposit: () -> Unit,
    onNavigateWithdraw: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    var showAnnouncementDialog by remember { mutableStateOf(false) }

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
            // Live Activity & Announcement Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(KelyGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "24x7 LIVE ARENA · NUMBERS, COLOR & DRAGON/TIGER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = KelyInkSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "ID ${user?.kwId ?: "---"}",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KelyCoralPrimary,
                            modifier = Modifier.clickable(onClick = onNavigateProfile)
                        )
                    }
                    AnnouncementBar(
                        message = announcementText,
                        onDetailClick = { showAnnouncementDialog = true }
                    )
                }
            }

            // VIP-Style Dark & Gold Balance Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, VipGold.copy(alpha = 0.6f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(VipDarkTop, VipDarkBottom)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = VipGold.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(0.8.dp, VipGold.copy(alpha = 0.55f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = VipGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "KELYWIN VIP WALLET",
                                            color = VipGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }

                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onNavigateBalance)
                                        .testTag("home_balance_shortcut")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = "Open Balance",
                                            tint = VipGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Available Balance",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", user?.balance ?: 0.0)}",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp,
                                color = Color.White
                            )

                            if (user != null && !user.hasReceivedFirstDepositBonus) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = VipGold.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CardGiftcard,
                                            contentDescription = null,
                                            tint = VipGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "First UPI QR Deposit unlocks +₹${firstDepositBonusAmount.toInt()} instant bonus!",
                                            color = VipGold,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
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
                                    colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("home_deposit_button")
                                ) {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("UPI QR Pay", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onNavigateWithdraw,
                                    colors = ButtonDefaults.buttonColors(containerColor = VipGold),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("home_withdraw_button")
                                ) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Withdraw", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Live Prediction Rooms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = KelyInkPrimary
                    )
                    Text(
                        text = "Instant Settlement",
                        style = MaterialTheme.typography.labelSmall,
                        color = KelyCoralPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Sharp Kelywin 30sec Card
            item {
                PremiumRoomLaunchCard(
                    mode = GameMode.SEC_30,
                    roundState = roundStates[GameMode.SEC_30],
                    recentItems = sec30History.take(4),
                    subtitle = "Turbo 30sec · 0–9 Numbers, Color & Dragon/Tiger",
                    badgeText = "TURBO",
                    icon = Icons.Default.Timer,
                    onClick = { onLaunchGame(GameMode.SEC_30) },
                    modifier = Modifier.testTag("home_launch_30sec")
                )
            }

            // Sharp Kelywin 1Min Card
            item {
                PremiumRoomLaunchCard(
                    mode = GameMode.MIN_1,
                    roundState = roundStates[GameMode.MIN_1],
                    recentItems = min1History.take(4),
                    subtitle = "Classic 1Min · 0–9 Numbers, Color & Dragon/Tiger",
                    badgeText = "PRO",
                    icon = Icons.Default.Schedule,
                    onClick = { onLaunchGame(GameMode.MIN_1) },
                    modifier = Modifier.testTag("home_launch_1min")
                )
            }

            // Dragon vs Tiger Highlight Card (Black text on Yellow & Blue!)
            item {
                val recentSides = sec30History.take(8)
                val dragonCount = recentSides.count { it.side == DragonTigerSide.DRAGON }
                val tigerCount = recentSides.size - dragonCount

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onLaunchGame(GameMode.SEC_30) }
                        .testTag("home_dragon_tiger_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = KelyDragon)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dragon vs Tiger Live Pulse",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KelyInkPrimary
                                )
                            }
                            Text(
                                text = "2.0x Payout",
                                style = MaterialTheme.typography.labelMedium,
                                color = KelyCoralPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = KelyDragon,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Text(
                                        text = "Dragon",
                                        color = Color.Black, // Pure #000000 black text
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$dragonCount / ${recentSides.size.coerceAtLeast(1)} recent",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Surface(
                                color = KelyTiger,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Text(
                                        text = "Tiger",
                                        color = Color.Black, // Pure #000000 black text
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$tigerCount / ${recentSides.size.coerceAtLeast(1)} recent",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showAnnouncementDialog) {
        AlertDialog(
            onDismissRequest = { showAnnouncementDialog = false },
            containerColor = Color.White,
            title = { Text("Kelywin Official Notice", fontWeight = FontWeight.Bold) },
            text = { Text(announcementText, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { showAnnouncementDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun PremiumRoomLaunchCard(
    mode: GameMode,
    roundState: GameRoundState?,
    recentItems: List<PeriodHistoryItem>,
    subtitle: String,
    badgeText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sec = roundState?.remainingSeconds ?: mode.durationSeconds
    val minPart = (sec / 60).toString().padStart(2, '0')
    val secPart = (sec % 60).toString().padStart(2, '0')

    Surface(
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 5.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(KelyHeaderGradientTop, KelyHeaderGradientBottom)
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            color = VipDarkTop.copy(alpha = 0.28f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, VipGold.copy(alpha = 0.7f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, contentDescription = null, tint = VipGold)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = mode.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(color = VipGold, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = badgeText,
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = subtitle,
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$minPart:$secPart",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.ExtraBold,
                            color = KelyCoralDark,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        recentItems.forEach { item ->
                            NumberBall(number = item.number, size = 26.dp, compact = true)
                        }
                        recentItems.firstOrNull()?.let { latest ->
                            OutcomeChip(
                                color = latest.colors.firstOrNull() ?: BetColor.GREEN,
                                side = latest.side,
                                compact = true
                            )
                        }
                    }

                    Surface(
                        color = VipDarkTop,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Play",
                                color = VipGold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Enter ${mode.title}",
                                tint = VipGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
