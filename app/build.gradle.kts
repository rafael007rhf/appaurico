plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "br.edu.ifpr.appaurico"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "br.edu.ifpr.appaurico"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = "br.edu.ifpr.appaurico"
        buildConfigField("String", "API_URL", "\"" + providers.gradleProperty("auricoApiUrl").getOrElse("") + "\"")
        buildConfigField("String", "OIDC_ISSUER", "\"" + providers.gradleProperty("auricoOidcIssuer").getOrElse("") + "\"")
        buildConfigField("String", "OIDC_CLIENT_ID", "\"aurico-android\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("patient") { dimension = "environment"; buildConfigField("boolean", "DEMO_MODE", "false") }
        create("demo") { dimension = "environment"; applicationIdSuffix = ".demo"; buildConfigField("boolean", "DEMO_MODE", "true") }
    }
    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navegacao
    implementation(libs.androidx.navigation.compose)

    // Injecao de dependencia (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Persistencia local (Room)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.10.5")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("net.openid:appauth:0.11.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    androidTestImplementation("androidx.room:room-testing:2.7.2")
}

android { testOptions { unitTests.isIncludeAndroidResources = true } }
dependencies { testImplementation("org.robolectric:robolectric:4.17") }
