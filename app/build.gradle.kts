plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.anjan.hydrate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.anjan.hydrate"
        minSdk = 26          // Android 8.0+
        targetSdk = 35
        versionCode = 11
        versionName = "3.1"
    }

    // Fixed signing key so every new APK installs over the previous one.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// No third-party libraries: only the Android framework is used.
dependencies { }
