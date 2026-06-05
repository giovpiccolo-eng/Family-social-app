package com.dispensa.app

import android.app.Application
import com.dispensa.app.di.Graph

class DispensaApp : Application() {
    lateinit var graph: Graph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = Graph(this)
    }
}
