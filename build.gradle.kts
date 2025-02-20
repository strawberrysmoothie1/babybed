buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // 구글 서비스 플러그인을 여기서 선언
        classpath("com.google.gms:google-services:4.3.15")

    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
