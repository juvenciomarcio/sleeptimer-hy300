plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.sleeptimer.hy300"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.sleeptimer.hy300"
        minSdk = 21
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("dev.mobile:dadb:1.2.7")
}
