@file:Suppress("unused", "KDocMissingDocumentation")
/*
* Copyright (C) 2019 - present Juergen Zimmermann, Hochschule Karlsruhe
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

// https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources

@Suppress("MemberVisibilityCanBePrivate", "Reformat")
object Versions {
    const val kotlin = "1.4.20-M1"
    const val springBoot = "2.4.0-M3"

    object Plugins {
        const val kotlin = Versions.kotlin
        const val allOpen = kotlin
        const val noArg = kotlin
        const val kapt = kotlin

        const val springBoot = Versions.springBoot
        const val testLogger = "2.1.0"
        const val allure = "2.8.1"

        const val vplugin = "3.0.5"
        const val versions = "0.33.0"
        const val detekt = "1.14.1"
        const val dokka = "1.4.10"
        const val jib = "2.5.1"
        const val sweeney = "4.2.0"
        const val owaspDependencyCheck = "6.0.2"
        const val asciidoctorConvert = "3.2.0"
        const val asciidoctorPdf = asciidoctorConvert
        const val asciidoctorDiagram = asciidoctorConvert
        const val jig = "2020.10.1"
        const val jk1DependencyLicenseReport = "1.14"
        //const val jaredsBurrowsLicense = "0.8.80"
        //const val hierynomusLicense = "0.15.0"
    }

    const val javaMin = "15"
    // IntelliJ IDEA 2020.2 unterstuetzt nicht Java 16
    //const val javaMax = "16"
    const val javaMax = "15"

    // fuer Cloud Native Buildpack innerhalb der Task "bootBuildImage"
    //val javaSourceCompatibility = org.gradle.api.JavaVersion.VERSION_15
    val javaSourceCompatibility = org.gradle.api.JavaVersion.VERSION_11

    //val kotlinJvmTarget = org.gradle.api.JavaVersion.VERSION_14.majorVersion
    val kotlinJvmTarget = org.gradle.api.JavaVersion.VERSION_11.majorVersion

    val jibJava = org.gradle.api.JavaVersion.VERSION_14.majorVersion

    const val annotations = "20.1.0"
    const val paranamer = "2.8"
    const val kotlinLogging = "2.0.3"
    const val springSecurityRsa = "1.0.9.RELEASE"

    // -------------------------------------------------------------------------------------------
    // Versionsnummern aus BOM-Dateien ueberschreiben
    // siehe org.springframework.boot:spring-boot-dependencies
    //    https://github.com/spring-projects/spring-boot/blob/master/spring-boot-dependencies/pom.xml
    // -------------------------------------------------------------------------------------------

    //const val assertj = "3.17.2"
    //const val hibernateValidator = "7.0.0.Alpha6"
    const val hibernateValidator = "6.1.6.Final"
    const val jackson = "2.11.3"
    //const val junitJupiterBom = "5.7.0"
    //const val kotlinCoroutines = "1.3.9"
    //const val mongodb = "4.1.0"
    //const val mongoDriverReactivestreams = mongodb
    //const val reactiveStreams = "1.0.3"
    //const val reactorBom = "2020.0.0-RC1"
    // fuer kapt mit spring-context-indexer
    const val springBom = "5.3.0-RC1"
    //const val springDataBom = "2020.0.0-RC1"
    //const val springDataMongoDB = "3.1.0-RC1"
    const val springGraalvmNative = "0.8.1"
    const val springHateoas = "1.2.0-RC1"
    //const val springSecurityBom = "5.4.0"
    //const val thymeleaf = "3.0.11.RELEASE"
    // org.apache.tomcat.embed:tomcat-embed-core   javax/servlet/Servlet -> jakarta/servlet/Servlet
    //const val tomcat = "10.0.0-M7"
    //const val tomcat = "9.0.38"

    const val mockk = "1.10.2"

    const val ktlint = "0.39.0"
    const val ktlintKotlin = kotlin
    const val httpClientKtlint = "4.5.12"
    const val jacocoVersion = "0.8.6"
    const val allure = "2.13.6"
    const val allureCommandline = allure
    const val allureJunit = allure
    const val asciidoctorj = "2.4.1"
    const val asciidoctorjDiagram = "2.0.5"
    const val asciidoctorjPdf = "1.5.3"
}
