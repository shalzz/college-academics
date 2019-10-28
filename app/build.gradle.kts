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
import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("com.bugsnag.android.gradle")
    id("com.google.gms.oss.licenses.plugin")
    id("com.github.triplet.play") version "2.2.1"
    id("com.google.gms.google-services") apply false
}

bugsnag {
    autoProguardConfig = false
    overwrite = true
    retryCount = 3
}

play {
    serviceAccountCredentials = file("../play-service-account-key.json")
    defaultToAppBundles = true
    track = "beta"
}

// query git for the SHA, Tag and commit count. Use these to automate versioning.
val gitSha = "asdf"
val gitTag = "123"
val gitCommitCount = 12333
//val gitSha = "git rev-parse --short HEAD".execute([], project.rootDir).text.trim()
//val gitTag = "git describe --tags".execute([], project.rootDir).text.trim()
//val gitCommitCount = 2007050 +
//        Integer.parseInt("git rev-list --count HEAD --no-merges".execute([], project.rootDir).text.trim())

android {
    compileSdkVersion(28)

    defaultConfig.apply {
        applicationId = "com.shalzz.attendance"
        minSdkVersion(16)
        targetSdkVersion(28)
        multiDexEnabled = true
        versionCode = gitCommitCount
        versionName = gitTag

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        resConfig("en")
        resValue("string", "app_version", versionName!!)

        // Needed because of this https://github.com/robolectric/robolectric/issues/{1399, 3826}
        testOptions.unitTests.isIncludeAndroidResources = true
        testOptions.unitTests.isReturnDefaultValues = true

        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")
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
            buildConfigField("String", "ACCOUNT_TYPE", "\"com.shalzz\"")
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("./proguard")
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        val commonTestDir = "src/commonTest/java"

        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            java.srcDirs(commonTestDir)
        }
        getByName("androidTest") {
            java.srcDirs(commonTestDir)
            assets.srcDirs("$projectDir/schemas")
        }
    }

    //Needed because of this https://github.com/square/okio/issues/58
    lintOptions {
        disable("InvalidPackage")
    }

    //Needed because of this https://github.com/ReactiveX/RxJava/issues/4445
    packagingOptions {
        exclude("META-INF/rxjava.properties")
    }

    dependencies {
        val DAGGER_VERSION = "2.19"
        val ESPRESSO_VERSION = "3.1.0"
        val RETROFIT_VERSION = "2.5.0"
        val MOSHI_VERSION = "1.8.0"
        val ROOM_VERSION = "2.0.0"
        val NAV_VERSION = "1.0.0"

        // TODO: re-evaluate when RxJava is completely replaced with kotlin co-routines
        implementation("com.android.support:multidex:1.0.3")

        implementation(kotlin("stdlib-jdk7", KotlinCompilerVersion.VERSION))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.0.0")

        implementation("androidx.core:core-ktx:1.0.2")
        implementation("android.arch.navigation:navigation-fragment-ktx:$NAV_VERSION")
        implementation("android.arch.navigation:navigation-ui-ktx:$NAV_VERSION")

        implementation("com.google.firebase:firebase-core:16.0.9")
        implementation("com.google.firebase:firebase-analytics:16.5.0")
        implementation("com.google.firebase:firebase-inappmessaging-display:17.1.1")

        implementation("com.google.android.gms:play-services-oss-licenses:16.0.2")
        implementation("com.google.android.material:material:1.0.0")

        implementation("androidx.core:core:1.0.2")
        implementation("androidx.appcompat:appcompat:1.0.2")
        implementation("androidx.fragment:fragment:1.0.0")
        implementation("androidx.recyclerview:recyclerview:1.0.0")
        implementation("androidx.preference:preference:1.0.0")
        implementation("androidx.annotation:annotation:1.0.2")
        implementation("androidx.constraintlayout:constraintlayout:1.1.3")
        implementation("androidx.constraintlayout:constraintlayout-solver:1.1.3")

        implementation("com.android.billingclient:billing:1.0")

        val daggerCompiler = "com.google.dagger:dagger-compiler:$DAGGER_VERSION"
        implementation("com.google.dagger:dagger:$DAGGER_VERSION")
        kapt(daggerCompiler)
        compileOnly("javax.annotation:jsr250-api:1.0")
        testAnnotationProcessor(daggerCompiler)
        androidTestAnnotationProcessor(daggerCompiler)

        implementation("com.jakewharton.timber:timber:4.7.1")

        implementation("io.reactivex.rxjava2:rxandroid:2.0.2")
        implementation("io.reactivex.rxjava2:rxjava:2.1.14")

        implementation("com.squareup.retrofit2:retrofit:$RETROFIT_VERSION")
        implementation("com.squareup.retrofit2:converter-moshi:$RETROFIT_VERSION")
        implementation("com.jakewharton.retrofit:retrofit2-rxjava2-adapter:1.0.0")

        implementation("com.squareup.moshi:moshi:$MOSHI_VERSION")
        implementation("com.squareup.moshi:moshi-adapters:$MOSHI_VERSION")
        kapt("com.squareup.moshi:moshi-kotlin-codegen:$MOSHI_VERSION")

        implementation("com.malinskiy:materialicons:1.0.2")
        implementation("com.github.amlcurran.showcaseview:library:5.4.0")
        implementation("com.github.afollestad.material-dialogs:core:0.8.5.5")

        //noinspection GradleDynamicVersion
        implementation("com.bugsnag:bugsnag-android:4.+")

        // Room Persistence
        implementation("androidx.room:room-runtime:$ROOM_VERSION")
        kapt("androidx.room:room-compiler:$ROOM_VERSION")
        implementation("androidx.room:room-rxjava2:$ROOM_VERSION")
        testImplementation("androidx.room:room-testing:$ROOM_VERSION")
        androidTestImplementation("androidx.room:room-testing:$ROOM_VERSION")

        val jUnit = "androidx.test.ext:junit:1.0.0"
        val truth = "androidx.test.ext:truth:1.0.0"
        val mockito = "org.mockito:mockito-core:2.8.9"

        // Instrumentation test dependencies
        androidTestImplementation(mockito)
        androidTestImplementation("com.google.code.findbugs:jsr305:3.0.2")
        androidTestImplementation("androidx.annotation:annotation:1.0.2")

        // Core library
        androidTestImplementation("androidx.test:core:1.1.0")

        // AndroidJUnitRunner and JUnit Rules
        androidTestImplementation("androidx.test:runner:1.1.1")
        androidTestImplementation("androidx.test:rules:1.1.1")

        // Assertions
        androidTestImplementation(jUnit)
        androidTestImplementation(truth)
        androidTestImplementation("com.google.truth:truth:0.42")

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

        // Unit tests dependencies
        testImplementation(jUnit)
        testImplementation(truth)
        testImplementation(mockito)
        testImplementation("androidx.test:core:1.1.0")
        testImplementation("org.robolectric:robolectric:4.0")
        testImplementation("org.robolectric:shadows-multidex:4.0")
    }
}