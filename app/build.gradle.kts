plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.marevente.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.marevente.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-test"
    }
    buildFeatures { compose = true }
    signingConfigs {
        create("development") {
            storeFile = file("dev-signing.jks")
            storePassword = "duelflashdev"
            keyAlias = "duelflash-dev"
            keyPassword = "duelflashdev"
        }
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("development") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
