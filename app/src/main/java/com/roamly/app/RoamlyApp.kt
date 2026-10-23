package com.roamly.app

import dagger.hilt.android.HiltAndroidApp
import android.app.Application
import com.roamly.app.manager.OsmdroidManager

@HiltAndroidApp
class RoamlyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OsmdroidManager.ensure(this)
    }
}