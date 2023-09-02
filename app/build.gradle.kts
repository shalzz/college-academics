/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import com.android.build.api.dsl.Packaging
import com.lordcodes.turtle.shellRun

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.devtools.ksp")
    id("com.bugsnag.android.gradle")
    id("com.google.android.gms.oss-licenses-plugin")
    id("com.github.triplet.play") version "3.8.4"
    id("com.google.gms.google-services") apply false
}

bugsnag {
    overwrite.set(true)
    retryCount.set(3)
}

play {
    defaultToAppBundles.set(true)
    track.set("beta")
    serviceAccountCredentials.set(file("../play-service-account-key.json"))
}

// query git for the SHA, Tag and commit count. Use these to automate versioning.

val gitSha = shellRun("git", listOf("rev-parse", "--short", "HEAD"), project.rootDir).trim()
val gitTag = shellRun("git", listOf("describe", "--tags"), project.rootDir).trim()
val gitCommitCount = 2007050 + Integer.parseInt(
        shellRun("git", listOf("rev-list", "--count", "HEAD", "--no-merges"),
                project.rootDir).trim()
        )

android {
    compileSdkVersion(33)

    defaultConfig {
        applicationId = "com.shalzz.attendance"
        minSdkVersion(21)
        targetSdkVersion(33)
        versionCode = gitCommitCount
        versionName = gitTag
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndkVersion = "22.1.7171670"
        resourceConfigurations += setOf("en")

        resValue("string", "app_version", versionName!!)
        javaCompileOptions {
            annotationProcessorOptions {
                arguments.plusAssign(mapOf(
                        "room.schemaLocation" to "$projectDir/schemas",
                        "room.incremental" to "true"
                ))
            }
        }
    }

    signingConfigs {
        // You must set up an environment var before release signing
        // Run: export APP_KEY={password}
        create("release") {
            storeFile = file("keystore/upload.keystore")
            keyAlias = "key0"
            storePassword = System.getenv("APP_KEY")
            keyPassword = System.getenv("APP_KEY")
        }

        getByName("debug") {
            storeFile = file("keystore/debug.keystore")
            keyAlias = "androiddebugkey"
            storePassword = "android"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            ext.set("enableBugsnag", false)
            isShrinkResources = false
            isMinifyEnabled = false
            resValue("string", "app_id", defaultConfig.applicationId!!+ ".debug")
            resValue("string", "app_name", "College Academics (debug)")
            resValue("string", "contentAuthority", defaultConfig.applicationId + ".debug.provider")
            resValue("string", "account_type", "com.shalzz.debug")
            resValue("string", "zoho_app_id", "0cf0e6f11763c00d387ee247ab64aed483f3768859c3ef35")
            buildConfigField("String", "ACCOUNT_TYPE", "\"com.shalzz.debug\"")
            multiDexKeepProguard = file("./proguard/proguard-multidex-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isShrinkResources = true
            isMinifyEnabled = true
            resValue("string", "app_id", defaultConfig.applicationId!!)
            resValue("string", "app_name", "College Academics")
            resValue("string", "contentAuthority", defaultConfig.applicationId + ".provider")
            resValue("string", "account_type", "com.shalzz")
            resValue("string", "zoho_app_id", "0cf0e6f11763c00d387ee247ab64aed474712349653bbd5b")
            buildConfigField("String", "ACCOUNT_TYPE", "\"com.shalzz\"")
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("./proguard")
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        //setCoreLibraryDesugaringEnabled(true)

        // Sets Java compatibility to Java 17
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            java.srcDirs("src/commonTest/java")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java")
            assets.srcDirs("$projectDir/schemas")
        }
    }

    // Needed because of this https://github.com/robolectric/robolectric/issues/{1399, 3826}
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
    fun Packaging.() {
        resources {
            excludes += setOf(
                "META-INF/rxjava.properties",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE"
            )
        }
    }

    //Needed because of this https://github.com/ReactiveX/RxJava/issues/4445

    buildFeatures {
        viewBinding = true
    }
    lint {
        textOutput = file("stdout")
        textReport = true
    }
    namespace = "com.shalzz.attendance"

}

kapt {
    useBuildCache = true
}

dependencies {
    val DAGGER_VERSION = "2.47"
    val ESPRESSO_VERSION = "3.1.0"
    val RETROFIT_VERSION = "2.8.1"
    val MOSHI_VERSION = "1.9.3"
    val ROOM_VERSION = "2.5.2"
    val NAV_VERSION = "2.2.0"
    val BILLING_VERSION = "4.0.0"

    // TODO: re-evaluate when RxJava is completely replaced with kotlin co-routines
    implementation("androidx.multidex:multidex:2.0.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.5")
//    implementation(platform(kotlin("bom")))

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.navigation:navigation-fragment-ktx:$NAV_VERSION")
    implementation("androidx.navigation:navigation-ui-ktx:$NAV_VERSION")
    implementation("androidx.drawerlayout:drawerlayout:1.1.0-rc01")

    implementation("com.google.firebase:firebase-core:17.3.0")
    implementation("com.google.firebase:firebase-analytics:17.3.0")
//    implementation("com.github.shalzz:helpstack-android:1.4.6")
//    implementation("com.github.shalzz:helpstack:1.4.1-debug")

    implementation("com.google.android.gms:play-services-oss-licenses:17.0.1")
    implementation("com.google.android.material:material:1.1.0")

    implementation("androidx.core:core:1.10.1")
    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.fragment:fragment:1.2.4")
    implementation("androidx.fragment:fragment-ktx:1.2.4")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.preference:preference:1.1.1")
    implementation("androidx.annotation:annotation:1.6.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.constraintlayout:constraintlayout-solver:1.1.3")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("androidx.vectordrawable:vectordrawable-animated:1.1.0")
    implementation("androidx.browser:browser:1.3.0")
    implementation ("com.google.androidbrowserhelper:androidbrowserhelper:2.2.2")

    implementation("com.android.billingclient:billing:$BILLING_VERSION")
    implementation("com.android.billingclient:billing-ktx:$BILLING_VERSION")

    val daggerCompiler = "com.google.dagger:dagger-compiler:$DAGGER_VERSION"
    implementation("com.google.dagger:dagger:$DAGGER_VERSION")
    kapt(daggerCompiler)
    compileOnly("javax.annotation:jsr250-api:1.0")
    testAnnotationProcessor(daggerCompiler)
    androidTestAnnotationProcessor(daggerCompiler)

    implementation("com.jakewharton.timber:timber:4.7.1")

    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.19")

    implementation("com.squareup.retrofit2:retrofit:$RETROFIT_VERSION")
    implementation("com.squareup.retrofit2:converter-moshi:$RETROFIT_VERSION")
    implementation("com.jakewharton.retrofit:retrofit2-rxjava2-adapter:1.0.0")

    implementation("com.squareup.moshi:moshi:$MOSHI_VERSION")
    implementation("com.squareup.moshi:moshi-adapters:$MOSHI_VERSION")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:$MOSHI_VERSION")
//    implementation("com.squareup.moshi:moshi-kotlin:1.9.1")

    implementation("com.malinskiy:materialicons:1.0.2")
    implementation("com.github.amlcurran.showcaseview:library:5.4.3")
    implementation("com.github.afollestad.material-dialogs:core:0.8.5.5")

    // Room Persistence
    implementation("androidx.room:room-runtime:$ROOM_VERSION")
    ksp("androidx.room:room-compiler:$ROOM_VERSION")
    implementation("androidx.room:room-ktx:$ROOM_VERSION")
    implementation("androidx.room:room-rxjava2:$ROOM_VERSION")
    testImplementation("androidx.room:room-testing:$ROOM_VERSION")
    androidTestImplementation("androidx.room:room-testing:$ROOM_VERSION")

    //noinspection GradleDynamicVersion
    implementation("com.bugsnag:bugsnag-android:5.+")

    val jUnit = "androidx.test.ext:junit:1.1.1"
    val truth = "androidx.test.ext:truth:1.0.0"
    val mockito = "org.mockito:mockito-core:3.3.3"

    // common shared test files as a library module
    testImplementation(project(path = ":commonTest"))
    androidTestImplementation(project(path = ":commonTest"))

    // Unit tests dependencies
    testImplementation(jUnit)
    testImplementation(truth)
    testImplementation(mockito)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.5.1")
    testImplementation("org.robolectric:shadows-multidex:4.5.1")

    // Instrumentation test dependencies
    androidTestImplementation(jUnit)
    androidTestImplementation(truth)
    androidTestImplementation(mockito)
    androidTestImplementation("androidx.annotation:annotation:1.6.0")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")


    // Espresso dependencies
//        androidTestImplementation("androidx.test.espresso:espresso-core:$ESPRESSO_VERSION"
//        androidTestImplementation("androidx.test.espresso:espresso-contrib:$ESPRESSO_VERSION"
//        androidTestImplementation("androidx.test.espresso:espresso-intents:$ESPRESSO_VERSION"
//        androidTestImplementation("androidx.test.espresso:espresso-accessibility:$ESPRESSO_VERSION"
//        androidTestImplementation("androidx.test.espresso:espresso-web:$ESPRESSO_VERSION"
//        androidTestImplementation("androidx.test.espresso.idling:idling-concurrent:$ESPRESSO_VERSION"
//
//        androidTestImplementation("androidx.test.espresso:espresso-contrib:$ESPRESSO_VERSION"
//        androidTestImplementation("androidx.test.espresso:espresso-core:$ESPRESSO_VERSION"

    configurations.all {
        resolutionStrategy.force("com.google.code.gson:gson:2.8.0")
        resolutionStrategy.force("org.checkerframework:checker-compat-qual:2.5.3")
    }
}

// Add this at the bottom of your file to actually apply the plugin
apply(mapOf("plugin" to "com.google.gms.google-services"))
