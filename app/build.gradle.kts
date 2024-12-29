plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
}

android {
    val major = 0
    val minor = 1
    val patch = 0

    defaultConfig {
        applicationId = "dev.thomas_kiljanczyk.bluetoothbroadcasting"
        minSdk = 23
        compileSdk = 35
        targetSdk = 35
        versionCode = major * 100000000 + minor * 10000 + patch
        versionName = "$major.$minor.$patch"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    namespace = "dev.thomas_kiljanczyk.bluetoothbroadcasting"
}

dependencies {
    // Submodules
    implementation(project(":bluetooth"))

    // App dependencies
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.android.material)

    // AndroidX
    implementation(libs.androidx.coreKtx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activityKtx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.recyclerView)

    // Hilt
    implementation(libs.hilt)
    ksp(libs.hiltCompiler)

    // LeakCanary
//    debugImplementation "com.squareup.leakcanary:leakcanary-android:$leakCanaryVersion"
}
