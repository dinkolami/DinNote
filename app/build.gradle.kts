import java.net.URI

plugins { id("com.android.application") }
android { namespace = "com.dinkolami.dinnote"; compileSdk = 35
 defaultConfig { applicationId = "com.dinkolami.dinnote.fresh"; minSdk = 26; targetSdk = 35; versionCode = 2; versionName = "1.0.1-fresh" }
 signingConfigs { create("persistent") { storeFile = file(System.getenv("DINNOTE_KEYSTORE_PATH") ?: "missing-release-key.jks"); storePassword = System.getenv("DINNOTE_STORE_PASSWORD"); keyAlias = System.getenv("DINNOTE_KEY_ALIAS"); keyPassword = System.getenv("DINNOTE_KEY_PASSWORD") } }
 buildTypes { getByName("release") { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("persistent") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}


dependencies { implementation("com.tom-roush:pdfbox-android:2.0.27.0") }

val alluraAssets = layout.buildDirectory.dir("generated/alluraAssets")
val prepareAllura by tasks.registering {
    outputs.dir(alluraAssets)
    doLast {
        val dir = alluraAssets.get().asFile
        dir.mkdirs()
        for (name in listOf("Allura-Regular.ttf", "OFL.txt")) {
            val target = dir.resolve(name)
            URI("https://raw.githubusercontent.com/google/fonts/main/ofl/allura/" + name).toURL().openStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        }
    }
}
android.sourceSets.getByName("main").assets.srcDir(alluraAssets)
tasks.named("preBuild").configure { dependsOn(prepareAllura) }
