package com.example.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.shared.model.BetColor
import com.example.shared.model.BetSelectionType
import com.example.shared.model.DragonTigerSide
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.RoundSettlementResult
import com.example.shared.ui.theme.JetBrainsMono
import com.example.shared.ui.theme.KelyBackground
import com.example.shared.ui.theme.KelyBorder
import com.example.shared.ui.theme.KelyCoralPrimary
import com.example.shared.ui.theme.KelyCoralSoft
import com.example.shared.ui.theme.KelyDragon
import com.example.shared.ui.theme.KelyError
import com.example.shared.ui.theme.KelyGold
import com.example.shared.ui.theme.KelyGreen
import com.example.shared.ui.theme.KelyHeaderGradientBottom
import com.example.shared.ui.theme.KelyHeaderGradientTop
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

@Composable
fun KelywinBrandMark(
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.16f, h * 0.14f)
                lineTo(w * 0.34f, h * 0.14f)
                lineTo(w * 0.34f, h * 0.48f)
                lineTo(w * 0.68f, h * 0.14f)
                lineTo(w * 0.88f, h * 0.14f)
                lineTo(w * 0.52f, h * 0.50f)
                lineTo(w * 0.90f, h * 0.88f)
                lineTo(w * 0.68f, h * 0.88f)
                lineTo(w * 0.34f, h * 0.54f)
                lineTo(w * 0.34f, h * 0.88f)
                lineTo(w * 0.16f, h * 0.88f)
                close()
            }
            drawPath(path = path, color = KelyCoralPrimary)
            drawCircle(
                color = KelyDragon,
                radius = w * 0.11f,
                center = Offset(w * 0.80f, h * 0.22f)
            )
        }
    }
}

@Composable
fun KelywinCoralHeader(
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    rightContent: @Composable () -> Unit = {
        Surface(
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(30.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E3A8A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("EN", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EN",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    },
    extraBottomContent: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(KelyHeaderGradientTop, KelyHeaderGradientBottom)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .testTag("header_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.Center)
            ) {
                KelywinBrandMark(size = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KELYWIN",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                rightContent()
            }
        }

        if (extraBottomContent != null) {
            Spacer(modifier = Modifier.height(6.dp))
            extraBottomContent()
        }
    }
}

/**
 * Credential Manager Google Sign-In Gate shared by User APK and Admin APK.
 */
@Composable
fun GoogleSignInGateCard(
    title: String,
    subtitle: String,
    isAdminTheme: Boolean = false,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    buttonLabel: String = "Continue with Google",
    onSignInClick: () -> Unit
) {
    val bgBrush = if (isAdminTheme) {
        Brush.verticalGradient(listOf(VipDarkTop, VipDarkBottom))
    } else {
        Brush.verticalGradient(listOf(KelyHeaderGradientTop, KelyHeaderGradientBottom))
    }
    val ctaColor = if (isAdminTheme) VipDarkTop else KelyCoralPrimary
    val ctaTextColor = if (isAdminTheme) VipGold else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KelyBackground)
            .navigationBarsPadding()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(bgBrush)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            KelywinBrandMark(size = 56.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp)
        ) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(22.dp)
                ) {
                    Surface(
                        color = if (isAdminTheme) VipDarkTop.copy(alpha = 0.08f) else KelyCoralSoft,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (isAdminTheme) VipDarkTop else KelyCoralPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isAdminTheme) "Authorized Google Account Required" else "Cloud-Synced Account Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = KelyInkPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isAdminTheme) {
                            "Sign in with an authorized Admin Google account to unlock live 4-mode game controls, gateways, and user management."
                        } else {
                            "Sign in with Google to securely sync your Kelywin wallet, secret recovery code, and 24x7 bets across devices."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = KelyInkSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = KelyError.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                color = KelyError,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onSignInClick,
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = ctaColor),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_signin_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = ctaTextColor,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = ctaTextColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buttonLabel,
                                color = ctaTextColor,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pure Color + Dragon/Tiger badge chip with black text on Dragon (yellow) and Tiger (blue).
 */
@Composable
fun OutcomeChip(
    color: BetColor,
    side: DragonTigerSide,
    compact: Boolean = false
) {
    val colorBg = when (color) {
        BetColor.GREEN -> KelyGreen
        BetColor.VIOLET -> KelyViolet
        BetColor.RED -> KelyRed
    }
    val sideBg = if (side == DragonTigerSide.DRAGON) KelyDragon else KelyTiger

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        Surface(color = colorBg, shape = RoundedCornerShape(50)) {
            Text(
                text = if (compact) color.label.take(1) else color.label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 10.sp else 12.sp,
                modifier = Modifier.padding(
                    horizontal = if (compact) 7.dp else 10.dp,
                    vertical = if (compact) 3.dp else 5.dp
                )
            )
        }
        Surface(color = sideBg, shape = RoundedCornerShape(50)) {
            Text(
                text = if (compact) side.label.take(1) else side.label,
                color = Color.Black, // Pure #000000 black text on yellow/blue
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (compact) 10.sp else 12.sp,
                modifier = Modifier.padding(
                    horizontal = if (compact) 7.dp else 10.dp,
                    vertical = if (compact) 3.dp else 5.dp
                )
            )
        }
    }
}

/**
 * Glossy 0-9 Number Ball matching WinGo split-color rules (0=Violet/Red, 5=Violet/Green, 1,3,7,9=Green, 2,4,6,8=Red).
 */
@Composable
fun NumberBall(
    number: Int,
    size: Dp = 48.dp,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clamped = number.coerceIn(0, 9)
    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }

    Box(
        contentAlignment = Alignment.Center,
        modifier = baseModifier
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val r = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            when (clamped) {
                0 -> {
                    drawArc(
                        color = KelyRed,
                        startAngle = 135f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = KelyViolet,
                        startAngle = -45f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                }
                5 -> {
                    drawArc(
                        color = KelyGreen,
                        startAngle = 135f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                    drawArc(
                        color = KelyViolet,
                        startAngle = -45f,
                        sweepAngle = 180f,
                        useCenter = true
                    )
                }
                1, 3, 7, 9 -> drawCircle(color = KelyGreen, radius = r, center = center)
                else -> drawCircle(color = KelyRed, radius = r, center = center)
            }

            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = r * 0.86f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.92f),
                radius = r * 0.64f,
                center = center
            )
        }

        val numColor = when (clamped) {
            0 -> KelyViolet
            5 -> KelyGreen
            1, 3, 7, 9 -> KelyGreen
            else -> KelyRed
        }
        Text(
            text = clamped.toString(),
            color = numColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (compact) (size.value * 0.42f).sp else (size.value * 0.44f).sp
        )
    }
}

@Composable
fun UpiQrCodeCanvas(
    upiPayload: String,
    size: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, KelyBorder),
        shadowElevation = 2.dp,
        modifier = modifier.size(size)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            val gridCount = 21
            val cellSize = this.size.minDimension / gridCount

            fun drawFinder(startCol: Int, startRow: Int) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(startCol * cellSize, startRow * cellSize),
                    size = Size(7 * cellSize, 7 * cellSize)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset((startCol + 1) * cellSize, (startRow + 1) * cellSize),
                    size = Size(5 * cellSize, 5 * cellSize)
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset((startCol + 2) * cellSize, (startRow + 2) * cellSize),
                    size = Size(3 * cellSize, 3 * cellSize)
                )
            }

            drawFinder(0, 0)
            drawFinder(gridCount - 7, 0)
            drawFinder(0, gridCount - 7)

            val seedHash = upiPayload.hashCode()
            for (r in 0 until gridCount) {
                for (c in 0 until gridCount) {
                    val inTopLeft = r < 8 && c < 8
                    val inTopRight = r < 8 && c >= gridCount - 8
                    val inBottomLeft = r >= gridCount - 8 && c < 8
                    if (inTopLeft || inTopRight || inBottomLeft) continue

                    val bit = ((r * 31 + c * 17 + seedHash) xor (r * c)) and 1
                    if (bit == 1 || (r == 6 && c % 2 == 0) || (c == 6 && r % 2 == 0)) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.94f, cellSize * 0.94f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementBar(
    message: String,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(50),
        shadowElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Announcement",
                tint = KelyCoralPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = KelyInkPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = KelyCoralPrimary,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDetailClick)
                    .testTag("announcement_detail_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Detail",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Bet Confirmation Bottom Sheet supporting flexible custom stakes starting from ₹1 up to full wallet balance.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BetConfirmationSheet(
    selectionType: BetSelectionType,
    selectionValue: String,
    initialMultiplier: Int,
    periodId: String,
    availableBalance: Double,
    isRoundLocked: Boolean,
    minBetAmount: Double = 1.0,
    multipliers: PayoutMultipliers = PayoutMultipliers(),
    onDismiss: () -> Unit,
    onConfirmBet: (baseStake: Double, multiplier: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val effectiveMin = minBetAmount.coerceAtLeast(1.0)
    var stakeInputText by remember(availableBalance, effectiveMin) {
        val defaultStake = when {
            availableBalance >= 10.0 -> "10"
            availableBalance >= effectiveMin -> effectiveMin.toInt().toString()
            else -> "1"
        }
        mutableStateOf(defaultStake)
    }
    var multiplier by remember(initialMultiplier) { mutableIntStateOf(initialMultiplier.coerceAtLeast(1)) }

    val parsedStake = stakeInputText.toDoubleOrNull() ?: 0.0
    val totalAmount = parsedStake * multiplier
    val isBelowMin = totalAmount < effectiveMin
    val isInsufficientBalance = totalAmount > availableBalance + 0.0001
    val canConfirm = !isRoundLocked && !isBelowMin && !isInsufficientBalance

    val accentColor = when (selectionType) {
        BetSelectionType.COLOR -> when (selectionValue) {
            BetColor.GREEN.label -> KelyGreen
            BetColor.VIOLET.label -> KelyViolet
            else -> KelyRed
        }
        BetSelectionType.NUMBER -> {
            val num = selectionValue.toIntOrNull() ?: 0
            when (num) {
                0, 5 -> KelyViolet
                1, 3, 7, 9 -> KelyGreen
                else -> KelyRed
            }
        }
        BetSelectionType.SIDE -> if (selectionValue == DragonTigerSide.DRAGON.label) KelyDragon else KelyTiger
    }

    val headerTextColor = if (selectionType == BetSelectionType.SIDE) Color.Black else Color.White
    val displaySelectionLabel = if (selectionType == BetSelectionType.NUMBER) "Number $selectionValue" else selectionValue

    val netFactor = 1.0 - multipliers.platformFeeRate
    val maxPotentialWin = when (selectionType) {
        BetSelectionType.COLOR -> if (selectionValue == BetColor.VIOLET.label) {
            totalAmount * netFactor * multipliers.violetMultiplier
        } else {
            totalAmount * netFactor * multipliers.greenRedMultiplier
        }
        BetSelectionType.NUMBER -> totalAmount * netFactor * multipliers.numberMultiplier
        BetSelectionType.SIDE -> totalAmount * netFactor * multipliers.dragonTigerMultiplier
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accentColor)
                    .padding(vertical = 14.dp, horizontal = 18.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Select $displaySelectionLabel",
                        color = headerTextColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Period $periodId · Bal ₹${String.format(Locale.US, "%.2f", availableBalance)}",
                        color = headerTextColor.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Custom Stake Amount (Min ₹${effectiveMin.toInt()})",
                    style = MaterialTheme.typography.titleSmall,
                    color = KelyInkPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = stakeInputText,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { it.isDigit() || it == '.' }
                        if (cleaned.count { it == '.' } <= 1) {
                            stakeInputText = cleaned
                        }
                    },
                    prefix = { Text("₹", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Enter any amount from ₹1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = KelyBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bet_custom_stake_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 10, 50, 100, 500, 1000).forEach { preset ->
                        val active = kotlin.math.abs(parsedStake - preset) < 0.01
                        val bg by animateColorAsState(if (active) accentColor else KelySurfaceMuted, label = "stake_bg")
                        val fg = if (active && selectionType == BetSelectionType.SIDE) Color.Black else if (active) Color.White else KelyInkPrimary
                        Surface(
                            color = bg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { stakeInputText = preset.toString() }
                                .testTag("stake_chip_$preset")
                        ) {
                            Text(
                                text = "₹$preset",
                                color = fg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    val maxBase = (availableBalance / multiplier.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                    Surface(
                        color = KelySurfaceMuted,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val maxStr = if (multiplier == 1) {
                                    String.format(Locale.US, "%.0f", kotlin.math.floor(availableBalance).coerceAtLeast(1.0))
                                } else {
                                    maxBase.toString()
                                }
                                stakeInputText = maxStr
                            }
                            .testTag("stake_chip_max")
                    ) {
                        Text(
                            text = "All-In",
                            color = KelyInkPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Multiplier",
                    style = MaterialTheme.typography.titleSmall,
                    color = KelyInkPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 5, 10, 20, 50).forEach { mult ->
                        val active = multiplier == mult
                        val bg by animateColorAsState(if (active) accentColor else KelySurfaceMuted, label = "mult_bg")
                        val fg = if (active && selectionType == BetSelectionType.SIDE) Color.Black else if (active) Color.White else KelyInkPrimary
                        Surface(
                            color = bg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { multiplier = mult }
                                .testTag("sheet_mult_chip_$mult")
                        ) {
                            Text(
                                text = "X$mult",
                                color = fg,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = KelySurfaceMuted,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Text("Total Stake", style = MaterialTheme.typography.bodySmall, color = KelyInkSecondary)
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", totalAmount)}",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isInsufficientBalance) KelyRed else KelyInkPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Est. Payout", style = MaterialTheme.typography.bodySmall, color = KelyInkSecondary)
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", maxPotentialWin)}",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = KelyGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, KelyBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cancel_bet_button")
                    ) {
                        Text("Cancel", color = KelyInkSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onConfirmBet(parsedStake, multiplier) },
                        enabled = canConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            disabledContainerColor = KelyBorder
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp)
                            .testTag("confirm_bet_button")
                    ) {
                        val btnTextColor = if (selectionType == BetSelectionType.SIDE && canConfirm) {
                            Color.Black
                        } else {
                            Color.White
                        }
                        Text(
                            text = when {
                                isRoundLocked -> "Round Locked"
                                isBelowMin -> "Min ₹${effectiveMin.toInt()}"
                                isInsufficientBalance -> "Low Balance"
                                else -> "Confirm ₹${String.format(Locale.US, if (totalAmount % 1.0 == 0.0) "%.0f" else "%.2f", totalAmount)}"
                            },
                            color = btnTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WinLossResultDialog(
    result: RoundSettlementResult,
    onDismiss: () -> Unit
) {
    val isWin = result.isWin
    val headerBrush = if (isWin) {
        Brush.verticalGradient(listOf(Color(0xFFFA5B5A), Color(0xFFFF8C69)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF4B5563), Color(0xFF6B7280)))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("win_loss_popup")
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBrush)
                        .padding(vertical = 22.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isWin) Icons.Default.EmojiEvents else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isWin) KelyGold else Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isWin) "Congratulations!" else "Sorry, Better Luck Next Round",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${result.mode.title} · Period ${result.periodId}",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMono
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Winning Outcome (Number, Color & Dragon/Tiger)",
                        style = MaterialTheme.typography.labelMedium,
                        color = KelyInkSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumberBall(number = result.winningNumber, size = 46.dp)
                        result.winningColors.forEach { betColor ->
                            val chipBg = when (betColor) {
                                BetColor.GREEN -> KelyGreen
                                BetColor.VIOLET -> KelyViolet
                                BetColor.RED -> KelyRed
                            }
                            Surface(color = chipBg, shape = RoundedCornerShape(50)) {
                                Text(
                                    text = betColor.label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                        val sideBg = if (result.winningSide == DragonTigerSide.DRAGON) KelyDragon else KelyTiger
                        Surface(color = sideBg, shape = RoundedCornerShape(50)) {
                            Text(
                                text = result.winningSide.label,
                                color = Color.Black,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        color = if (isWin) KelyCoralSoft else KelySurfaceMuted,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = if (isWin) "Total Payout" else "Net Loss",
                                style = MaterialTheme.typography.labelMedium,
                                color = KelyInkSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isWin) {
                                    "+₹${String.format(Locale.US, "%.2f", result.totalPayout)}"
                                } else {
                                    "-₹${String.format(Locale.US, "%.2f", result.totalStake)}"
                                },
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp,
                                color = if (isWin) KelyCoralPrimary else KelyInkPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val selections = result.settledBets.joinToString(", ") {
                                "${it.selectionValue} (₹${String.format(Locale.US, if (it.totalStake % 1.0 == 0.0) "%.0f" else "%.2f", it.totalStake)})"
                            }
                            Text(
                                text = "Your Bets: $selections",
                                style = MaterialTheme.typography.bodySmall,
                                color = KelyInkSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("continue_playing_button")
                    ) {
                        Text("Continue Playing", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HowToPlayDialog(
    multipliers: PayoutMultipliers = PayoutMultipliers(),
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = KelyCoralPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("How to Play Kelywin", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "1. Choose Kelywin 30sec or Kelywin 1Min. Rounds run 24x7 and lock during the final 5 seconds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KelyInkPrimary
                )
                Text(
                    "2. Color Prediction: Green (1,3,7,9 -> ${multipliers.greenRedMultiplier}x; 5 -> 1.5x), Red (2,4,6,8 -> ${multipliers.greenRedMultiplier}x; 0 -> 1.5x), or Violet (0,5 -> ${multipliers.violetMultiplier}x).",
                    style = MaterialTheme.typography.bodySmall,
                    color = KelyInkPrimary
                )
                Text(
                    "3. Numbers 0–9: Exact winning number pays ${multipliers.numberMultiplier}x. Bet any amount starting from ₹1!",
                    style = MaterialTheme.typography.bodySmall,
                    color = KelyInkPrimary
                )
                Text(
                    "4. Dragon & Tiger: Numbers 5–9 = Dragon, 0–4 = Tiger (${multipliers.dragonTigerMultiplier}x payout).",
                    style = MaterialTheme.typography.bodySmall,
                    color = KelyInkPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = KelyCoralPrimary),
                shape = RoundedCornerShape(50)
            ) {
                Text("Got it", color = Color.White)
            }
        }
    )
}
