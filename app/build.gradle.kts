plugins {
    alias(libs.plugins.android.application)
    // Reads app/google-services.json (the LEEF Firebase project).
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.itismob.s03.group7.leef"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.itismob.s03.group7.leef"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.swiperefreshlayout)

    // Loads event photos from their web addresses (the photos themselves are database records).
    implementation(libs.glide)

    // Draws the check-in QR code on the device (Screen 9). Free, no network needed.
    implementation(libs.zxing.core)

    // Firebase: the BoM keeps every Firebase library on matching versions.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // "Continue with Google": Credential Manager shows Google's account sheet and returns an ID token
    // that Firebase Authentication exchanges for a LEEF sign-in. Free on the Spark plan.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}