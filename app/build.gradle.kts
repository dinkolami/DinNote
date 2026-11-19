plugins { id("com.android.application") }
android { namespace = "com.dinkolami.dinnote"; compileSdk = 35
 defaultConfig { applicationId = "com.dinkolami.dinnote"; minSdk = 26; targetSdk = 35; versionCode = 3; versionName = "3.0.2-clean" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
