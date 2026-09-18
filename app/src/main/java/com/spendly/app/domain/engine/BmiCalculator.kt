package com.spendly.app.domain.engine

enum class BmiCategory {
    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
    OBESE
}

/**
 * Standalone wellness calculation (V2 spec section 23) - never reads or
 * writes Transaction/Budget/Safe-to-Spend/Analytics data, and nothing in
 * those calculations ever reads BMI data. Kept in domain/engine only for
 * consistency with the rest of the deterministic engine, not because it
 * participates in the financial engine.
 */
fun calculateBmi(heightCm: Double, weightKg: Double): Double {
    if (heightCm <= 0.0 || weightKg <= 0.0) return 0.0
    val heightM = heightCm / 100.0
    return weightKg / (heightM * heightM)
}

/** WHO standard BMI thresholds. */
fun classifyBmi(bmi: Double): BmiCategory = when {
    bmi < 18.5 -> BmiCategory.UNDERWEIGHT
    bmi < 25.0 -> BmiCategory.NORMAL
    bmi < 30.0 -> BmiCategory.OVERWEIGHT
    else -> BmiCategory.OBESE
}
