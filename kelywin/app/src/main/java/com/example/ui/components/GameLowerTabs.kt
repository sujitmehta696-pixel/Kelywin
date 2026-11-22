package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.BetColor
import com.example.shared.model.BetSelectionType
import com.example.shared.model.BetStatus
import com.example.shared.model.DragonTigerSide
import com.example.shared.model.PeriodHistoryItem
import com.example.shared.model.StrategyTip
import com.example.shared.model.UserBet
import com.example.shared.ui.components.NumberBall
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBorder
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyCoralSoft
import com.example.shared.ui.theme.KelyDragon
import com.example.shared.ui.theme.KelyDragonSoft
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyGreenSoft
import com.example.shared.ui.theme.KelyInkPrimary
import com.example.shared.ui.theme.KelyInkSecondary
import com.example.shared.ui.theme.KelyRed
import com.example.shared.ui.theme.KelyRedSoft
import com.example.shared.ui.theme.KelySurfaceMuted
import com.example.shared.ui.theme.KelyTiger
import com.example.shared.ui.theme.KelyViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GameHistorySection(history: List<PeriodHistoryItem>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KelyCoralPrimary)
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Period",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1.75f)
                )
                Text(
                    text = "Number",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.75f)
                )
                Text(
                    text = "Dragon / Tiger",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Color",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.75f)
                )
            }

            if (history.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Text("Waiting for first round settlement...", color = KelyInkSecondary)
                }
            } else {
                history.take(15).forEachIndexed { index, item ->
                    val sideBg = if (item.side == DragonTigerSide.DRAGON) KelyDragon else KelyTiger

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = item.periodId,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = KelyInkPrimary,
                            modifier = Modifier.weight(1.75f)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(0.75f)
                        ) {
                            NumberBall(number = item.number, size = 28.dp, compact = true)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Surface(color = sideBg, shape = RoundedCornerShape(50)) {
                                Text(
                                    text = item.side.label,
                                    color = Color.Black, // Pure #000000 black text
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(0.75f)
                        ) {
                            item.colors.forEachIndexed { cIdx, c ->
                                if (cIdx > 0) Spacer(modifier = Modifier.width(4.dp))
                                val dotColor = when (c) {
                                    BetColor.GREEN -> KelyGreen
                                    BetColor.VIOLET -> KelyViolet
                                    BetColor.RED -> KelyRed
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            }
                        }
                    }
                    if (index < history.take(15).lastIndex) {
                        HorizontalDivider(color = KelyBorder, thickness = 0.6.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun GameChartSection(history: List<PeriodHistoryItem>) {
    val chartItems = history.take(12).reversed()
    Surface(
        shape = RoundedCornerShape(16.dp),
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
                Text(
                    text = "0–9 Number & Dragon/Tiger Trend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KelyInkPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendDot("Dragon", KelyDragon)
                    LegendDot("Tiger", KelyTiger)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (chartItems.isEmpty()) {
                Text("Waiting for settled rounds to plot trend.", color = KelyInkSecondary, style = MaterialTheme.typography.bodySmall)
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val stepX = w / chartItems.size.coerceAtLeast(1)

                    drawLine(
                        color = KelyBorder,
                        start = Offset(0f, h * 0.5f),
                        end = Offset(w, h * 0.5f),
                        strokeWidth = 1.dp.toPx()
                    )

                    val points = chartItems.mapIndexed { idx, item ->
                        val cx = stepX * idx + stepX / 2f
                        val cy = h * (1f - (item.number / 9f) * 0.78f - 0.11f)
                        Offset(cx, cy)
                    }

                    for (i in 0 until points.lastIndex) {
                        drawLine(
                            color = KelyCoralPrimary.copy(alpha = 0.5f),
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    points.forEachIndexed { idx, pt ->
                        val item = chartItems[idx]
                        val sideColor = if (item.side == DragonTigerSide.DRAGON) KelyDragon else KelyTiger
                        drawCircle(color = sideColor, radius = 9.dp.toPx(), center = pt)
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    chartItems.forEach { item ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            NumberBall(number = item.number, size = 22.dp, compact = true)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = if (item.side == DragonTigerSide.DRAGON) KelyDragon else KelyTiger,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = item.side.label.take(1),
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                val dragonPct = (chartItems.count { it.side == DragonTigerSide.DRAGON } * 100) / chartItems.size.coerceAtLeast(1)
                val tigerPct = 100 - dragonPct
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Dragon (5–9): $dragonPct%", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Tiger (0–4): $tigerPct%", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = KelyInkSecondary)
    }
}

@Composable
fun FollowStrategySection(
    tips: List<StrategyTip>,
    onQuickFollow: (BetSelectionType, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tips.forEachIndexed { index, tip ->
            val isSidePick = tip.selectionType == BetSelectionType.SIDE
            val btnColor = when {
                tip.suggestedPick == DragonTigerSide.DRAGON.label -> KelyDragon
                tip.suggestedPick == DragonTigerSide.TIGER.label -> KelyTiger
                tip.suggestedPick == BetColor.GREEN.label -> KelyGreen
                tip.suggestedPick == BetColor.VIOLET.label -> KelyViolet
                else -> KelyCoralPrimary
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Surface(
                        color = KelyCoralSoft,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoGraph, contentDescription = null, tint = KelyCoralPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tip.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = KelyInkPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = KelyGreenSoft, shape = RoundedCornerShape(50)) {
                                Text(
                                    text = "${tip.confidencePercent}%",
                                    color = KelyGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tip.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = KelyInkSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onQuickFollow(tip.selectionType, tip.suggestedPick) },
                        colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("follow_strategy_btn_$index")
                    ) {
                        Text(
                            text = "Bet ${tip.suggestedPick}",
                            color = if (isSidePick) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyBetsSection(bets: List<UserBet>) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (bets.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = KelyCoralPrimary, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No bets placed yet", fontWeight = FontWeight.SemiBold, color = KelyInkPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Pick Green, Violet, Red, Numbers 0–9, Dragon, or Tiger above to place your first bet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KelyInkSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                bets.take(25).forEach { bet ->
                    val statusColor = when (bet.status) {
                        BetStatus.WON -> KelyGreen
                        BetStatus.LOST -> KelyRed
                        BetStatus.PENDING -> KelyDragon
                    }
                    val statusBg = when (bet.status) {
                        BetStatus.WON -> KelyGreenSoft
                        BetStatus.LOST -> KelyRedSoft
                        BetStatus.PENDING -> KelyDragonSoft
                    }

                    Surface(
                        color = KelySurfaceMuted,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = bet.selectionValue,
                                        fontWeight = FontWeight.Bold,
                                        color = KelyInkPrimary,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${bet.modeId} · x${bet.multiplier})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KelyInkSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Period ${bet.periodId} · ${timeFormat.format(Date(bet.createdAtMillis))}",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = KelyInkSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(color = statusBg, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = bet.status.label,
                                        color = if (bet.status == BetStatus.PENDING) Color.Black else statusColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val amountLabel = when (bet.status) {
                                    BetStatus.WON -> "+₹${String.format(Locale.US, "%.2f", bet.payout)}"
                                    BetStatus.LOST -> "-₹${String.format(Locale.US, "%.2f", bet.totalStake)}"
                                    BetStatus.PENDING -> "₹${String.format(Locale.US, "%.0f", bet.totalStake)}"
                                }
                                Text(
                                    text = amountLabel,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = statusColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
