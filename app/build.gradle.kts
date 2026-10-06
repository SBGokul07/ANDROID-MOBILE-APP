plugins {
    id("com.android.application")
}

android {
    namespace = "com.aeromaintenance.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aeromaintenance.ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0-java"
    }

    signingConfigs {
        // Demo-only key, committed on purpose so every build can be installed over
        // the previous one. Do not use it for anything that goes to a store.
        create("demo") {
            storeFile = file("aero-demo.jks")
            storePassword = "aerodemo"
            keyAlias = "aero"
            keyPassword = "aerodemo"
        }
    }

    buildTypes {
        release {
            // Optimised, shrunk build for the live demo.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("demo")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // CI runs `lintDebug` to catch any call to an API newer than minSdk 26
        // (the emulator runs Android 14, so it would not crash there).
        checkOnly += "NewApi"
        abortOnError = true
    }
}

dependencies {
    // No runtime libraries: the app uses only the Android SDK.
    testImplementation("junit:junit:4.13.2")
}
