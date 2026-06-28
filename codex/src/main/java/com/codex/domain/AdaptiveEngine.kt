package com.codex.domain

object AdaptiveEngine {
    fun calcolaDifficolta(accuratezza: Float, velocitaMedia: Float): Float =
        when {
            accuratezza > 0.9f && velocitaMedia < 3f -> 4f  // Elite
            accuratezza > 0.8f -> 3f
            accuratezza > 0.65f -> 2f
            else -> 1f
        }

    fun filtraPerDifficolta(
        esercizi: List<com.codex.data.model.ExerciseItem>,
        difficolta: Float
    ): List<com.codex.data.model.ExerciseItem> {
        val max = difficolta + 1f
        val filtrati = esercizi.filter { it.difficulty <= max }
        return filtrati.ifEmpty { esercizi }
    }
}
