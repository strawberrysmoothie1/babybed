import com.android.build.gradle.internal.dsl.BaseAppModuleExtension

plugins {
    id("com.android.application") version "8.8.0"
    id("org.jetbrains.kotlin.android") version "1.8.20"
    id("com.google.gms.google-services") // 구글 서비스 플러그인 추가
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // kotlinOptions 설정
    (this as BaseAppModuleExtension).kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // AndroidX, Material
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase BOM + FCM
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
