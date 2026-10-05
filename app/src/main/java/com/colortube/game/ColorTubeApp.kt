package com.colortube.game

import android.app.Application

class ColorTubeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AdManager.initialize(this)
    }
}
