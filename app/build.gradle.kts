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
        minSdk = 26
        targetSdk = 34
        versionCode = 122
        versionName = "1.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFile = System.getenv("AZUBIMARK_RELEASE_STORE_FILE") 
                ?: project.findProperty("AZUBIMARK_RELEASE_STORE_FILE") as? String
            val storePassword = System.getenv("AZUBIMARK_RELEASE_STORE_PASSWORD") 
                ?: project.findProperty("AZUBIMARK_RELEASE_STORE_PASSWORD") as? String
            val keyAlias = System.getenv("AZUBIMARK_RELEASE_KEY_ALIAS") 
                ?: project.findProperty("AZUBIMARK_RELEASE_KEY_ALIAS") as? String
            val keyPassword = System.getenv("AZUBIMARK_RELEASE_KEY_PASSWORD") 
                ?: project.findProperty("AZUBIMARK_RELEASE_KEY_PASSWORD") as? String
                
            if (storeFile != null && storePassword != null && keyAlias != null && keyPassword != null) {
                this.storeFile = file(storeFile)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Only apply signing config if it's properly configured
            val releaseSigningConfig = signingConfigs.getByName("release")
            if (releaseSigningConfig.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
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
    
    // APK naming configuration
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val appName = "AzubiMark"
            val version = variant.versionName
            val buildType = variant.buildType.name
            output.outputFileName = "${appName}-${version}-${buildType}.apk"
        }
    }
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.5.8")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.8")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    
    // Markwon for markdown rendering
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:syntax-highlight:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tasklist:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    
    // Additional UI libraries
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // CardView for enhanced layouts
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Networking for GitHub API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    kapt("io.noties:prism4j-bundler:2.0.0")
}

// Prevent the duplicate-annotations-java5 module from ever being resolved
configurations.all {
    exclude(group = "org.jetbrains", module = "annotations-java5")
}
