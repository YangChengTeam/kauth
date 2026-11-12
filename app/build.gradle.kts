plugins {
    alias(libs.plugins.kauth.android.application)
    alias(libs.plugins.kauth.android.compose)
    alias(libs.plugins.kauth.hilt)
}

android {
    namespace = "com.yangfeisg.kauth"

    defaultConfig {
        applicationId = "com.yangfeisg.kauth"
        // CI passes the tag's version; local builds stay 1.0.0.
        versionCode = providers.gradleProperty("kauth.versionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("kauth.versionName").orNull ?: "1.0.0"
    }

    // Release builds are signed in CI (.github/workflows/release.yml); the keystore never enters the repo.
    val releaseKeystore = System.getenv("KAUTH_KEYSTORE")?.let(::file)
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("KAUTH_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KAUTH_KEY_ALIAS")
                keyPassword = System.getenv("KAUTH_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":feature:home"))
    implementation(project(":feature:setup"))
    implementation(project(":feature:scan"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
}
