import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Service account credentials are stored in local.properties (not committed to git).
// Keys: SA_CLIENT_EMAIL, SA_PRIVATE_KEY
val localProps = Properties().also { props ->
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.reader()?.use { props.load(it) }
}

android {
    namespace   = "com.distributor.app"
    compileSdk  = 35

    defaultConfig {
        applicationId = "com.distributor.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 24
        versionName   = "3.3"

        buildConfigField("String", "RELEASE_DATE", "\"2026-06-11\"")

        // Supabase credentials from local.properties (never committed to git).
        val supabaseUrl = localProps.getProperty("SUPABASE_URL", "REPLACE_WITH_SUPABASE_URL")
        val supabaseKey = localProps.getProperty("SUPABASE_KEY", "REPLACE_WITH_SUPABASE_KEY")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Stamp each APK filename with the version name and build number
    applicationVariants.configureEach {
        val variant = this
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl)
                .outputFileName =
                    "Distributor-v${variant.versionName}-${variant.versionCode}-${variant.buildType.name}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose       = true
        buildConfig   = true
    }

    // Exports Room schema JSON for migration auditing
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.activity.compose)

    // Compose BOM pins all Compose library versions together
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.ext)
    debugImplementation(libs.androidx.ui.tooling)

    // Lifecycle + ViewModel
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // QR code generation for QRIS payment
    implementation("com.google.zxing:core:3.5.3")

    // WorkManager (scheduled daily backup)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
