package com.example

import com.example.shared.model.BetColor
import com.example.shared.model.BetSelectionType
import com.example.shared.model.DragonTigerSide
import com.example.shared.model.GameMode
import com.example.shared.model.GameRules
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.RoundPeriodClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KelywinEngineTest {

    @Test
    fun `number rules resolve exact colors and dragon tiger side`() {
        assertEquals(listOf(BetColor.RED, BetColor.VIOLET), GameRules.colorsForNumber(0))
        assertEquals(listOf(BetColor.GREEN, BetColor.VIOLET), GameRules.colorsForNumber(5))
        assertEquals(listOf(BetColor.GREEN), GameRules.colorsForNumber(7))
        assertEquals(listOf(BetColor.RED), GameRules.colorsForNumber(4))

        assertEquals(DragonTigerSide.TIGER, GameRules.sideForNumber(0))
        assertEquals(DragonTigerSide.TIGER, GameRules.sideForNumber(4))
        assertEquals(DragonTigerSide.DRAGON, GameRules.sideForNumber(5))
        assertEquals(DragonTigerSide.DRAGON, GameRules.sideForNumber(9))
    }

    @Test
    fun `payout calculations resolve accurately for 1 rupee minimum and color number dragon tiger outcomes`() {
        // Flexible ₹1 minimum bet payout verification
        val oneRupeeGreenWin = GameRules.calculatePayout(
            selectionType = BetSelectionType.COLOR,
            selectionValue = "Green",
            totalStake = 1.0,
            winningNumber = 7
        )
        assertEquals(1.96, oneRupeeGreenWin, 0.01)

        val greenWin = GameRules.calculatePayout(
            selectionType = BetSelectionType.COLOR,
            selectionValue = "Green",
            totalStake = 100.0,
            winningNumber = 7
        )
        assertEquals(196.0, greenWin, 0.01)

        // Number 5 is Violet + Green split -> Green wins 1.5x, Violet wins 4.5x
        val greenOnSplitFive = GameRules.calculatePayout(
            selectionType = BetSelectionType.COLOR,
            selectionValue = "Green",
            totalStake = 100.0,
            winningNumber = 5
        )
        assertEquals(147.0, greenOnSplitFive, 0.01)

        val violetWin = GameRules.calculatePayout(
            selectionType = BetSelectionType.COLOR,
            selectionValue = "Violet",
            totalStake = 100.0,
            winningNumber = 0
        )
        assertEquals(441.0, violetWin, 0.01)

        val numberWin = GameRules.calculatePayout(
            selectionType = BetSelectionType.NUMBER,
            selectionValue = "7",
            totalStake = 100.0,
            winningNumber = 7
        )
        assertEquals(882.0, numberWin, 0.01)

        val numberLoss = GameRules.calculatePayout(
            selectionType = BetSelectionType.NUMBER,
            selectionValue = "3",
            totalStake = 100.0,
            winningNumber = 7
        )
        assertTrue(numberLoss == 0.0)

        val dragonWin = GameRules.calculatePayout(
            selectionType = BetSelectionType.SIDE,
            selectionValue = "Dragon",
            totalStake = 100.0,
            winningNumber = 6
        )
        assertEquals(196.0, dragonWin, 0.01)

        val tigerLoss = GameRules.calculatePayout(
            selectionType = BetSelectionType.SIDE,
            selectionValue = "Tiger",
            totalStake = 100.0,
            winningNumber = 6
        )
        assertTrue(tigerLoss == 0.0)
    }

    @Test
    fun `custom admin multipliers and period clock rollover work accurately`() {
        val customMultipliers = PayoutMultipliers(
            greenRedMultiplier = 2.1,
            violetMultiplier = 5.0,
            numberMultiplier = 9.5,
            dragonTigerMultiplier = 1.95,
            platformFeeRate = 0.0
        )

        val customNumber = GameRules.calculatePayout(
            selectionType = BetSelectionType.NUMBER,
            selectionValue = "8",
            totalStake = 100.0,
            winningNumber = 8,
            multipliers = customMultipliers
        )
        assertEquals(950.0, customNumber, 0.01)

        val customTiger = GameRules.calculatePayout(
            selectionType = BetSelectionType.SIDE,
            selectionValue = "Tiger",
            totalStake = 100.0,
            winningNumber = 1,
            multipliers = customMultipliers
        )
        assertEquals(195.0, customTiger, 0.01)

        val state30s = RoundPeriodClock.computeRoundState(GameMode.SEC_30, 1_760_000_000_000L)
        val nextId = RoundPeriodClock.computeNextPeriodId(GameMode.SEC_30, state30s.periodId)
        assertEquals(state30s.periodIndex + 1, RoundPeriodClock.periodIndexFromId(nextId))
    }
}
