plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.aeromaintenance.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aeromaintenance.ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-prototype"
        vectorDrawables { useSupportLibrary = true }
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
            // Optimised build: smooth animations for the live demo.
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
