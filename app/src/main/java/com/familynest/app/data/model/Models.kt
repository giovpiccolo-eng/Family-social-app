package com.familynest.app.data.model

/** Roles within a family. Parents administer; children participate. */
enum class MemberRole { PARENT, CHILD }

/**
 * The kinds of things a family can share. The first three behave like a
 * "task manager" (they carry status / due dates / checklists); IDEA is the
 * aspirational, free-form heart of the app; IMAGE is a shared photo/memory.
 */
enum class PostType(val label: String, val emoji: String) {
    IDEA("Idea", "💡"),
    EVENT("Event", "📅"),
    PLAN("Plan", "🗺️"),
    PROJECT("Project", "🛠️"),
    IMAGE("Photo", "🖼️");

    /** Whether this type participates in the task-manager workflow. */
    val isActionable: Boolean
        get() = this == EVENT || this == PLAN || this == PROJECT

    companion object {
        fun fromName(name: String?): PostType =
            entries.firstOrNull { it.name == name } ?: IDEA
    }
}

enum class PostStatus(val label: String) {
    DREAMING("Dreaming"),
    PLANNED("Planned"),
    IN_PROGRESS("In progress"),
    DONE("Done");

    companion object {
        fun fromName(name: String?): PostStatus =
            entries.firstOrNull { it.name == name } ?: DREAMING
    }
}

/** A single checklist item attached to an actionable post. */
data class TaskItem(
    val id: String = "",
    val text: String = "",
    val done: Boolean = false,
    val assigneeId: String = "",
    val assigneeName: String = "",
)

/** A comment on a post (mirrors `families/{id}/posts/{id}/comments/{id}`). */
data class Comment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** A family member's profile (mirrors a Firestore `users/{uid}` document). */
data class AppUser(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val familyId: String = "",
    val role: String = MemberRole.CHILD.name,
    val fcmTokens: List<String> = emptyList(),
) {
    val roleEnum: MemberRole
        get() = runCatching { MemberRole.valueOf(role) }.getOrDefault(MemberRole.CHILD)
}

/** A family group (mirrors a Firestore `families/{id}` document). */
data class Family(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val inviteCode: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

/** A shared item in the family feed (mirrors a Firestore `families/{id}/posts/{id}`). */
data class Post(
    val id: String = "",
    val familyId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val type: String = PostType.IDEA.name,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val status: String = PostStatus.DREAMING.name,
    val dueDate: Long? = null,
    val tasks: List<TaskItem> = emptyList(),
    val likedBy: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    val typeEnum: PostType get() = PostType.fromName(type)
    val statusEnum: PostStatus get() = PostStatus.fromName(status)
    val completedTaskCount: Int get() = tasks.count { it.done }
}
