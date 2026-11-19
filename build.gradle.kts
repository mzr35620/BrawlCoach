plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.brawlcoach"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.brawlcoach"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
}
