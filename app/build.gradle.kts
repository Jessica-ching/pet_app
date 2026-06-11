plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.pet_app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.pet_app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gridlayout)
    implementation(libs.threetenabp)
    implementation("androidx.cardview:cardview:1.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1") {
        exclude(group = "com.android.support")
    }
    // 網路連線與 JSON 解析套件
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.sourceforge.jtds:jtds:1.3.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.microsoft.sqlserver:mssql-jdbc:12.8.1.jre11")
}