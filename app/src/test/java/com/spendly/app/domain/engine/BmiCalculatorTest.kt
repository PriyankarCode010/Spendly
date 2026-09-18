package com.spendly.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class BmiCalculatorTest {

    @Test
    fun `calculateBmi computes weight over height squared in meters`() {
        val bmi = calculateBmi(heightCm = 180.0, weightKg = 81.0)

        assertEquals(25.0, bmi, 0.01)
    }

    @Test
    fun `calculateBmi is zero for non-positive height or weight`() {
        assertEquals(0.0, calculateBmi(heightCm = 0.0, weightKg = 70.0), 0.001)
        assertEquals(0.0, calculateBmi(heightCm = 170.0, weightKg = 0.0), 0.001)
        assertEquals(0.0, calculateBmi(heightCm = -10.0, weightKg = 70.0), 0.001)
    }

    @Test
    fun `classifyBmi thresholds match WHO standard bands`() {
        assertEquals(BmiCategory.UNDERWEIGHT, classifyBmi(18.4))
        assertEquals(BmiCategory.NORMAL, classifyBmi(18.5))
        assertEquals(BmiCategory.NORMAL, classifyBmi(24.9))
        assertEquals(BmiCategory.OVERWEIGHT, classifyBmi(25.0))
        assertEquals(BmiCategory.OVERWEIGHT, classifyBmi(29.9))
        assertEquals(BmiCategory.OBESE, classifyBmi(30.0))
    }
}
