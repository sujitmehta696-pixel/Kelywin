package com.example.shared

import com.example.shared.model.BetSelectionType
import com.example.shared.model.PayoutMultipliers
import com.example.shared.model.UserBet
import com.example.shared.model.BetStatus
import com.example.shared.model.XinLogicPattern
import com.example.shared.model.XinLogicResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XinLogicResolverTest {

    private fun sampleBet(
        type: BetSelectionType,
        value: String,
        stake: Double
    ) = UserBet(
        id = "B_${type.name}_$value",
        userId = "u1",
        periodId = "20261121100000100",
        modeId = "30sec",
        selectionType = type,
        selectionValue = value,
        baseStake = stake,
        multiplier = 1,
        totalStake = stake,
        status = BetStatus.PENDING,
        payout = 0.0,
        createdAtMillis = 1L
    )

    @Test
    fun `two high four low repeats cleanly over 6 period cycles`() {
        val anchor = 100
        val highSteps = (0 until 12).map { offset ->
            XinLogicResolver.stepInfo(anchor + offset, anchor, XinLogicPattern.TWO_HIGH_FOUR_LOW).isHighStep
        }
        assertEquals(
            listOf(
                true, true, false, false, false, false,
                true, true, false, false, false, false
            ),
            highSteps
        )
    }

    @Test
    fun `two low four high repeats cleanly over 6 period cycles`() {
        val anchor = 50
        val highSteps = (0 until 12).map { offset ->
            XinLogicResolver.stepInfo(anchor + offset, anchor, XinLogicPattern.TWO_LOW_FOUR_HIGH).isHighStep
        }
        assertEquals(
            listOf(
                false, false, true, true, true, true,
                false, false, true, true, true, true
            ),
            highSteps
        )
    }

    @Test
    fun `combined number color and dragon tiger stakes pick highest on high step and lowest on low step`() {
        val anchor = 200
        val multipliers = PayoutMultipliers(platformFeeRate = 0.0)
        // Huge stake on Number 8 (Red + Dragon) and Tiger + Green modest stakes, zero on Number 2
        val bets = listOf(
            sampleBet(BetSelectionType.NUMBER, "8", 500.0), // 8 pays 4500
            sampleBet(BetSelectionType.COLOR, "Red", 200.0), // Red pays 400
            sampleBet(BetSelectionType.SIDE, "Dragon", 300.0), // Dragon pays 600
            sampleBet(BetSelectionType.SIDE, "Tiger", 10.0), // Tiger pays 20
            sampleBet(BetSelectionType.COLOR, "Green", 10.0) // Green pays 20
        )

        // Step 1 (offset 0) = HIGH in TWO_HIGH_FOUR_LOW -> should pick #8 (payout 4500+400+600 = 5500)
        val highPick = XinLogicResolver.resolveNumber(
            pendingBets = bets,
            periodIndex = anchor,
            anchorPeriodIndex = anchor,
            pattern = XinLogicPattern.TWO_HIGH_FOUR_LOW,
            multipliers = multipliers,
            randomIndexPicker = { 0 }
        )
        assertEquals(8, highPick)

        // Step 3 (offset 2) = LOW in TWO_HIGH_FOUR_LOW -> Green + Tiger (numbers 1 or 3) pays 20+20 = 40
        // vs Red + Tiger (2,4) pays 420 -> minimum is 1 or 3!
        val lowPick = XinLogicResolver.resolveNumber(
            pendingBets = bets,
            periodIndex = anchor + 2,
            anchorPeriodIndex = anchor,
            pattern = XinLogicPattern.TWO_HIGH_FOUR_LOW,
            multipliers = multipliers,
            randomIndexPicker = { 0 }
        )
        assertTrue(lowPick in listOf(1, 3))

        // Full Control always picks lowest payout
        val fullControlPick = XinLogicResolver.resolveLowestPayoutNumber(
            pendingBets = bets,
            multipliers = multipliers,
            randomIndexPicker = { 0 }
        )
        assertTrue(fullControlPick in listOf(1, 3))
    }

    @Test
    fun `stepNumber stays in 1 to 6 even before anchor`() {
        val info = XinLogicResolver.stepInfo(periodIndex = 4, anchorPeriodIndex = 10, pattern = XinLogicPattern.TWO_HIGH_FOUR_LOW)
        assertTrue(info.stepNumber in 1..6)
        assertFalse(info.stepLabel.isBlank())
    }
}
