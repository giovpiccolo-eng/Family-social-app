package com.codex.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val day: Int,
    val completato: Boolean = false,
    val xpGuadagnato: Int = 0,
    val aureiGuadagnati: Int = 0,
    val dataCompletamento: Long = 0L
)

@Entity(tableName = "mastery")
data class MasteryEntity(
    @PrimaryKey val skillId: String,
    val punteggio: Float = 0f,     // 0..100
    val ultimaRevisione: Long = 0L,
    val totaleRisposte: Int = 0,
    val risposteCorrette: Int = 0
)

@Entity(tableName = "srs_cards")
data class SrsCardEntity(
    @PrimaryKey val cardId: String,  // exercise id
    val intervallo: Int = 1,         // giorni
    val easeFactor: Float = 2.5f,
    val ripetizioni: Int = 0,
    val prossimaRevisione: Long = 0L
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val nome: String,
    val descrizione: String,
    val sblocato: Boolean = false,
    val dataSblocco: Long = 0L,
    val icona: String = "trophy"
)

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey val id: Int = 1,
    val xpTotale: Int = 0,
    val aureiTotali: Int = 0,
    val streakCorrente: Int = 0,
    val streakMassimo: Int = 0,
    val gettonigelo: Int = 3,
    val ultimaSessione: Long = 0L,
    val livello: Int = 1,
    val titolo: String = "Tiro",
    val temaScuro: Boolean = false,
    val audioAttivo: Boolean = true
)

@Entity(tableName = "session_log")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val day: Int,
    val timestamp: Long,
    val durata: Int,               // secondi
    val xp: Int,
    val aurei: Int,
    val accuratezza: Float,        // 0..1
    val roundCompletati: Int
)
