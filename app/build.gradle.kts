import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Where the upload key and its passwords are named, kept out of the repository.
 *
 * A signing key is the one secret here that can't be rotated quietly: it is what Play
 * checks every update against, so anyone holding it holds the app. The keystore and the
 * passwords to it therefore live beside the project rather than in it, in a
 * keystore.properties at the project root holding four lines:
 *
 *     storeFile=upload-keystore.jks
 *     storePassword=...
 *     keyAlias=upload
 *     keyPassword=...
 *
 * Absent, everything still builds. Only a signed release can't be produced, which is
 * the one thing a fresh clone has no business producing.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}
val canSignRelease = keystoreProperties.getProperty("storeFile") != null

android {
    // The package the source is written under, and the one R and BuildConfig are
    // generated into. Play never reads it, so it needn't match the line below — but it
    // was moved to agree while the app is still unreleased and the move costs nothing.
    namespace = "app.visionov.visionwidget"
    compileSdk = 37

    defaultConfig {
        // What the app is called on a device and in Play, fixed by the Console entry it
        // was reserved under. Permanent from the first upload: a second name would be a
        // second app, and nobody on the first could be updated across to it.
        applicationId = "app.visionov.visionwidget"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "1.0"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Left unsigned when there are no credentials to sign with, rather than
            // falling back to the debug key: a debug-signed bundle is one Play rejects,
            // and it should be obvious which of the two came out.
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Carried from the first upload rather than added when the purchase flow is
    // written. Play reads the billing permission off the manifest and, finding no
    // library behind it, takes the app for one still on the AIDL interface it
    // retired — and refuses the bundle outright.
    implementation(libs.billing.ktx)
}
