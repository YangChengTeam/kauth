plugins {
    alias(libs.plugins.kauth.android.feature)
}

android {
    namespace = "com.yangfeisg.kauth.feature.home"
}

dependencies {
    implementation(project(":core:backup"))
}
