import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
    id("com.mikepenz.aboutlibraries.plugin")
    id("androidx.room")
}

android {
    namespace = "ua.acclorite.book_story"
    compileSdk = 36

    fun getGitVersionName(): String {
        val prop = project.findProperty("versionName") as String?
            ?: project.findProperty("appVersionName") as String?
            ?: System.getenv("VERSION_NAME")
            ?: System.getenv("APP_VERSION_NAME")
        if (!prop.isNullOrBlank()) {
            return prop.trim().removePrefix("v")
        }
        return try {
            val tag = providers.exec {
                commandLine("git", "describe", "--tags", "--abbrev=0")
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
            if (tag.isNotEmpty() && !tag.contains("fatal")) {
                tag.removePrefix("v")
            } else {
                "1.8.0"
            }
        } catch (e: Exception) {
            "1.8.0"
        }
    }

    fun getGitVersionCode(): Int {
        val prop = project.findProperty("versionCode") as String?
            ?: project.findProperty("appVersionCode") as String?
            ?: System.getenv("VERSION_CODE")
            ?: System.getenv("APP_VERSION_CODE")
        if (!prop.isNullOrBlank()) {
            return prop.trim().toIntOrNull() ?: 14
        }
        return try {
            val countStr = providers.exec {
                commandLine("git", "rev-list", "--count", "HEAD")
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
            countStr.toIntOrNull() ?: 14
        } catch (e: Exception) {
            14
        }
    }

    // Default configuration
    defaultConfig {
        applicationId = "ua.acclorite.book_story"
        minSdk = 26
        targetSdk = 36
        versionCode = getGitVersionCode()
        versionName = getGitVersionName()

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "BACKEND_BASE_URL", "\"http://172.20.2.76:8082/data-api\"")
    }

    fun formatServerUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "http://$trimmed"
        } else {
            trimmed
        }
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev-macos") {
            dimension = "env"
            val devUrl = project.findProperty("DEV_SERVER_URL") as String?
                ?: System.getenv("DEV_SERVER_URL")
                ?: "http://172.20.2.76:8082/data-api"
            buildConfigField("String", "BACKEND_BASE_URL", "\"${formatServerUrl(devUrl)}\"")
        }

        create("dev-ubuntu") {
            dimension = "env"
            val devUbuntuUrl = project.findProperty("DEV_UBUNTU_SERVER_URL") as String?
                ?: project.findProperty("DEV_UBUNTU_URL") as String?
                ?: System.getenv("DEV_UBUNTU_SERVER_URL")
                ?: System.getenv("DEV_UBUNTU_URL")
                ?: "http://192.168.1.21:8083/data-api"
            buildConfigField("String", "BACKEND_BASE_URL", "\"${formatServerUrl(devUbuntuUrl)}\"")
        }

        create("prod") {
            dimension = "env"
            val prodUrl = project.findProperty("PROD_SERVER_URL") as String?
                ?: System.getenv("PROD_SERVER_URL")
                ?: "http://172.20.2.76:8082/data-api"
            buildConfigField("String", "BACKEND_BASE_URL", "\"${formatServerUrl(prodUrl)}\"")
        }
    }

    // Build types configuration
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")

            proguardFiles("proguard-rules.pro")
        }

        create("release-debug") {
            initWith(getByName("release"))
            applicationIdSuffix = ".release.debug"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Kotlin configuration
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Room configuration
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            if (output != null) {
                val flavor = variant.flavorName.ifEmpty { "standard" }
                output.outputFileName = "bookstory-v${variant.versionName}-${flavor}-${variant.buildType.name}.apk"
            }
        }
    }
}

// About Libraries configuration
aboutLibraries {
    registerAndroidTasks = false
    prettyPrint = true

    filterVariants = arrayOf(
        "dev-macosDebug", "dev-macosRelease", "dev-macosRelease-debug",
        "dev-ubuntuDebug", "dev-ubuntuRelease", "dev-ubuntuRelease-debug",
        "prodDebug", "prodRelease", "prodRelease-debug"
    )
    excludeFields = arrayOf("generated", "funding", "description")
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.activity:activity-compose:1.11.0")

    // Compose BOM libraries
    // Compose BOM was eliminated - it is recognized as Closed Source in AboutLibraries..
    // although it is not.
    implementation("androidx.compose.foundation:foundation:1.9.3")
    implementation("androidx.compose.animation:animation:1.9.3")
    implementation("androidx.compose.animation:animation-android:1.9.3")
    implementation("androidx.compose.foundation:foundation-layout:1.9.3")
    implementation("androidx.compose.ui:ui:1.9.3")
    implementation("androidx.compose.ui:ui-graphics:1.9.3")
    implementation("androidx.compose.ui:ui-android:1.9.3")
    implementation("androidx.compose.material3:material3:1.5.0-alpha06")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.material:material:1.9.3")

    // All dependencies
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.36.0")

    // Dagger - Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")
    implementation("com.google.dagger:hilt-compiler:2.57.2")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    // Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:2.7.2")

    // Datastore (Settings)
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // SAF
    implementation("com.anggrayudi:storage:2.2.0")

    // PDF parser
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // EPUB parser
    implementation("org.jsoup:jsoup:1.21.2")

    // Language Switcher
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.appcompat:appcompat-resources:1.7.1")

    // Coil for loading images
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Open source libraries
    implementation("com.mikepenz:aboutlibraries-core:11.4.0")
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.4.0")

    // Drag & Drop
    implementation("sh.calvin.reorderable:reorderable:2.4.3")

    // Scrollbar
    implementation("com.github.nanihadesuka:LazyColumnScrollbar:2.2.0")

    // Markdown
    implementation("org.commonmark:commonmark:0.26.0")

    // Json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // Math & LaTeX rendering
    implementation("ru.noties:jlatexmath-android:0.2.0")
}