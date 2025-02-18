import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Use your version catalog plugin aliases:
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.hilt)
    id("org.jetbrains.kotlin.kapt")
    // If needed, also use:
    // alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.ainotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.ainotes"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    // Align the Compose compiler with your Kotlin version (1.9.10)
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // ----------------------------------------------------------
    // Compose
    // ----------------------------------------------------------
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // AndroidX & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    // ----------------------------------------------------------
    // Firebase - Downgrade BOM for Kotlin 1.9 compatibility
    // ----------------------------------------------------------
    // Comment out the older "libs.firebaseBom" usage:
    // implementation(platform(libs.firebaseBom))

    // Use an explicit stable BOM that doesn't pull in Kotlin 2.x
    implementation(platform("com.google.firebase:firebase-bom:32.4.0"))

    // The BOM will manage versions for these artifacts:
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // ----------------------------------------------------------
    // Coroutines - Downgrade from 1.10.x to 1.7.x for Kotlin 1.9
    // ----------------------------------------------------------
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // ----------------------------------------------------------
    // Hilt (runtime + compiler)
    // ----------------------------------------------------------
    implementation("com.google.dagger:hilt-android:2.55")
    kapt("com.google.dagger:hilt-android-compiler:2.55")

    // ----------------------------------------------------------
    // Testing
    // ----------------------------------------------------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
