package com.codex.data.model

import com.google.gson.annotations.SerializedName

data class ExerciseItem(
    val id: String,
    val mode: String,           // recall | decode | forge | raid | crack | dive
    val skill: String,          // nodo skill tree (es. "casi.nom_acc")
    val day: Int,
    val act: Int,
    val difficulty: Float = 1f, // 1..5
    val prompt: String = "",
    val latin: String = "",
    val answer: String = "",
    val distractors: List<String> = emptyList(),
    val insight: String = "",
    val dive: String = "",
    val reward: Reward = Reward(),
    // forge-specific
    val lemma: String = "",
    val stem: String = "",
    val target: String = "",
    val hint: String = "",
    // recall-specific
    val front: String = "",
    val back: String = "",
    // crack-specific (boss)
    val title: String = "",
    val translation: String = "",
    val scaffold: List<ScaffoldQuestion> = emptyList(),
    val sigillo: String = ""
)

data class Reward(
    val xp: Int = 10,
    val aurei: Int = 2
)

data class ScaffoldQuestion(
    val question: String,
    val answer: String,
    val options: List<String>
)
