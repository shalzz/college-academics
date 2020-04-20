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

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val kotlinVersion = "1.3.71"
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven("https://kotlin.bintray.com/kotlinx")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("com.android.tools.build:gradle:4.0.0-beta04")
        classpath("com.google.gms:google-services:4.3.2")

        classpath("com.google.android.gms:oss-licenses-plugin:0.10.2")

        //noinspection GradleDynamicVersion
        classpath("com.bugsnag:bugsnag-android-gradle-plugin:4.+")
        classpath("com.lordcodes.turtle:turtle:0.2.0")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}