plugins { id("com.android.application") }
android {
    namespace = "com.dinkolami.dinnote"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.dinkolami.dinnote"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"
    }
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("DINNOTE_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("DINNOTE_STORE_PASSWORD")
                keyAlias = System.getenv("DINNOTE_KEY_ALIAS")
                keyPassword = System.getenv("DINNOTE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
