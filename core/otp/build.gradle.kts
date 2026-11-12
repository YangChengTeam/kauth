plugins {
    alias(libs.plugins.kauth.jvm.library)
}

dependencies {
    api(project(":core:model"))
}
