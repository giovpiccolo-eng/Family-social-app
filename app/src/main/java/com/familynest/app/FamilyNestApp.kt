package com.familynest.app

import android.app.Application
import com.familynest.app.di.Graph
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class FamilyNestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
