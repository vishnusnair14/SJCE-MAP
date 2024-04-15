plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.vishnu.sjce_map"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vishnu.sjce_map"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation(libs.play.services.maps)
    implementation(libs.firebase.firestore)
    implementation(libs.activity)
    implementation(libs.camera.view)
    implementation(libs.gridlayout)
    implementation(libs.legacy.support.v4)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform(libs.firebase.bom))
    implementation(dependencyNotation = "com.google.firebase:firebase-analytics")

    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")

    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.android.gms:play-services-vision:20.1.3")

    implementation("com.squareup.picasso:picasso:2.71828")
    implementation ("de.hdodenhof:circleimageview:3.1.0")

    implementation("com.google.android.gms:play-services-location:18.0.0")
    implementation("com.google.android.gms:play-services-maps:17.0.1")
}