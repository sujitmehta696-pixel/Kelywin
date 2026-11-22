package com.example.shared.model

import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class GameMode(
    val id: String,
    val title: String,
    val shortLabel: String,
    val durationSeconds: Int,
    val lockThresholdSeconds: Int = 5
) {
    SEC_30(
        id = "30sec",
        title = "Kelywin 30sec",
        shortLabel = "30sec",
        durationSeconds = 30,
        lockThresholdSeconds = 5
    ),
    MIN_1(
        id = "1min",
        title = "Kelywin 1Min",
        shortLabel = "1Min",
        durationSeconds = 60,
        lockThresholdSeconds = 5
    )
}

enum class BetColor(val label: String) {
    GREEN("Green"),
    VIOLET("Violet"),
    RED("Red")
}

enum class DragonTigerSide(val label: String) {
    DRAGON("Dragon"),
    TIGER("Tiger")
}

enum class BetSelectionType {
    COLOR,
    NUMBER,
    SIDE
}

enum class BetStatus(val label: String) {
    PENDING("Pending"),
    WON("Won"),
    LOST("Lost")
}

enum class GameControlMode(val label: String, val description: String) {
    RANDOM("Random", "Fair randomized Number (0–9), Color & Dragon/Tiger outcomes"),
    MANUAL("Manual", "Set exact outcome for 1 live period only (resets after settlement)"),
    FULL_CONTROL("Full Control", "Auto-picks lowest payout / highest profit outcome across all bets"),
    XIN_LOGIC("XIN LOGIC", "Repeating 6-period cycle: 2 periods High payout + 4 periods Low payout across Numbers, Color & D/T")
}

enum class XinLogicPattern(val label: String, val summary: String) {
    TWO_HIGH_FOUR_LOW("2 High → 4 Low", "2 periods highest staked outcome wins, then 4 periods lowest outcome wins, then repeats"),
    TWO_LOW_FOUR_HIGH("2 Low → 4 High", "2 periods lowest staked outcome wins, then 4 periods highest outcome wins, then repeats")
}

data class XinLogicStepInfo(
    val cycleStepZeroBased: Int,
    val stepNumber: Int,
    val isHighStep: Boolean,
    val pattern: XinLogicPattern
) {
    val stepLabel: String
        get() = "Step $stepNumber/6 · ${if (isHighStep) "HIGH WINS" else "LOW WINS"}"
}

data class GameRoundState(
    val mode: GameMode,
    val periodId: String,
    val periodIndex: Int,
    val remainingSeconds: Int,
    val totalSeconds: Int,
    val isLocked: Boolean
)

data class PeriodHistoryItem(
    val periodId: String,
    val modeId: String,
    val number: Int,
    val colors: List<BetColor>,
    val side: DragonTigerSide,
    val controlModeUsed: GameControlMode = GameControlMode.RANDOM,
    val timestampMillis: Long
)

data class UserBet(
    val id: String,
    val userId: String,
    val periodId: String,
    val modeId: String,
    val selectionType: BetSelectionType,
    val selectionValue: String,
    val baseStake: Double,
    val multiplier: Int,
    val totalStake: Double,
    val status: BetStatus,
    val payout: Double,
    val createdAtMillis: Long
)

data class RoundSettlementResult(
    val mode: GameMode,
    val periodId: String,
    val winningNumber: Int,
    val winningColors: List<BetColor>,
    val winningSide: DragonTigerSide,
    val settledBets: List<UserBet>,
    val totalStake: Double,
    val totalPayout: Double
) {
    val netProfit: Double get() = totalPayout - totalStake
    val isWin: Boolean get() = totalPayout > 0.0
}

data class StrategyTip(
    val title: String,
    val suggestedPick: String,
    val selectionType: BetSelectionType,
    val confidencePercent: Int,
    val rationale: String
)

data class PayoutMultipliers(
    val greenRedMultiplier: Double = 2.0,
    val violetMultiplier: Double = 4.5,
    val numberMultiplier: Double = 9.0,
    val dragonTigerMultiplier: Double = 2.0,
    val platformFeeRate: Double = 0.02
)

object RoundPeriodClock {
    private val dateFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("yyyyMMdd", Locale.US)
    }

    fun computeRoundState(mode: GameMode, nowMillis: Long): GameRoundState {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val secondsSinceMidnight =
            cal.get(Calendar.HOUR_OF_DAY) * 3600 +
                cal.get(Calendar.MINUTE) * 60 +
                cal.get(Calendar.SECOND)

        val periodIndex = (secondsSinceMidnight / mode.durationSeconds) + 1
        val elapsedInPeriod = secondsSinceMidnight % mode.durationSeconds
        val remaining = mode.durationSeconds - elapsedInPeriod

        val datePart = dateFormatter.get()!!.format(Date(nowMillis))
        val roomCode = when (mode) {
            GameMode.SEC_30 -> "1000"
            GameMode.MIN_1 -> "2000"
        }
        val periodId = "$datePart$roomCode${periodIndex.toString().padStart(5, '0')}"

        return GameRoundState(
            mode = mode,
            periodId = periodId,
            periodIndex = periodIndex,
            remainingSeconds = remaining,
            totalSeconds = mode.durationSeconds,
            isLocked = remaining <= mode.lockThresholdSeconds
        )
    }

    fun periodIndexFromId(periodId: String): Int {
        return periodId.takeLast(5).toIntOrNull()?.coerceAtLeast(1) ?: 1
    }

    fun computeNextPeriodId(mode: GameMode, currentPeriodId: String): String {
        if (currentPeriodId.length < 10) return currentPeriodId
        val maxPerDay = 86400 / mode.durationSeconds
        val currentIdx = currentPeriodId.takeLast(5).toIntOrNull() ?: return currentPeriodId
        return if (currentIdx < maxPerDay) {
            currentPeriodId.dropLast(5) + (currentIdx + 1).toString().padStart(5, '0')
        } else {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val dateStr = dateFormatter.get()!!.format(Date(cal.timeInMillis))
            val roomCode = if (mode == GameMode.SEC_30) "1000" else "2000"
            "${dateStr}${roomCode}00001"
        }
    }
}

object GameRules {
    fun colorsForNumber(number: Int): List<BetColor> = when (number) {
        0 -> listOf(BetColor.RED, BetColor.VIOLET)
        5 -> listOf(BetColor.GREEN, BetColor.VIOLET)
        1, 3, 7, 9 -> listOf(BetColor.GREEN)
        else -> listOf(BetColor.RED)
    }

    fun primaryColorForNumber(number: Int): BetColor =
        colorsForNumber(number).firstOrNull() ?: BetColor.GREEN

    fun sideForNumber(number: Int): DragonTigerSide =
        if (number >= 5) DragonTigerSide.DRAGON else DragonTigerSide.TIGER

    fun numberForColorAndSide(color: BetColor, side: DragonTigerSide): Int = when (side) {
        DragonTigerSide.DRAGON -> when (color) {
            BetColor.GREEN -> 7
            BetColor.VIOLET -> 5
            BetColor.RED -> 6
        }
        DragonTigerSide.TIGER -> when (color) {
            BetColor.GREEN -> 1
            BetColor.VIOLET -> 0
            BetColor.RED -> 2
        }
    }

    fun calculatePayout(
        selectionType: BetSelectionType,
        selectionValue: String,
        totalStake: Double,
        winningNumber: Int,
        multipliers: PayoutMultipliers = PayoutMultipliers()
    ): Double {
        val netStake = totalStake * (1.0 - multipliers.platformFeeRate)
        val winningColors = colorsForNumber(winningNumber)
        val winningSide = sideForNumber(winningNumber)

        return when (selectionType) {
            BetSelectionType.NUMBER -> {
                val chosenNum = selectionValue.toIntOrNull()
                if (chosenNum == winningNumber) netStake * multipliers.numberMultiplier else 0.0
            }
            BetSelectionType.COLOR -> {
                val chosen = BetColor.entries.find { it.label.equals(selectionValue, ignoreCase = true) }
                when (chosen) {
                    BetColor.VIOLET -> if (BetColor.VIOLET in winningColors) netStake * multipliers.violetMultiplier else 0.0
                    BetColor.GREEN -> if (BetColor.GREEN in winningColors) {
                        val mult = if (BetColor.VIOLET in winningColors) 1.5 else multipliers.greenRedMultiplier
                        netStake * mult
                    } else 0.0
                    BetColor.RED -> if (BetColor.RED in winningColors) {
                        val mult = if (BetColor.VIOLET in winningColors) 1.5 else multipliers.greenRedMultiplier
                        netStake * mult
                    } else 0.0
                    null -> 0.0
                }
            }
            BetSelectionType.SIDE -> {
                val chosen = DragonTigerSide.entries.find { it.label.equals(selectionValue, ignoreCase = true) }
                if (chosen == winningSide) netStake * multipliers.dragonTigerMultiplier else 0.0
            }
        }
    }
}

data class CandidateOutcomeScore(
    val number: Int,
    val totalPayout: Double
)

object XinLogicResolver {
    private val defaultRandom = SecureRandom()

    fun stepInfo(
        periodIndex: Int,
        anchorPeriodIndex: Int,
        pattern: XinLogicPattern = XinLogicPattern.TWO_HIGH_FOUR_LOW
    ): XinLogicStepInfo {
        val normalized = (((periodIndex - anchorPeriodIndex) % 6) + 6) % 6
        val isHigh = when (pattern) {
            XinLogicPattern.TWO_HIGH_FOUR_LOW -> normalized < 2
            XinLogicPattern.TWO_LOW_FOUR_HIGH -> normalized >= 2
        }
        return XinLogicStepInfo(
            cycleStepZeroBased = normalized,
            stepNumber = normalized + 1,
            isHighStep = isHigh,
            pattern = pattern
        )
    }

    fun evaluateCandidateScores(
        pendingBets: List<UserBet>,
        multipliers: PayoutMultipliers = PayoutMultipliers()
    ): List<CandidateOutcomeScore> {
        return (0..9).map { candidate ->
            val payout = pendingBets.sumOf { bet ->
                GameRules.calculatePayout(
                    selectionType = bet.selectionType,
                    selectionValue = bet.selectionValue,
                    totalStake = bet.totalStake,
                    winningNumber = candidate,
                    multipliers = multipliers
                )
            }
            CandidateOutcomeScore(number = candidate, totalPayout = payout)
        }
    }

    fun resolveNumber(
        pendingBets: List<UserBet>,
        periodIndex: Int,
        anchorPeriodIndex: Int,
        pattern: XinLogicPattern = XinLogicPattern.TWO_HIGH_FOUR_LOW,
        multipliers: PayoutMultipliers = PayoutMultipliers(),
        randomIndexPicker: (Int) -> Int = { size -> defaultRandom.nextInt(size) }
    ): Int {
        if (pendingBets.isEmpty()) {
            return randomIndexPicker(10).coerceIn(0, 9)
        }

        val info = stepInfo(periodIndex, anchorPeriodIndex, pattern)
        val scores = evaluateCandidateScores(pendingBets, multipliers)

        val targetPayout = if (info.isHighStep) {
            scores.maxOf { it.totalPayout }
        } else {
            scores.minOf { it.totalPayout }
        }

        val matchingCandidates = scores
            .filter { kotlin.math.abs(it.totalPayout - targetPayout) <= 0.001 }
            .map { it.number }

        if (matchingCandidates.isEmpty()) return randomIndexPicker(10).coerceIn(0, 9)
        return matchingCandidates[randomIndexPicker(matchingCandidates.size).coerceIn(0, matchingCandidates.lastIndex)]
    }

    fun resolveLowestPayoutNumber(
        pendingBets: List<UserBet>,
        multipliers: PayoutMultipliers = PayoutMultipliers(),
        randomIndexPicker: (Int) -> Int = { size -> defaultRandom.nextInt(size) }
    ): Int {
        if (pendingBets.isEmpty()) return randomIndexPicker(10).coerceIn(0, 9)
        val scores = evaluateCandidateScores(pendingBets, multipliers)
        val minPayout = scores.minOf { it.totalPayout }
        val matching = scores.filter { kotlin.math.abs(it.totalPayout - minPayout) <= 0.001 }.map { it.number }
        if (matching.isEmpty()) return randomIndexPicker(10).coerceIn(0, 9)
        return matching[randomIndexPicker(matching.size).coerceIn(0, matching.lastIndex)]
    }
}
