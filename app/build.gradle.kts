plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.aromaatlas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aromaatlas"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // AndroidX UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity)
    implementation(libs.androidx.fragment)
    implementation (libs.androidx.recyclerview.v132)

    // Firebase BOM (must come first)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database)
    implementation (libs.firebase.ui.firestore)
    implementation (libs.google.firebase.auth)
    implementation (libs.google.firebase.firestore)


    // Google Play Services (choose one maps version only)
    implementation(libs.play.services.maps) // <- keep just one
    implementation(libs.play.services.location)
    implementation(libs.play.services.auth)

    // Media & utils
    implementation(libs.glide)
    implementation (libs.material.v1110)
    implementation(libs.androidx.recyclerview)
    annotationProcessor(libs.compiler)
    implementation (libs.androidx.cardview)
    implementation (libs.mpandroidchart)
    implementation (libs.gson)


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
