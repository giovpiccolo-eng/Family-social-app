package com.dispensa.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Photo(
    val id: String,
    val fileName: String,
    val createdAt: Long,
)

@Serializable
enum class DispensaStatus { GENERATING, READY, FAILED }

@Serializable
data class Dispensa(
    val id: String,
    val htmlFileName: String? = null,
    val createdAt: Long,
    val status: DispensaStatus = DispensaStatus.GENERATING,
    val error: String? = null,
    val sourceNotes: String = "",
)

@Serializable
data class Topic(
    val id: String,
    val title: String,
    val subject: String = "",
    val createdAt: Long,
    val photos: List<Photo> = emptyList(),
    val dispense: List<Dispensa> = emptyList(),
)
