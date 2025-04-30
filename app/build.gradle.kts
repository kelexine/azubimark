plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "me.kelexine.azubimark"
    compileSdk = 34

    defaultConfig {
        applicationId = "me.kelexine.azubimark"
        minSdk = 21
        targetSdk = 34
        versionCode = 111
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs { // Add this signingConfigs block
        create("release") {
            // It's safer to read from environment variables in CI/CD pipelines,
            // falling back to project properties for local builds.
            storeFile = file(System.getenv("AZUBIMARK_RELEASE_STORE_FILE") ?: project.property("AZUBIMARK_RELEASE_STORE_FILE") as String)
            storePassword = System.getenv("AZUBIMARK_RELEASE_STORE_PASSWORD") ?: project.property("AZUBIMARK_RELEASE_STORE_PASSWORD") as String
            keyAlias = System.getenv("AZUBIMARK_RELEASE_KEY_ALIAS") ?: project.property("AZUBIMARK_RELEASE_KEY_ALIAS") as String
            keyPassword = System.getenv("AZUBIMARK_RELEASE_KEY_PASSWORD") ?: project.property("AZUBIMARK_RELEASE_KEY_PASSWORD") as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Set to true for code shrinking in release
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release") // Link release build type to signing config
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.material3:material3:1.1.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.4.3")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:syntax-highlight:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tasklist:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    //implementation("io.noties:prism4j-bundler:2.0.0")
    kapt("io.noties:prism4j-bundler:2.0.0")
}
// Prevent the duplicate-annotations-java5 module from ever being resolved
configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}
