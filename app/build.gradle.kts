plugins { id("com.android.application") }
android { namespace = "com.dinkolami.dinnote"; compileSdk = 35
 defaultConfig { applicationId = "com.dinkolami.dinnote.fresh"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0.0-fresh" }
 signingConfigs { create("persistent") { storeFile = file(System.getenv("DINNOTE_KEYSTORE_PATH") ?: "missing-release-key.jks"); storePassword = System.getenv("DINNOTE_STORE_PASSWORD"); keyAlias = System.getenv("DINNOTE_KEY_ALIAS"); keyPassword = System.getenv("DINNOTE_KEY_PASSWORD") } }
 buildTypes { getByName("release") { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("persistent") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
