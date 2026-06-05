package com.dispensa.app.data.repository

import android.content.Context
import com.dispensa.app.data.model.Dispensa
import com.dispensa.app.data.model.DispensaStatus
import com.dispensa.app.data.model.Photo
import com.dispensa.app.data.model.Topic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Simple JSON-file backed store. One JSON manifest lists all topics; photo bytes
 * and generated HTML dispense live as side-files under files/topics/{topicId}/.
 *
 * No DB — this app belongs to a single user and the dataset is tiny (a few
 * dozen topics, each with a handful of photos).
 */
class TopicRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()

    private val manifest: File
        get() = File(context.filesDir, "topics.json")

    private val topicsRoot: File
        get() = File(context.filesDir, "topics").also { it.mkdirs() }

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics: StateFlow<List<Topic>> = _topics.asStateFlow()

    init { load() }

    private fun load() {
        val all: List<Topic> = if (manifest.exists()) {
            runCatching { json.decodeFromString<List<Topic>>(manifest.readText()) }
                .getOrDefault(emptyList())
        } else emptyList()
        _topics.value = all.sortedByDescending { it.createdAt }
    }

    private suspend fun persist(list: List<Topic>) = mutex.withLock {
        manifest.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Topic.serializer()), list))
        _topics.value = list.sortedByDescending { it.createdAt }
    }

    fun topicById(id: String): Topic? = _topics.value.firstOrNull { it.id == id }

    suspend fun createTopic(title: String, subject: String): Topic {
        val t = Topic(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            subject = subject.trim(),
            createdAt = System.currentTimeMillis(),
        )
        File(topicsRoot, t.id).mkdirs()
        File(topicsRoot, "${t.id}/photos").mkdirs()
        File(topicsRoot, "${t.id}/dispense").mkdirs()
        persist(_topics.value + t)
        return t
    }

    suspend fun deleteTopic(id: String) {
        File(topicsRoot, id).deleteRecursively()
        persist(_topics.value.filterNot { it.id == id })
    }

    /** Returns the absolute file in which the caller should write the photo bytes. */
    fun newPhotoFile(topicId: String): File {
        val dir = File(topicsRoot, "$topicId/photos").also { it.mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    suspend fun addPhoto(topicId: String, file: File) {
        val photo = Photo(
            id = UUID.randomUUID().toString(),
            fileName = file.name,
            createdAt = System.currentTimeMillis(),
        )
        update(topicId) { it.copy(photos = it.photos + photo) }
    }

    suspend fun removePhoto(topicId: String, photoId: String) {
        val topic = topicById(topicId) ?: return
        val photo = topic.photos.firstOrNull { it.id == photoId } ?: return
        File(topicsRoot, "$topicId/photos/${photo.fileName}").delete()
        update(topicId) { it.copy(photos = it.photos.filterNot { p -> p.id == photoId }) }
    }

    fun photoFile(topicId: String, photo: Photo): File =
        File(topicsRoot, "$topicId/photos/${photo.fileName}")

    fun dispensaFile(topicId: String, dispensa: Dispensa): File? =
        dispensa.htmlFileName?.let { File(topicsRoot, "$topicId/dispense/$it") }

    suspend fun startDispensa(topicId: String, notes: String): Dispensa {
        val d = Dispensa(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            status = DispensaStatus.GENERATING,
            sourceNotes = notes,
        )
        update(topicId) { it.copy(dispense = it.dispense + d) }
        return d
    }

    suspend fun completeDispensa(topicId: String, dispensaId: String, html: String) {
        val fileName = "$dispensaId.html"
        val file = File(topicsRoot, "$topicId/dispense/$fileName").apply {
            parentFile?.mkdirs()
            writeText(html)
        }
        update(topicId) { topic ->
            topic.copy(dispense = topic.dispense.map { d ->
                if (d.id == dispensaId) d.copy(
                    htmlFileName = file.name,
                    status = DispensaStatus.READY,
                ) else d
            })
        }
    }

    suspend fun failDispensa(topicId: String, dispensaId: String, error: String) {
        update(topicId) { topic ->
            topic.copy(dispense = topic.dispense.map { d ->
                if (d.id == dispensaId) d.copy(status = DispensaStatus.FAILED, error = error) else d
            })
        }
    }

    suspend fun deleteDispensa(topicId: String, dispensaId: String) {
        val topic = topicById(topicId) ?: return
        val d = topic.dispense.firstOrNull { it.id == dispensaId } ?: return
        d.htmlFileName?.let { File(topicsRoot, "$topicId/dispense/$it").delete() }
        update(topicId) { it.copy(dispense = it.dispense.filterNot { x -> x.id == dispensaId }) }
    }

    private suspend fun update(topicId: String, transform: (Topic) -> Topic) {
        val current = _topics.value
        val updated = current.map { if (it.id == topicId) transform(it) else it }
        persist(updated)
    }
}
