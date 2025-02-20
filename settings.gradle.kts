// settings.gradle.kts

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    // 원하는 경우, Kotlin 플러그인 버전도 여기서 선언할 수 있습니다.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin-android") {
                useVersion("1.8.20")
            }
        }
    }
}


dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // ...
    }
}

rootProject.name = "MyApplication"
include(":app")
