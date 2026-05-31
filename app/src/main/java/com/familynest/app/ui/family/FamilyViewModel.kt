package com.familynest.app.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familynest.app.data.model.AppUser
import com.familynest.app.data.model.Family
import com.familynest.app.di.Graph
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class FamilyViewModel(familyId: String) : ViewModel() {

    val family: StateFlow<Family?> =
        Graph.family.familyFlow(familyId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val members: StateFlow<List<AppUser>> =
        Graph.family.familyFlow(familyId)
            .flatMapLatest { fam -> Graph.family.membersFlow(fam?.memberIds.orEmpty()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun signOut() = Graph.auth.signOut()
}
