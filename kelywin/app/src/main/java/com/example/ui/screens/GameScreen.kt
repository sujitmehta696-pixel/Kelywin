package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.shared.model.BetColor
import com.example.shared.model.BetSelectionType
import com.example.shared.model.DragonTigerSide
import com.example.shared.model.GameMode
import com.example.shared.model.GameRoundState
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.PeriodHistoryItem
import com.example.shared.model.StrategyTip
import com.example.shared.model.UserAccount
import com.example.shared.model.UserBet
import com.example.shared.model.UserSettings
import com.example.shared.ui.components.AnnouncementBar
import com.example.shared.ui.components.BetConfirmationSheet
import com.example.shared.ui.components.HowToPlayDialog
import com.example.shared.ui.components.KelywinCoralHeader
import com.example.shared.ui.components.NumberBall
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyCoralDark
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyCoralSoft
import com.example.shared.ui.theme.KelyDragon
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyHeaderGradientBottom
import com.example.shared.ui.theme.KelyHeaderGradientTop
import com.example.shared.ui.theme.KelyInkMuted
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelyRed
import com.example.shared.ui.theme.KelySurfaceMuted
import com.example.shared.ui.theme.KelyTiger
import com.example.shared.ui.theme.KelyViolet
import com.example.ui.components.FollowStrategySection
import com.example.ui.components.GameChartSection
import com.example.ui.components.GameHistorySection
import com.example.ui.components.MyBetsSection
import java.util.Locale

enum class GameLowerTab(val label: String) {
    HISTORY("Game history"),
    CHART("Chart"),
    STRATEGY("Follow Strategy"),
    MY_BETS("My Bets")
}

@Composable
fun GameScreen(
    user: UserAccount?,
    announcementText: String,
    multipliers: PayoutMultipliers,
    enableNumberBetting: Boolean = true,
    minBetAmount: Double = 1.0,
    activeMode: GameMode,
    roundState: GameRoundState,
    history: List<PeriodHistoryItem>,
    currentRoundBets: List<UserBet>,
    allUserBets: List<UserBet>,
    strategyTips: List<StrategyTip>,
    selectedMultiplier: Int,
    userSettings: UserSettings,
    initialLowerTab: GameLowerTab = GameLowerTab.HISTORY,
    onSelectMode: (GameMode) -> Unit,
    onSelectMultiplier: (Int) -> Unit,
    onPlaceBet: (BetSelectionType, String, Double, Int, () -> Unit) -> Unit,
    onToggleSound: () -> Unit,
    onBackClick: () -> Unit
) {
    var lowerTab by remember(initialLowerTab) { mutableStateOf(initialLowerTab) }
    var pendingSelection by remember { mutableStateOf<Pair<BetSelectionType, String>?>(null) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
    ) {
        KelywinCoralHeader(
            showBackButton = true,
            onBackClick = onBackClick,
            rightContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showRulesDialog = true },
                        modifier = Modifier.testTag("game_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "How to play",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier.testTag("game_sound_toggle")
                    ) {
                        Icon(
                            imageVector = if (userSettings.soundEffectsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle sound",
                            tint = Color.White
                        )
                    }
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                AnnouncementBar(
                    message = announcementText,
                    onDetailClick = { showAnnouncementDialog = true }
                )
            }

            // Room Switcher Tabs: 30sec & 1Min
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(6.dp)
                    ) {
                        GameMode.entries.forEach { mode ->
                            val selected = mode == activeMode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) {
                                            Brush.verticalGradient(listOf(KelyHeaderGradientTop, KelyHeaderGradientBottom))
                                        } else {
                                            Brush.verticalGradient(listOf(Color.White, Color.White))
                                        }
                                    )
                                    .clickable { onSelectMode(mode) }
                                    .padding(vertical = 12.dp)
                                    .testTag("room_tab_${mode.id}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        color = if (selected) Color.White else KelySurfaceMuted,
                                        shape = CircleShape,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = if (selected) KelyCoralPrimary else KelyInkMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = mode.title,
                                        color = if (selected) Color.White else KelyInkSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ticket-Style Coral Countdown & Period Card (Recent 0-9 Balls)
            item {
                TicketTimerCard(
                    mode = activeMode,
                    roundState = roundState,
                    recentOutcomes = history.take(5),
                    onHowToPlayClick = { showRulesDialog = true }
                )
            }

            // Main Betting Board Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "Balance: ₹${String.format(Locale.US, "%.2f", user?.balance ?: 0.0)}",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = KelyInkPrimary
                            )
                            if (roundState.isLocked) {
                                Surface(color = KelyCoralSoft, shape = RoundedCornerShape(50)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = KelyCoralDark, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Locked · Settling",
                                            color = KelyCoralDark,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Open for bets",
                                    color = KelyGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Green / Violet / Red buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ColorBetButton(
                                label = stringResource(R.string.color_green),
                                subLabel = "${multipliers.greenRedMultiplier}x",
                                color = KelyGreen,
                                onClick = { pendingSelection = BetSelectionType.COLOR to BetColor.GREEN.label },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bet_color_green")
                            )
                            ColorBetButton(
                                label = stringResource(R.string.color_violet),
                                subLabel = "${multipliers.violetMultiplier}x",
                                color = KelyViolet,
                                onClick = { pendingSelection = BetSelectionType.COLOR to BetColor.VIOLET.label },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bet_color_violet")
                            )
                            ColorBetButton(
                                label = stringResource(R.string.color_red),
                                subLabel = "${multipliers.greenRedMultiplier}x",
                                color = KelyRed,
                                onClick = { pendingSelection = BetSelectionType.COLOR to BetColor.RED.label },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("bet_color_red")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 0-9 Number Balls Betting Grid (2 rows of 5) — remotely toggleable by Admin
                        if (enableNumberBetting) {
                            val ballSize = if (userSettings.compactGridMode) 42.dp else 50.dp
                            Surface(
                                color = KelySurfaceMuted,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        (0..4).forEach { num ->
                                            NumberBall(
                                                number = num,
                                                size = ballSize,
                                                compact = userSettings.compactGridMode,
                                                onClick = {
                                                    pendingSelection = BetSelectionType.NUMBER to num.toString()
                                                },
                                                modifier = Modifier.testTag("bet_number_$num")
                                            )
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        (5..9).forEach { num ->
                                            NumberBall(
                                                number = num,
                                                size = ballSize,
                                                compact = userSettings.compactGridMode,
                                                onClick = {
                                                    pendingSelection = BetSelectionType.NUMBER to num.toString()
                                                },
                                                modifier = Modifier.testTag("bet_number_$num")
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = KelySurfaceMuted,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "0–9 number betting is currently paused by Admin. Color & Dragon/Tiger rooms remain active.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KelyInkSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Multiplier Row: Random, X1, X5, X10, X20, X50
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, KelyCoralPrimary),
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val options = buildList {
                                            if (enableNumberBetting) {
                                                (0..9).forEach { add(BetSelectionType.NUMBER to it.toString()) }
                                            }
                                            add(BetSelectionType.COLOR to BetColor.GREEN.label)
                                            add(BetSelectionType.COLOR to BetColor.RED.label)
                                            add(BetSelectionType.SIDE to DragonTigerSide.DRAGON.label)
                                            add(BetSelectionType.SIDE to DragonTigerSide.TIGER.label)
                                        }
                                        pendingSelection = options.random()
                                    }
                                    .testTag("multiplier_random_button")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.multiplier_random),
                                        color = KelyCoralPrimary,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            listOf(1, 5, 10, 20, 50).forEach { mult ->
                                val selected = selectedMultiplier == mult
                                Surface(
                                    color = if (selected) KelyGreen else KelySurfaceMuted,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSelectMultiplier(mult) }
                                        .testTag("multiplier_x$mult")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "X$mult",
                                            color = if (selected) Color.White else KelyInkPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dragon (Yellow + Black Text) & Tiger (Blue + Black Text)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(50))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(KelyDragon)
                                    .clickable {
                                        pendingSelection = BetSelectionType.SIDE to DragonTigerSide.DRAGON.label
                                    }
                                    .testTag("bet_side_dragon")
                            ) {
                                Text(
                                    text = stringResource(R.string.side_dragon),
                                    color = Color.Black, // Pure #000000 black text
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(KelyTiger)
                                    .clickable {
                                        pendingSelection = BetSelectionType.SIDE to DragonTigerSide.TIGER.label
                                    }
                                    .testTag("bet_side_tiger")
                            ) {
                                Text(
                                    text = stringResource(R.string.side_tiger),
                                    color = Color.Black, // Pure #000000 black text
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        if (currentRoundBets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = KelyCoralSoft,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val summary = currentRoundBets.joinToString(" · ") {
                                    val amt = if (it.totalStake % 1.0 == 0.0) it.totalStake.toInt().toString() else String.format(Locale.US, "%.2f", it.totalStake)
                                    "${it.selectionValue} (₹$amt)"
                                }
                                Text(
                                    text = "Active in Period: $summary",
                                    color = KelyCoralDark,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Lower Tab Pills: Game history, Chart, Follow Strategy, My Bets
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GameLowerTab.entries.forEach { tab ->
                        val selected = lowerTab == tab
                        Surface(
                            color = if (selected) KelyCoralPrimary else Color.White,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = if (selected) 2.dp else 1.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { lowerTab = tab }
                                .testTag("lower_tab_${tab.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(
                                    text = tab.label,
                                    color = if (selected) Color.White else KelyInkSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            item {
                when (lowerTab) {
                    GameLowerTab.HISTORY -> GameHistorySection(history = history)
                    GameLowerTab.CHART -> GameChartSection(history = history)
                    GameLowerTab.STRATEGY -> FollowStrategySection(
                        tips = strategyTips,
                        onQuickFollow = { type, value ->
                            pendingSelection = type to value
                        }
                    )
                    GameLowerTab.MY_BETS -> MyBetsSection(bets = allUserBets)
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }

    pendingSelection?.let { (type, value) ->
        BetConfirmationSheet(
            selectionType = type,
            selectionValue = value,
            initialMultiplier = selectedMultiplier,
            periodId = roundState.periodId,
            availableBalance = user?.balance ?: 0.0,
            isRoundLocked = roundState.isLocked,
            minBetAmount = minBetAmount,
            multipliers = multipliers,
            onDismiss = { pendingSelection = null },
            onConfirmBet = { baseStake, mult ->
                onPlaceBet(type, value, baseStake, mult) {
                    pendingSelection = null
                }
            }
        )
    }

    if (showRulesDialog) {
        HowToPlayDialog(multipliers = multipliers, onDismiss = { showRulesDialog = false })
    }

    if (showAnnouncementDialog) {
        AlertDialog(
            onDismissRequest = { showAnnouncementDialog = false },
            containerColor = Color.White,
            title = { Text("Kelywin Announcement", fontWeight = FontWeight.Bold) },
            text = { Text(announcementText, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = { showAnnouncementDialog = false },
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
private fun TicketTimerCard(
    mode: GameMode,
    roundState: GameRoundState,
    recentOutcomes: List<PeriodHistoryItem>,
    onHowToPlayClick: () -> Unit
) {
    val sec = roundState.remainingSeconds
    val minStr = (sec / 60).toString().padStart(2, '0')
    val secStr = (sec % 60).toString().padStart(2, '0')

    Surface(
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(KelyHeaderGradientTop, KelyHeaderGradientBottom)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onHowToPlayClick)
                            .testTag("how_to_play_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.how_to_play),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = mode.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Recent 5 winning 0-9 number balls
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        recentOutcomes.forEach { item ->
                            NumberBall(
                                number = item.number,
                                size = 28.dp,
                                compact = true
                            )
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .width(12.dp)
                        .height(92.dp)
                        .padding(horizontal = 5.dp)
                ) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.65f),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1.05f)
                ) {
                    Text(
                        text = stringResource(R.string.time_remaining),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimerDigitBox(minStr[0].toString())
                        TimerDigitBox(minStr[1].toString())
                        TimerDigitBox(":", isColon = true)
                        TimerDigitBox(secStr[0].toString())
                        TimerDigitBox(secStr[1].toString())
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = roundState.periodId,
                        color = Color.White,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.testTag("active_period_id")
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerDigitBox(char: String, isColon: Boolean = false) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .width(if (isColon) 18.dp else 26.dp)
            .height(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = char,
                color = KelyInkPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
private fun ColorBetButton(
    label: String,
    subLabel: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier.height(52.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subLabel,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
