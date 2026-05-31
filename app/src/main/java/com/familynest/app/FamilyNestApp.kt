package com.familynest.app

import android.app.Application
import com.familynest.app.di.Graph
import com.familynest.app.notifications.Notifications

class FamilyNestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // FirebaseApp is auto-initialised by the firebase-common ContentProvider,
        // so we only need to wire up our repositories and notification channel here.
        Graph.init()
        Notifications.ensureChannel(this)
    }
}
