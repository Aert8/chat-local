plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
}


android {
    namespace = "com.example.modelolocal"
    compileSdk = 37 // O tu versión configurada

    defaultConfig {
        applicationId = "com.example.modelolocal"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags("-O3 -std=c++17")
                arguments(
                    "-DGGML_OPENMP=OFF",
                    "-DGGML_CPU_KLEIDIAI=ON",
                    "-DGGML_VULKAN=ON",
                    "-DGGML_VULKAN_DEBUG=OFF",
                    "-DGGML_VULKAN_CHECK_RESULTS=OFF",
                    "-DGGML_VULKAN_VALIDATE=OFF",
                    "-DGGML_LLAMAFILE=OFF",
                    "-DGGML_NATIVE=OFF",
                    "-DGGML_BUILD_EXAMPLES=OFF",
                    "-DGGML_BUILD_TESTS=OFF",
                    "-DLLAMA_BUILD_EXAMPLES=OFF",
                    "-DLLAMA_BUILD_TESTS=OFF",
                    "-DLLAMA_BUILD_SERVER=OFF"
                )
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // 1. Soporte para ViewModel y Corrutinas
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.activity:activity-ktx:1.9.2")

    // 2. Jetpack Compose & Material 3
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")

    // 3. Compose Material Icons (extended para más iconos)
    implementation("androidx.compose.material:material-icons-extended")

    // 4. ViewModel Compose y Lifecycle Runtime
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // 5. Room Database (persistencia de chats)
    val roomVersion = "2.7.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // 6. Markdown rendering
    implementation("com.mikepenz:multiplatform-markdown-renderer:0.27.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.27.0")
    implementation("com.mikepenz:multiplatform-markdown-renderer-code:0.27.0")

    // Native LaTeX math rendering
    implementation("ru.wertik.orcex:orcex-core:0.5.0")
    implementation("ru.wertik.orcex:orcex-layout:0.5.0")
    implementation("ru.wertik.orcex:orcex-render-android:0.5.0")
    implementation("ru.wertik.orcex:orcex-font-stix2-android:0.5.0")

    // 7. Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
}