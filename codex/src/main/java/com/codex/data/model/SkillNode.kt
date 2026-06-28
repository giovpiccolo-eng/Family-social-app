package com.codex.data.model

data class SkillNode(
    val id: String,
    val nome: String,
    val descrizione: String,
    val act: Int,
    val day: Int,
    val requires: List<String> = emptyList(),
    val x: Float = 0f,
    val y: Float = 0f
)

data class SkillTree(
    val nodes: List<SkillNode>
)
