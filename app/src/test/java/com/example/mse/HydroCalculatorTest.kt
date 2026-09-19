package com.example.mse

import com.example.mse.logic.AuthValidator
import com.example.mse.logic.HydroCalculator
import com.example.mse.logic.HydroCalculatorInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HydroCalculatorTest {

    @Test
    fun testHydroPowerCalculation() {
        val input = HydroCalculatorInput(
            waterHeadMeters = 15.0,
            waterFlowM3PerSec = 350.0,
            efficiencyPercentage = 88.0,
            operatingHoursPerDay = 24.0
        )

        val result = HydroCalculator.calculateEnergy(input)

        assertEquals(45.32, result.powerMw, 0.1)
        assertTrue(result.dailyGenerationMwh > 0)
        assertTrue(result.monthlyGenerationMwh > 0)
    }

    @Test
    fun testAuthValidatorEmail() {
        val validEmail = AuthValidator.validateEmail("operator@hydro.com")
        assertTrue(validEmail.isValid)

        val invalidEmail = AuthValidator.validateEmail("invalid-email")
        assertFalse(invalidEmail.isValid)
    }

    @Test
    fun testAuthValidatorPassword() {
        val validPass = AuthValidator.validatePassword("pass1234")
        assertTrue(validPass.isValid)

        val shortPass = AuthValidator.validatePassword("123")
        assertFalse(shortPass.isValid)
    }
}
