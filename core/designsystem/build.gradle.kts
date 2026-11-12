plugins {
    alias(libs.plugins.kauth.android.library)
    alias(libs.plugins.kauth.android.compose)
}

android {
    namespace = "com.yangfeisg.kauth.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
}
