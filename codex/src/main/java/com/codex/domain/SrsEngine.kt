package com.codex.domain

object SrsEngine {
    /**
     * SM-2 semplificato.
     * qualita: 0=blackout, 1=difficile, 2=ok, 3=facile
     * Restituisce Triple(nuovoIntervallo, nuovoEaseFactor, nuoveRipetizioni)
     */
    fun aggiorna(
        intervalloCorrente: Int,
        easeCorrente: Float,
        ripetizioniCorrente: Int,
        qualita: Int
    ): Triple<Int, Float, Int> {
        return if (qualita < 2) {
            Triple(1, maxOf(1.3f, easeCorrente - 0.2f), 0)
        } else {
            val nuovoIntervallo = when (ripetizioniCorrente) {
                0 -> 1
                1 -> 4
                else -> (intervalloCorrente * easeCorrente).toInt().coerceAtLeast(1)
            }
            val deltaEase = 0.1f - (3 - qualita) * (0.08f + (3 - qualita) * 0.02f)
            val nuovoEase = (easeCorrente + deltaEase).coerceIn(1.3f, 2.5f)
            Triple(nuovoIntervallo, nuovoEase, ripetizioniCorrente + 1)
        }
    }
}
