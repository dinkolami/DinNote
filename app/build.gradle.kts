plugins { id("com.android.application") }
android {
 namespace = "com.dinkolami.dinnote"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.dinkolami.dinnote"
  minSdk = 26
  targetSdk = 35
  versionCode = 3
  versionName = "2.0.0-beta1"
 }
 val keyFile = System.getenv("DINNOTE_KEYSTORE_FILE")
 val storePass = System.getenv("DINNOTE_STORE_PASSWORD")
 val keyAlias = System.getenv("DINNOTE_KEY_ALIAS")
 val keyPass = System.getenv("DINNOTE_KEY_PASSWORD")
 signingConfigs {
  create("releaseKey") {
   if (!keyFile.isNullOrBlank() && !storePass.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPass.isNullOrBlank()) {
    storeFile = file(keyFile)
    storePassword = storePass
    this.keyAlias = keyAlias
    keyPassword = keyPass
   }
  }
 }
 buildTypes {
  getByName("release") {
   isMinifyEnabled = false
   signingConfig = signingConfigs.getByName("releaseKey")
  }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
