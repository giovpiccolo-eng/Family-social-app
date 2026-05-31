package com.familynest.app.di

import com.familynest.app.data.repository.AuthRepository
import com.familynest.app.data.repository.FamilyRepository
import com.familynest.app.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Tiny manual dependency graph. Initialised once in [com.familynest.app.FamilyNestApp]
 * and read by ViewModels via their no-arg constructors. Keeps the build free of
 * annotation processors while still giving us single instances of each repository.
 */
object Graph {
    lateinit var auth: AuthRepository
        private set
    lateinit var family: FamilyRepository
        private set
    lateinit var posts: PostRepository
        private set

    fun init() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        auth = AuthRepository(firebaseAuth, firestore)
        family = FamilyRepository(firestore)
        posts = PostRepository(firestore, storage)
    }
}
