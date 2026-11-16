plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dinnote.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dinnote.app"
        minSdk = 26
        targetSdk = 36
        versionCode = (providers.gradleProperty("dinnoteVersionCode").orNull?.toIntOrNull() ?: 6)
        versionName = providers.gradleProperty("dinnoteVersionName").orNull ?: "2.4"
    }

    signingConfigs {
        getByName("debug") {
            val persistentKeystore = file("dinnote-debug.keystore")
            if (persistentKeystore.exists()) {
                storeFile = persistentKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
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

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("com.google.mlkit:translate:17.0.3")
}
