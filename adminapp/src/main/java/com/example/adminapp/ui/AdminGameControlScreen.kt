package com.example.adminapp.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.shared.model.CandidateOutcomeScore
import com.example.shared.model.GameControlMode
import com.example.shared.model.GameMode
import com.example.shared.model.GameRoundState
import com.example.shared.model.GameRules
import com.example.shared.model.RoundBetPool
import com.example.shared.model.RoomControlConfig
import com.example.shared.model.RoundAdminLog
import com.example.shared.model.XinLogicPattern
import com.example.shared.model.XinLogicResolver
import com.example.shared.ui.components.NumberBall
import com.example.shared.ui.components.OutcomeChip
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyBorder
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyDragon
import com.example.shared.ui.theme.KelyError
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelyRed
import com.example.shared.ui.theme.KelySurfaceMuted
import com.example.shared.ui.theme.KelyTiger
import com.example.shared.ui.theme.KelyViolet
import com.example.shared.ui.theme.VipDarkBottom
import com.example.shared.ui.theme.VipDarkTop
import com.example.shared.ui.theme.VipGold
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminGameControlScreen(
    selectedMode: GameMode,
    roundState: GameRoundState?,
    roomControl: RoomControlConfig,
    livePool: RoundBetPool,
    candidateScores: List<CandidateOutcomeScore>,
    roundLogs: List<RoundAdminLog>,
    onSelectMode: (GameMode) -> Unit,
    onSetControlMode: (GameControlMode) -> Unit,
    onSetManualNumber: (Int, Boolean) -> Unit,
    onSetFallbackMode: (GameControlMode) -> Unit,
    onSetXinPattern: (XinLogicPattern) -> Unit,
    onResetXinCycleNow: (XinLogicPattern) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
    ) {
        // 1. Room Switcher (30sec / 1Min)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GameMode.entries.forEach { mode ->
                    val active = mode == selectedMode
                    Surface(
                        color = if (active) VipDarkTop else Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, if (active) VipGold else KelyBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectMode(mode) }
                            .testTag("admin_room_tab_${mode.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = mode.title,
                                color = if (active) VipGold else KelyInkPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }

        // 2. Live Period Ticker Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(VipDarkTop, VipDarkBottom)))
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "${selectedMode.title} · LIVE PERIOD",
                            color = VipGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = roundState?.periodId ?: "--",
                            color = Color.White,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Active Mode: ${roomControl.controlMode.label} · ${livePool.totalBetCount} bets (₹${String.format(Locale.US, "%.0f", livePool.totalPool)})",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        color = if (roundState?.isLocked == true) KelyError else Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "00:%02d", roundState?.remainingSeconds ?: 0),
                                color = Color.White,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                            Text(
                                text = if (roundState?.isLocked == true) "LOCKED" else "OPEN",
                                color = if (roundState?.isLocked == true) Color.White else VipGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // 3. 4 Control Mode Cards
        item {
            Text(
                text = "Select Game Control Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = KelyInkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ControlModeCard(
                    mode = GameControlMode.RANDOM,
                    icon = Icons.Default.Casino,
                    accent = KelyGreen,
                    selected = roomControl.controlMode == GameControlMode.RANDOM,
                    onClick = { onSetControlMode(GameControlMode.RANDOM) }
                )
                ControlModeCard(
                    mode = GameControlMode.MANUAL,
                    icon = Icons.Default.TouchApp,
                    accent = KelyCoralPrimary,
                    selected = roomControl.controlMode == GameControlMode.MANUAL,
                    onClick = { onSetControlMode(GameControlMode.MANUAL) }
                )
                ControlModeCard(
                    mode = GameControlMode.FULL_CONTROL,
                    icon = Icons.Default.Shield,
                    accent = VipDarkTop,
                    selected = roomControl.controlMode == GameControlMode.FULL_CONTROL,
                    onClick = { onSetControlMode(GameControlMode.FULL_CONTROL) }
                )
                ControlModeCard(
                    mode = GameControlMode.XIN_LOGIC,
                    icon = Icons.Default.AutoAwesome,
                    accent = KelyViolet,
                    selected = roomControl.controlMode == GameControlMode.XIN_LOGIC,
                    onClick = { onSetControlMode(GameControlMode.XIN_LOGIC) }
                )
            }
        }

        // 4A. Manual 1-Period Control Section
        if (roomControl.controlMode == GameControlMode.MANUAL) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, KelyCoralPrimary.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = KelyCoralPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "1-Period Manual Result Override",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KelyInkPrimary,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Locked Period: ${roomControl.manualTargetPeriodId ?: roundState?.periodId ?: "--"} -> #${roomControl.manualNumber} (${GameRules.primaryColorForNumber(roomControl.manualNumber).label} / ${GameRules.sideForNumber(roomControl.manualNumber).label})",
                                    fontSize = 12.sp,
                                    color = KelyCoralPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = "Tap a ball (0–9) to set outcome for this single period only:",
                            style = MaterialTheme.typography.bodySmall,
                            color = KelyInkSecondary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (0..9).forEach { num ->
                                val isPicked = roomControl.manualNumber == num
                                Surface(
                                    color = if (isPicked) KelyCoralPrimary.copy(alpha = 0.14f) else KelySurfaceMuted,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        width = if (isPicked) 2.dp else 1.dp,
                                        color = if (isPicked) KelyCoralPrimary else KelyBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSetManualNumber(num, false) }
                                        .testTag("manual_pick_ball_$num")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        NumberBall(number = num, size = 42.dp, compact = true)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = GameRules.sideForNumber(num).label.take(1),
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { onSetManualNumber(roomControl.manualNumber, true) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lock #${roomControl.manualNumber} for NEXT Period Instead", fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "Auto-Revert Fallback After Period Closes:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = KelyInkPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(GameControlMode.RANDOM, GameControlMode.FULL_CONTROL, GameControlMode.XIN_LOGIC).forEach { fb ->
                                val activeFb = roomControl.fallbackMode == fb
                                Surface(
                                    color = if (activeFb) VipDarkTop else KelySurfaceMuted,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onSetFallbackMode(fb) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = fb.label,
                                            color = if (activeFb) VipGold else KelyInkPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4B. XIN LOGIC MODE Configuration Card
        if (roomControl.controlMode == GameControlMode.XIN_LOGIC) {
            item {
                val periodIdx = roundState?.periodIndex ?: 1
                val stepInfo = XinLogicResolver.stepInfo(periodIdx, roomControl.xinAnchorPeriodIndex, roomControl.xinPattern)
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, KelyViolet.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = KelyViolet)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "XIN LOGIC 6-Period Cycle",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KelyInkPrimary,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Surface(
                                color = if (stepInfo.isHighStep) KelyGreen.copy(alpha = 0.14f) else KelyCoralPrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = stepInfo.stepLabel,
                                    color = if (stepInfo.isHighStep) KelyGreen else KelyCoralPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Visual 6-step cycle pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (stepOffset in 0 until 6) {
                                val stepMeta = XinLogicResolver.stepInfo(
                                    periodIndex = roomControl.xinAnchorPeriodIndex + stepOffset,
                                    anchorPeriodIndex = roomControl.xinAnchorPeriodIndex,
                                    pattern = roomControl.xinPattern
                                )
                                val isCurrent = stepMeta.cycleStepZeroBased == stepInfo.cycleStepZeroBased
                                val pillBg = when {
                                    isCurrent && stepMeta.isHighStep -> KelyGreen
                                    isCurrent && !stepMeta.isHighStep -> KelyCoralPrimary
                                    stepMeta.isHighStep -> KelyGreen.copy(alpha = 0.14f)
                                    else -> KelyCoralPrimary.copy(alpha = 0.14f)
                                }
                                val pillFg = if (isCurrent) Color.White else if (stepMeta.isHighStep) KelyGreen else KelyCoralPrimary
                                Surface(
                                    color = pillBg,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text("P${stepOffset + 1}", fontSize = 10.sp, color = pillFg, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (stepMeta.isHighStep) "HIGH" else "LOW",
                                            fontSize = 10.sp,
                                            color = pillFg,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Evaluates ALL combined stakes (Numbers 0–9 + Green/Violet/Red + Dragon/Tiger) to pick the true ${if (stepInfo.isHighStep) "highest" else "lowest"} total payout outcome.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KelyInkSecondary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            XinLogicPattern.entries.forEach { pat ->
                                val selectedPat = roomControl.xinPattern == pat
                                Surface(
                                    color = if (selectedPat) KelyViolet.copy(alpha = 0.12f) else KelySurfaceMuted,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (selectedPat) KelyViolet else KelyBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onSetXinPattern(pat) }
                                        .testTag("xin_pattern_${pat.name}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = pat.label,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (selectedPat) KelyViolet else KelyInkPrimary,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = pat.summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = KelyInkSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { onResetXinCycleNow(roomControl.xinPattern) },
                            colors = ButtonDefaults.buttonColors(containerColor = KelyViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("xin_reset_cycle_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Cycle to Step 1 Now", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Live Multi-User Pool Breakdown
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
                    Text(
                        text = "Live Multi-User Bet Pool (${roundState?.periodId ?: "--"})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = KelyInkPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PoolChip("Green", livePool.greenStake, KelyGreen, Color.White, Modifier.weight(1f))
                        PoolChip("Violet", livePool.violetStake, KelyViolet, Color.White, Modifier.weight(1f))
                        PoolChip("Red", livePool.redStake, KelyRed, Color.White, Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PoolChip("Dragon", livePool.dragonStake, KelyDragon, Color.Black, Modifier.weight(1f))
                        PoolChip("Tiger", livePool.tigerStake, KelyTiger, Color.Black, Modifier.weight(1f))
                    }

                    Text(
                        text = "0–9 Projected Total Payouts (Combined Numbers + Color + D/T)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = KelyInkSecondary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (0..9).forEach { n ->
                            val stakeOnNum = livePool.numberStakes[n] ?: 0.0
                            val projectedPayout = candidateScores.find { it.number == n }?.totalPayout ?: 0.0
                            Surface(
                                color = KelySurfaceMuted,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.width(62.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                ) {
                                    NumberBall(number = n, size = 30.dp, compact = true)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${stakeOnNum.toInt()}",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KelyInkPrimary
                                    )
                                    Text(
                                        text = "Out ₹${projectedPayout.toInt()}",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 9.sp,
                                        color = KelyCoralPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Room History Logs
        item {
            val roomLogs = roundLogs.filter { it.modeId == selectedMode.id }.take(10)
            Text(
                text = "${selectedMode.shortLabel} Settled Audit Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = KelyInkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                roomLogs.forEach { log ->
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
                                NumberBall(number = log.winningNumber, size = 36.dp, compact = true)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = log.periodId,
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
                                Text(log.controlModeUsed.label, fontSize = 11.sp, color = KelyInkSecondary)
                                Text(
                                    text = "In ₹${log.totalBetsAmount.toInt()} · Out ₹${log.totalPayoutAmount.toInt()}",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
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

@Composable
private fun ControlModeCard(
    mode: GameControlMode,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.08f) else Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else KelyBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("control_mode_card_${mode.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = mode.label, tint = accent)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.label,
                    fontWeight = FontWeight.ExtraBold,
                    color = KelyInkPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = mode.description,
                    color = KelyInkSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (selected) {
                Surface(color = accent, shape = RoundedCornerShape(50)) {
                    Text(
                        text = "ACTIVE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PoolChip(
    label: String,
    amount: Double,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Text(label, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            Text(
                text = "₹${String.format(Locale.US, "%.0f", amount)}",
                color = fg,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
