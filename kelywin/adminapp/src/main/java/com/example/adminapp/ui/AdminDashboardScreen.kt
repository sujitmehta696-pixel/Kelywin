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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.AdminDashboardStats
import com.example.shared.model.AppControlConfig
import com.example.shared.model.GameControlMode
import com.example.shared.model.GameMode
import com.example.shared.model.GameRoundState
import com.example.shared.model.RoomControlConfig
import com.example.shared.model.RoundAdminLog
import com.example.shared.model.XinLogicResolver
import com.example.shared.ui.components.NumberBall
import com.example.shared.ui.components.OutcomeChip
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
import com.example.shared.ui.theme.VipDarkBottom
import com.example.shared.ui.theme.VipDarkTop
import com.example.shared.ui.theme.VipGold
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    stats: AdminDashboardStats,
    appConfig: AppControlConfig,
    roundStates: Map<GameMode, GameRoundState>,
    roomControls: Map<String, RoomControlConfig>,
    recentLogs: List<RoundAdminLog>,
    onNavigateTab: (Int) -> Unit,
    onSelectGameRoom: (GameMode) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
    ) {
        if (appConfig.maintenanceMode) {
            item {
                Surface(
                    color = KelyError.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, KelyError),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = KelyError)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Maintenance Mode is ACTIVE",
                                fontWeight = FontWeight.ExtraBold,
                                color = KelyError,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "User APK is currently locked. Turn off in Manage tab when ready.",
                                color = KelyInkPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // VIP Financial Summary Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(VipDarkTop, VipDarkBottom)))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "KELYWIN COMMAND DESK",
                                color = VipGold,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Net House Margin",
                                color = Color.White.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Surface(
                            color = VipGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, VipGold.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${stats.activePlayers}/${stats.totalUsers} Active Users",
                                color = VipGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = "₹${String.format(Locale.US, "%,.2f", stats.netMargin)}",
                        color = if (stats.netMargin >= 0) Color.White else KelyCoralPrimary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DashboardMetricPill(
                            label = "Total Bets Volume",
                            value = "₹${String.format(Locale.US, "%,.0f", stats.totalBetsVolume)}",
                            modifier = Modifier.weight(1f)
                        )
                        DashboardMetricPill(
                            label = "Total Payouts",
                            value = "₹${String.format(Locale.US, "%,.0f", stats.totalPayouts)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Pending Action Queues
        item {
            Text(
                text = "Live Action Queues",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = KelyInkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QueueCountCard(
                    title = "UPI Deposits",
                    count = stats.pendingDeposits,
                    icon = Icons.Default.QrCode2,
                    accent = KelyGreen,
                    onClick = { onNavigateTab(2) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dash_queue_deposits")
                )
                QueueCountCard(
                    title = "Withdrawals",
                    count = stats.pendingWithdrawals,
                    icon = Icons.Default.Payments,
                    accent = KelyCoralPrimary,
                    onClick = { onNavigateTab(3) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dash_queue_withdrawals")
                )
                QueueCountCard(
                    title = "Pass Resets",
                    count = stats.pendingPasswordResets,
                    icon = Icons.Default.Key,
                    accent = KelyViolet,
                    onClick = { onNavigateTab(4) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dash_queue_resets")
                )
            }
        }

        // Live 30sec & 1Min Room Monitor Cards
        item {
            Text(
                text = "Live 24x7 Rooms & Control Modes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = KelyInkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GameMode.entries.forEach { mode ->
                    val round = roundStates[mode]
                    val rc = roomControls[mode.id] ?: RoomControlConfig(modeId = mode.id)
                    val xinInfo = if (rc.controlMode == GameControlMode.XIN_LOGIC && round != null) {
                        XinLogicResolver.stepInfo(round.periodIndex, rc.xinAnchorPeriodIndex, rc.xinPattern)
                    } else null

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, KelyBorder),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onSelectGameRoom(mode)
                                onNavigateTab(1)
                            }
                            .testTag("dash_room_${mode.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = KelyInkPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val modeBadgeColor = when (rc.controlMode) {
                                        GameControlMode.RANDOM -> KelyGreen
                                        GameControlMode.MANUAL -> KelyCoralPrimary
                                        GameControlMode.FULL_CONTROL -> VipDarkTop
                                        GameControlMode.XIN_LOGIC -> KelyViolet
                                    }
                                    Surface(color = modeBadgeColor.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                                        Text(
                                            text = rc.controlMode.label,
                                            color = modeBadgeColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Period ${round?.periodId ?: "--"}",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 12.sp,
                                    color = KelyInkSecondary
                                )
                                if (xinInfo != null) {
                                    Text(
                                        text = xinInfo.stepLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KelyViolet
                                    )
                                } else if (rc.controlMode == GameControlMode.MANUAL) {
                                    Text(
                                        text = "Locked #${rc.manualNumber} for ${rc.manualTargetPeriodId ?: "next period"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KelyCoralPrimary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val rem = round?.remainingSeconds ?: 0
                                Text(
                                    text = String.format(Locale.US, "00:%02d", rem),
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = if (round?.isLocked == true) KelyError else KelyCoralPrimary
                                )
                                Text(
                                    text = if (round?.isLocked == true) "LOCKED" else "LIVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (round?.isLocked == true) KelyError else KelyGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Shortcuts
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { onNavigateTab(1) },
                    colors = ButtonDefaults.buttonColors(containerColor = VipDarkTop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VipGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("4-Mode Engine", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = { onNavigateTab(4) },
                    colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remote Flags", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Recent Settled Rounds
        item {
            Text(
                text = "Recent Cloud Settlements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = KelyInkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (recentLogs.isEmpty()) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, KelyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = KelyInkSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Settlements appear automatically as each 30sec / 1Min period finishes.",
                            color = KelyInkSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentLogs.take(8).forEach { log ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, KelyBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    NumberBall(number = log.winningNumber, size = 38.dp, compact = true)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${log.modeId.uppercase(Locale.US)} · ${log.periodId}",
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = KelyInkPrimary
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        OutcomeChip(color = log.winningColor, side = log.winningSide, compact = true)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = log.controlModeUsed.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KelyInkSecondary
                                    )
                                    Text(
                                        text = "${if (log.netProfit >= 0) "+" else ""}₹${String.format(Locale.US, "%.0f", log.netProfit)}",
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (log.netProfit >= 0) KelyGreen else KelyError
                                    )
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
private fun DashboardMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = Color.White,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun QueueCountCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (count > 0) accent.copy(alpha = 0.55f) else KelyBorder),
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count.toString(),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = if (count > 0) accent else KelyInkPrimary
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = KelyInkSecondary
            )
        }
    }
}
