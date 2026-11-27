package com.example.hostelfix.data

import kotlin.random.Random

object RegistrationNumberHelper {
    private val allowedPrefixes = listOf("121", "122", "123", "124", "125")
    private val digits = ('0'..'9').toList()

    private val regNumberRegex = Regex("^(121|122|123|124|125)\\d{5}$")

    fun isValid(value: String): Boolean {
        return regNumberRegex.matches(value.trim())
    }

    fun generate(
        prefix: String? = null,
        existingNumbers: Set<String> = emptySet(),
        random: Random = Random.Default
    ): String {
        val validPrefix = prefix?.trim()?.takeIf { it in allowedPrefixes }
            ?: allowedPrefixes.random(random)
        val normalizedExisting = existingNumbers.mapTo(hashSetOf()) { it.trim() }

        repeat(200) {
            val suffix = (1..5).map { digits.random(random) }.joinToString("")
            val candidate = "$validPrefix$suffix"
            if (candidate !in normalizedExisting) {
                return candidate
            }
        }
        error("Unable to generate a unique registration number for prefix $validPrefix")
    }
}