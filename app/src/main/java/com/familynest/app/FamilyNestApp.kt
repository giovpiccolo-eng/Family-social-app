package com.familynest.app

import android.app.Application
import com.familynest.app.di.Graph

class FamilyNestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // FirebaseApp is auto-initialised by the firebase-common ContentProvider,
        // so we only need to wire up our repositories here.
        Graph.init()
    }
}
