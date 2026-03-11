@file:Suppress("UnstableApiUsage")

import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.lspandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lspandroid"
        minSdk = 26
        targetSdk = 35

        versionCode = 12
        versionName = "2.1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Note: ndk.abiFilters removed to avoid conflict with splits.abi
        // ABIs are configured in splits block below

        externalNativeBuild {
            cmake {
                arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DLSP_PLUGINS_ENABLED=ON",
                    "-DLSP_JACK_ENABLED=OFF",
                    "-DLSP_ALSA_ENABLED=OFF",
                    "-DLSP_OPENGL_ENABLED=ON",
                    "-DLSP_X11_ENABLED=OFF",
                    "-DLSP_PROFILING=OFF"
                )
                cppFlags += listOf(
                    "-std=c++17",
                    "-O3",
                    "-DNDEBUG",
                    "-fvisibility=hidden"
                )
            }
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Configure APK splits per architecture with version code offsets
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    // Version code strategy per architecture
    // arm64-v8a: base version code (primary target)
    // x86_64: base + 1 (emulator)
    // armeabi-v7a: base + 2 (legacy)
    androidComponents {
        onVariants { variant ->
            val baseVersionCode = 12  // Match defaultConfig versionCode
            variant.outputs.forEach { output ->
                val abiVersionCode = when (output.filters.find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI }?.identifier) {
                    "arm64-v8a" -> 0      // Base version code
                    "x86_64" -> 1         // Base + 1
                    "armeabi-v7a" -> 2    // Base + 2
                    else -> 0
                }
                output.versionCode.set(baseVersionCode * 10 + abiVersionCode)
            }
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                storeFile = file(keystoreProperties["keystore"] as String)
                storePassword = keystoreProperties["keystore.password"] as String
                keyAlias = keystoreProperties["key.alias"] as String
                keyPassword = keystoreProperties["key.password"] as String
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            externalNativeBuild {
                cmake {
                    arguments += listOf(
                        "-DCMAKE_BUILD_TYPE=Debug",
                        "-DLSP_PROFILING=ON"
                    )
                }
            }
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            signingConfig = signingConfigs.getByName("release")
        }
        
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles("benchmark-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }



    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/versions/9/previous-compilation-data.bin"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    
    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    
    // Material Design
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.3")
    
    // Audio & Media
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    
    // Storage & Persistence
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Serialization & JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    
    // Dependency Injection
    implementation("io.insert-koin:koin-android:3.5.6")
    implementation("io.insert-koin:koin-androidx-compose:3.5.6")
    implementation("io.insert-koin:koin-androidx-workmanager:3.5.6")
    
    // Work Manager
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // File Management
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    
    // Charts & Visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Audio Analysis
    implementation("org.apache.commons:commons-math3:3.6.1")
    
    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("io.insert-koin:koin-test:3.5.6")
    testImplementation("io.insert-koin:koin-test-junit4:3.5.6")
    
    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.work:work-testing:2.9.1")
    
    // Debug Tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24")
    }
}
