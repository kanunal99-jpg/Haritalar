plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.haritalar.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.haritalar.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":core"))
    implementation("org.maplibre.gl:android-sdk:13.4.1")
}
