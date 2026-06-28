package com.codex

import android.app.Application
import com.codex.data.db.CodexDatabase
import com.codex.data.db.entity.AchievementEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CodexApp : Application() {

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            initAchievements()
        }
    }

    private suspend fun initAchievements() {
        val dao = CodexDatabase.get(applicationContext).achievementDao()
        val achievements = listOf(
            AchievementEntity("primo_giorno", "Primo Protocollo", "Completa il Giorno 1"),
            AchievementEntity("incursione_perfetta", "Incursore Perfetto", "Completa un'Incursione senza errori"),
            AchievementEntity("settimana_decoder", "Settimana del Decoder", "Streak di 7 giorni consecutivi"),
            AchievementEntity("boss_atto1", "Primo Codice Decifrato", "Supera il Boss dell'Atto I (Giorno 7)"),
            AchievementEntity("boss_atto2", "Decifratore di Fedro", "Supera il Boss dell'Atto II (Giorno 14)"),
            AchievementEntity("imperator", "Imperator", "Completa l'intera campagna — 21 giorni"),
            AchievementEntity("combo_5", "Combo ×5", "Raggiungi un moltiplicatore combo di 5"),
            AchievementEntity("maestro_declinazioni", "Maestro delle Declinazioni", "Porta tutte e 5 le declinazioni all'80%"),
            AchievementEntity("velocita_luce", "Velocità della Luce", "Rispondi correttamente in meno di 2 secondi × 10"),
            AchievementEntity("archivio_ricostruito", "L'Archivio è Ricostruito", "Porta ogni nodo al 100%")
        )
        achievements.forEach { ach ->
            dao.upsert(ach)
        }
    }
}
