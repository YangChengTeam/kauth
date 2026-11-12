plugins {
    alias(libs.plugins.kauth.android.library)
    alias(libs.plugins.kauth.hilt)
}

android {
    namespace = "com.yangfeisg.kauth.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:otp"))
    implementation(project(":core:backup"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
