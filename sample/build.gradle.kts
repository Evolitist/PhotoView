import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 35
    namespace = "uk.co.senab.photoview.sample"

    defaultConfig {
        applicationId = "uk.co.senab.photoview.sample"
        minSdk = 21
        targetSdk = 35
        versionCode = 100
        versionName = "1.0"
    }

    lint {
        abortOnError = false
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)

    implementation(libs.google.material)

    implementation(libs.glide)
    implementation(libs.coil)

    implementation(project(":photoview"))
}
