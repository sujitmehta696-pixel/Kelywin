package com.example.adminapp

import android.app.Application
import com.example.adminapp.data.AdminFirebaseRepository
import com.example.adminapp.services.AdminRoundCoordinator
import com.google.firebase.FirebaseApp

interface AdminRuntimeHost {
    val adminFirebaseRepository: AdminFirebaseRepository
    val adminRoundCoordinator: AdminRoundCoordinator
}

class AdminApplication : Application(), AdminRuntimeHost {
    override lateinit var adminFirebaseRepository: AdminFirebaseRepository
        private set

    override lateinit var adminRoundCoordinator: AdminRoundCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        adminFirebaseRepository = AdminFirebaseRepository(applicationContext)
        adminRoundCoordinator = AdminRoundCoordinator(adminFirebaseRepository)
    }
}
