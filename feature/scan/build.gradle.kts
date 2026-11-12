plugins {
    alias(libs.plugins.kauth.android.feature)
}

android {
    namespace = "com.yangfeisg.kauth.feature.scan"
}

dependencies {
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // Decodes on the device: no Play services, no network.
    implementation(libs.zxing.core)
}
