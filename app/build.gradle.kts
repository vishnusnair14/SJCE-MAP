plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.vishnu.sjcemap"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vishnu.sjcemap"
        minSdk = 29
        targetSdk = 34
        versionCode = 13
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)
    implementation(libs.camera.view)
    implementation(libs.gridlayout)
    implementation(libs.legacy.support.v4)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
//    implementation("com.google.firebase:firebase-analytics")

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

//    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    implementation("com.google.android.material:material:1.12.0")

//    implementation("com.github.razir.progressbutton:progressbutton:2.1.0")

    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")

    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.android.gms:play-services-vision:20.1.3")

    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    implementation("com.google.android.gms:play-services-location:21.0.1")

//    implementation("org.greenrobot:eventbus:3.3.0")

}