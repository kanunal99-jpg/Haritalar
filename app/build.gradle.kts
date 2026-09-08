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

        val tomTomApiKey = providers.gradleProperty("TOMTOM_API_KEY")
            .orElse(providers.environmentVariable("TOMTOM_API_KEY"))
            .orElse("")
            .get()
        buildConfigField("String", "TOMTOM_API_KEY", "\"${tomTomApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    buildFeatures {
        buildConfig = true
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
    implementation(project(":core"))
    implementation("org.maplibre.gl:android-sdk:13.4.1")
    testImplementation(kotlin("test"))
}
