import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun prop(name: String, defaultValue: String = ""): String =
    localProperties.getProperty(name, defaultValue).replace("\\", "\\\\").replace("\"", "\\\"")

fun secretProp(name: String): String =
    localProperties.getProperty(name) ?: System.getenv(name).orEmpty()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization") version "2.3.0"
}

android {
    namespace = "com.butch708.projectivy.tvbgsuite"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.butch708.projectivy.tvbgsuite"
        minSdk = 23
        targetSdk = 36
        versionCode = 34
        versionName = "1.3.13"

        buildConfigField("String", "TMDB_API_KEY", "\"${prop("TMDB_API_KEY")}\"")
        buildConfigField("String", "TMDB_API_URL", "\"${prop("TMDB_API_URL", "https://api.themoviedb.org/3/")}\"")
        buildConfigField("String", "TRAKT_CLIENT_ID", "\"${prop("TRAKT_CLIENT_ID")}\"")
        buildConfigField("String", "TRAKT_CLIENT_SECRET", "\"${prop("TRAKT_CLIENT_SECRET")}\"")
        buildConfigField("String", "TRAKT_API_URL", "\"${prop("TRAKT_API_URL", "https://api.trakt.tv/")}\"")
        buildConfigField("String", "TRAKT_REDIRECT_URI", "\"${prop("TRAKT_REDIRECT_URI", "urn:ietf:wg:oauth:2.0:oob")}\"")

    }

    signingConfigs {
        create("release") {
            val storeFilePath = secretProp("TVBGSUITE_RELEASE_STORE_FILE")
            if (storeFilePath.isNotBlank()) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = secretProp("TVBGSUITE_RELEASE_STORE_PASSWORD")
                keyAlias = secretProp("TVBGSUITE_RELEASE_KEY_ALIAS")
                keyPassword = secretProp("TVBGSUITE_RELEASE_KEY_PASSWORD").ifBlank { storePassword }
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.leanback:leanback:1.2.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")
    implementation("com.google.zxing:core:3.5.3")
    implementation(project(":api"))

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
}
