import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright (C) 2016 - present Juergen Zimmermann, Hochschule Karlsruhe
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

//  Aufrufe
//  1) Microservice uebersetzen und starten
//        .\gradlew bootRun [--args='--debug'] [--continuous]
//        .\gradlew compileKotlin
//        .\gradlew compileTestKotlin
//
//  2) Microservice als selbstausfuehrendes JAR erstellen und ausfuehren
//        .\gradlew bootJar
//        java -jar build/libs/....jar --spring.profiles.active=dev
//        .\gradlew bootBuildImage
//              erfordert die lokale Windows-Gruppe docker-users
//              docker run --publish 8080:8080 --env TZ=Europe/Berlin --name kunde --rm kunde:1.0-GraalVM
//              curl --silent http://localhost:8080
//
//        .\gradlew jibDockerBuild
//
//  3) Tests und QS
//        .\gradlew test [--rerun-tasks] [--fail-fast]
//              EINMALIG>>   .\gradlew downloadAllure
//        .\gradlew allureServe
//        .\gradlew ktlint detekt
//
//  4) Sicherheitsueberpruefung durch OWASP Dependency Check
//        .\gradlew dependencyCheckAnalyze --info
//
//  5) "Dependencies Updates"
//        .\gradlew versions
//        .\gradlew dependencyUpdates
//
//  6) API-Dokumentation erstellen (funktioniert NICHT mit proxy.hs-karlruhe.de, sondern nur mit proxyads)
//        .\gradlew dokkaHtml
//
//  7) Entwicklerhandbuch in "Software Engineering" erstellen
//        .\gradlew asciidoctor asciidoctorPdf
//        .\gradlew jigReports
//
//  8) Projektreport erstellen
//        .\gradlew projectReport
//        .\gradlew dependencyInsight --dependency spring-security-rsa
//        .\gradlew dependencies
//        .\gradlew dependencies --configuration runtimeClasspath
//        .\gradlew buildEnvironment
//        .\gradlew htmlDependencyReport
//
//  9) Report ueber die Lizenzen der eingesetzten Fremdsoftware
//        .\gradlew generateLicenseReport
//
//  10) Daemon stoppen
//        .\gradlew --stop
//
//  11) Verfuegbare Tasks auflisten
//        .\gradlew tasks
//
//  12) Initialisierung des Gradle Wrappers in der richtigen Version
//      (dazu ist ggf. eine Internetverbindung erforderlich)
//        gradle wrapper --gradle-version=6.7-rc-3 --distribution-type=all

// https://github.com/gradle/kotlin-dsl/tree/master/samples
// https://docs.gradle.org/current/userguide/kotlin_dsl.html
// https://docs.gradle.org/current/userguide/task_configuration_avoidance.html
// https://guides.gradle.org/migrating-build-logic-from-groovy-to-kotlin

plugins {
    //`kotlin-dsl`
    java
    idea
    jacoco
    `project-report`

    // org.jetbrains.kotlin:kotlin-gradle-plugin
    kotlin("jvm") version Versions.Plugins.kotlin
    // fuer Spring Beans: org.jetbrains.kotlin:kotlin-allopen
    kotlin("plugin.allopen") version Versions.Plugins.allOpen
    // fuer @ConfigurationProperties mit "data class": org.jetbrains.kotlin:kotlin-noarg
    kotlin("plugin.noarg") version Versions.Plugins.noArg
    // Voraussetzung fuer spring-context-indexer: org.jetbrains.kotlin:kotlin-annotation-processing-gradle
    kotlin("kapt") version Versions.Plugins.kapt

    id("org.springframework.boot") version Versions.Plugins.springBoot
    id("com.adarshr.test-logger") version Versions.Plugins.testLogger

    // https://github.com/allure-framework/allure-gradle
    // https://docs.qameta.io/allure/#_gradle_2
    // https://github.com/allure-framework/allure-gradle/issues/52
    id("io.qameta.allure") version Versions.Plugins.allure

    // https://github.com/arturbosch/detekt
    id("io.gitlab.arturbosch.detekt") version Versions.Plugins.detekt

    // https://github.com/Kotlin/dokka
    // FIXME https://github.com/Kotlin/dokka/issues/1255
    id("org.jetbrains.dokka") version Versions.Plugins.dokka

    // https://github.com/nwillc/vplugin
    id("com.github.nwillc.vplugin") version Versions.Plugins.vplugin

    // https://github.com/ben-manes/gradle-versions-plugin
    id("com.github.ben-manes.versions") version Versions.Plugins.versions

    // https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin
    // Alternative: https://buildpacks.io siehe https://spring.io/blog/2020/01/23/spring-boot-2-3-0-m1-is-now-available
    id("com.google.cloud.tools.jib") version Versions.Plugins.jib

    id("com.fizzpod.sweeney") version Versions.Plugins.sweeney

    // https://github.com/jeremylong/dependency-check-gradle
    id("org.owasp.dependencycheck") version Versions.Plugins.owaspDependencyCheck

    // https://github.com/asciidoctor/asciidoctor-gradle-plugin
    id("org.asciidoctor.jvm.convert") version Versions.Plugins.asciidoctorConvert
    id("org.asciidoctor.jvm.pdf") version Versions.Plugins.asciidoctorPdf
    // Leanpub als Alternative zu PDF: https://github.com/asciidoctor/asciidoctor-leanpub-converter

    // https://github.com/dddjava/jig
    id("org.dddjava.jig-gradle-plugin") version Versions.Plugins.jig

    // https://github.com/intergamma/gradle-zap
    //id("net.intergamma.gradle.gradle-zap-plugin") version Versions.Plugins.zap

    // https://github.com/jk1/Gradle-License-Report
    // FIXME https://github.com/jk1/Gradle-License-Report/issues/176
    id("com.github.jk1.dependency-license-report") version Versions.Plugins.jk1DependencyLicenseReport

    // https://github.com/jaredsburrows/gradle-license-plugin
    // erfordert Repository fuer Android
    //id("com.jaredsburrows.license") version Versions.Plugins.jaredsBurrowsLicense

    // https://github.com/hierynomus/license-gradle-plugin
    //id("com.github.hierynomus.license") version Versions.Plugins.hierynomusLicense
}

defaultTasks = mutableListOf("compileTestKotlin")
group = "com.acme"
version = "1.0"

// https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/html/#build-image-example-builder-configuration
// https://github.com/paketo-buildpacks/bellsoft-liberica#configuration
// fuer Cloud Native Buildpack innerhalb der Task "bootBuildImage"
java.sourceCompatibility = Versions.javaSourceCompatibility

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    //maven("https://dl.bintray.com/kotlin/kotlin-dev") {
    //    mavenContent { snapshotsOnly() }
    //}
    // https://github.com/spring-projects/spring-framework/wiki/Spring-repository-FAQ
    // https://github.com/spring-projects/spring-framework/wiki/Release-Process
    maven("https://repo.spring.io/milestone") {
        mavenContent { releasesOnly() }
    }
    maven("https://repo.spring.io/release") {
        mavenContent { releasesOnly() }
    }

    jcenter()

    // Snapshots von Spring Framework, Spring Boot, Spring Data und Spring Security
    //maven("https://repo.spring.io/snapshot") {
    //    mavenContent { snapshotsOnly() }
    //}
    // Snapshots von JaCoCo
    //maven("https://oss.sonatype.org/content/repositories/snapshots") {
    //    mavenContent { snapshotsOnly() }
    //}
}

/** Konfiguration fuer ktlint */
val ktlintCfg: Configuration by configurations.creating

// https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_separation
dependencies {
    // https://docs.gradle.org/current/userguide/managing_transitive_dependencies.html#sec:bom_import
    // https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-bom/pom.xml
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:${Versions.kotlin}"))
    //implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:${Versions.kotlinCoroutines}"))
    //implementation(platform("org.junit:junit-bom:${Versions.junitJupiterBom}"))
    //implementation(platform("io.projectreactor:reactor-bom:${Versions.reactorBom}"))
    implementation(platform("com.fasterxml.jackson:jackson-bom:${Versions.jackson}"))
    //implementation(platform("org.springframework:spring-framework-bom:${Versions.springBom}"))
    //implementation(platform("org.springframework.data:spring-data-bom:${Versions.springDataBom}"))
    //implementation(platform("org.springframework.security:spring-security-bom:${Versions.springSecurityBom}"))
    implementation(platform("org.springframework.boot:spring-boot-starter-parent:${Versions.springBoot}"))

    // https://github.com/spring-projects-experimental/spring-graalvm-native
    // https://github.com/spring-projects-experimental/spring-graalvm-native/blob/master/spring-graalvm-native-docs/src/main/asciidoc/support.adoc
    // https://repo.spring.io/milestone/org/springframework/experimental/spring-graalvm-native-docs/0.8.0/spring-graalvm-native-docs-0.8.0.zip!/reference/index.html
    // https://github.com/oracle/graal/issues?q=is%3Aissue+is%3Aopen+label%3Aspring
    // Project Leyden: https://mail.openjdk.java.net/pipermail/discuss/2020-April/005429.html
    //implementation("org.springframework.experimental:spring-graalvm-native:${Versions.springGraalvmNative}")

    // kotlinx.reflect.lite unterstuetzt nur Namen von Parametern und deren Nullability
    implementation(kotlin("reflect"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("io.github.microutils:kotlin-logging:${Versions.kotlinLogging}")

    // "Starters" enthalten sinnvolle Abhaengigkeiten, die man i.a. benoetigt
    // Netty ist default bei Spring WebFlux
    // Tomcat ist default bei Spring WebMVC
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-tomcat")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.hateoas:spring-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-kotlin-configuration-properties
    // https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-configuration-metadata.html#configuration-metadata-annotation-processor
    // META-INF\additional-spring-configuration-metadata.json
    kapt("org.springframework.boot:spring-boot-configuration-processor:${Versions.springBoot}")
    // Java statt Kotlin:
    //annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${Versions.springBoot}")

    // https://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#beans-scanning-index
    // generiert Index-Datei META-INF/spring.components (in build\tmp\kapt3\classes\main\META-INF) statt Scanning des Classpath
    kapt("org.springframework:spring-context-indexer:${Versions.springBom}")
    // Java statt Kotlin:
    //compileOnly("org.springframework:spring-context-indexer")

    // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#using-boot-devtools
    // https://www.vojtechruzicka.com/spring-boot-devtools
    //runtimeOnly("org.springframework.boot:spring-boot-devtools:${Versions.springBoot}")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    ktlintCfg("com.pinterest:ktlint:${Versions.ktlint}")

    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:${Versions.Plugins.dokka}")

    // https://youtrack.jetbrains.net/issue/KT-27463
    @Suppress("UnstableApiUsage")
    constraints {
        implementation("org.springframework.hateoas:spring-hateoas:${Versions.springHateoas}")
        //implementation("org.springframework.data:spring-data-mongodb:${Versions.springDataMongoDB}")
        implementation("org.springframework.security:spring-security-rsa:${Versions.springSecurityRsa}")

        implementation("org.jetbrains:annotations:${Versions.annotations}")
        //implementation("org.reactivestreams:reactive-streams:${Versions.reactiveStreams}")
        implementation("org.hibernate.validator:hibernate-validator:${Versions.hibernateValidator}")
        //implementation("org.mongodb:mongodb-driver-core:${Versions.mongodb}")
        //implementation("org.mongodb:mongodb-driver-reactivestreams:${Versions.mongoDriverReactivestreams}")
        //implementation("org.apache.tomcat.embed:tomcat-embed-core:${Versions.tomcat}")
        //implementation("org.apache.tomcat.embed:tomcat-embed-el:${Versions.tomcat}")
        //implementation("org.thymeleaf:thymeleaf-spring5:${Versions.thymeleaf}")

        //testImplementation("org.assertj:assertj-core:${Versions.assertj}")

        ktlintCfg("org.jetbrains:annotations:${Versions.annotations}")
        ktlintCfg("org.jetbrains.kotlin:kotlin-stdlib:${Versions.ktlintKotlin}")
        ktlintCfg("org.jetbrains.kotlin:kotlin-compiler-embeddable:${Versions.ktlintKotlin}")
        ktlintCfg("org.apache.httpcomponents:httpclient:${Versions.httpClientKtlint}")
    }
    implementation(kotlin("stdlib-jdk8"))
}

configurations.all {
    exclude(group = "org.apache.tomcat.embed", module = "tomcat-embed-websocket")

    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    exclude(group = "junit", module = "junit")
    exclude(group = "org.hamcrest", module = "hamcrest")
    exclude(group = "org.mockito", module = "mockito-core")
    exclude(group = "org.skyscreamer", module = "jsonassert")
    exclude(group = "org.xmlunit", module = "xmlunit-core")

    exclude(group = "com.pinterest.ktlint", module = "ktlint-test")
    exclude(group = "org.apache.maven")

    // aktuelle Snapshots laden
    //resolutionStrategy { cacheChangingModulesFor(0, "seconds") }
}

//kotlinDslPluginOptions {
//}

allOpen {
    // FIXME Funktionale Bean Definition und @SpringBootApplication
    //annotation("org.springframework.context.annotation.Configuration")

    annotation("org.springframework.stereotype.Component")
    annotation("org.springframework.stereotype.Service")
    annotation("org.springframework.boot.context.properties.ConfigurationProperties")
}

noArg {
    annotation("org.springframework.boot.context.properties.ConfigurationProperties")
}

sweeney {
    enforce(mapOf("type" to "gradle", "expect" to "[6.7,6.8)"))
    // https://devcenter.heroku.com/articles/java-support#specifying-a-java-version
    enforce(
        mapOf(
            "type" to "jdk",
            "expect" to "[${Versions.javaMin},${Versions.javaMax}]"
        )
    )
    validate()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        apiVersion = "1.4"
        languageVersion = "1.4"
        jvmTarget = Versions.kotlinJvmTarget
        verbose = true
        // https://docs.gradle.org/6.6/userguide/kotlin_dsl.html#sec:kotlin_compiler_arguments
        freeCompilerArgs = listOfNotNull("-Xjsr305=strict")
        // https://kotlinlang.org/docs/reference/whatsnew14.html#new-jvm-ir-backend
        //useIR = true

        //allWarningsAsErrors = true
        // ggf. wegen Kotlin-Daemon: %TEMP%\kotlin-daemon.* und %LOCALAPPDATA%\kotlin\daemon
        // https://youtrack.jetbrains.com/issue/KT-18300
        //  $env:LOCALAPPDATA\kotlin\daemon
        //  $env:TEMP\kotlin-daemon.<ZEITSTEMPEL>
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        // https://play.kotlinlang.org/hands-on/Introduction%20to%20Coroutines%20and%20Channels/09_Testing
        @Suppress("SuspiciousCollectionReassignment")
        freeCompilerArgs += "-Xuse-experimental=kotlin.Experimental"
    }
}

// META-INF\additional-spring-configuration-metadata.json
//tasks.compileJava.inputs.files(processResources)

tasks.bootJar {
    doLast {
        println("")
        println("Aufruf der ausfuehrbaren JAR-Datei:")
        println("java -D'javax.net.ssl.trustStore=./src/main/resources/truststore.p12' -D'javax.net.ssl.trustStorePassword=zimmermann' -jar build/libs/${archiveFileName.get()} --spring.profiles.active=dev")
        println("")
    }
}

tasks.bootBuildImage {
    // https://github.com/paketo-buildpacks/builder
    // https://paketo.io/docs/getting-started/where-do-buildpacks-factor-in
    // https://github.com/spring-projects-experimental/spring-graalvm-native/blob/master/spring-graalvm-native-samples/webmvc-kotlin/build.gradle.kts
    // Image-Groesse: Default Builder ~ 150 MB, Tiny Builder ~ 75 MB
    //builder = "paketobuildpacks/builder:tiny"

    // Mit BP_JVM_VERSION kann man die Java-Version aus java.sourceCompatibility ueberschreiben
//    environment = mapOf(
//        // https://github.com/paketo-buildpacks/spring-boot-native-image
//        "BP_BOOT_NATIVE_IMAGE" to "true",
//        // https://repo.spring.io/milestone/org/springframework/experimental/spring-graalvm-native-docs/0.8.0/spring-graalvm-native-docs-0.8.0.zip!/reference/index.html#_graalvm_options
//        "BP_BOOT_NATIVE_IMAGE_BUILD_ARGUMENTS" to """
//                -Dspring.spel.ignore=true
//                -H:+ReportExceptionStackTraces
//            """.trimIndent()
//              // evtl. -H:+TraceClassInitialization
//              // evtl. --initialize-at-build-time fuer Netty
//    )
}

// https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin
jib {
    from {
        // https://hub.docker.com/_/openjdk
        // https://github.com/docker-library/docs/blob/master/openjdk/README.md
        //image = "openjdk:${Versions.jibJava}"
        image = "openjdk:${Versions.jibJava}-alpine"
    }
    to {
        image = project.name
    }
}

tasks.bootRun {
    // default ab Java 15:   -XX:+ShowCodeDetailsInExceptionMessages
    systemProperties = mapOf(
        "javax.net.ssl.trustStore" to "./src/main/resources/truststore.p12",
        "javax.net.ssl.trustStorePassword" to "zimmermann"
    )

    args("--spring.profiles.active=dev", "--spring.config.location=classpath:/application.yml")

    // Spring Property
    environment("spring.data.mongodb.password", "p")

    // Hotspot Compiler fuer aggressivere Optimierung als der Client-Compiler, aber laengere Startzeit: -server"
    // Remote Debugger:   .\gradlew bootRun --debug-jvm -verbose:class -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005
}

tasks.test {
    useJUnitPlatform {
        includeEngines("junit-jupiter")

        includeTags("rest", "fileRest", "streamingRest", "service")
        //includeTags("rest")
        //includeTags("fileRest")
        //includeTags("streamingRest")
        //includeTags("service")

        //excludeTags("service")
    }

    //filter {
    //    includeTestsMatching(includeTests)
    //}

    systemProperty("javax.net.ssl.trustStore", "./src/main/resources/truststore.p12")
    systemProperty("javax.net.ssl.trustStorePassword", "zimmermann")
    systemProperty("junit.platform.output.capture.stdout", true)
    systemProperty("junit.platform.output.capture.stderr", true)
    systemProperty("spring.config.location", "./src/main/resources/application.yml")
    //systemProperty("org.aspectj.tracing.enabled", false)
    //systemProperty("org.aspectj.tracing.messages", false)

    environment("spring.data.mongodb.password", "p")

    // https://docs.gradle.org/current/userguide/java_testing.html#sec:debugging_java_tests
    // https://www.jetbrains.com/help/idea/run-debug-configuration-junit.html
    // https://docs.gradle.org/current/userguide/java_testing.html#sec:debugging_java_tests
    //debug = true

    // damit nach den Tests immer ein HTML-Report von JaCoCo erstellt wird
    // https://github.com/gradle/gradle/pull/12626
    finalizedBy(tasks.jacocoTestReport)
}

// https://docs.qameta.io/allure/#_gradle_2
allure {
    autoconfigure = true
    configuration = "testImplementation"
    version = Versions.allure
    useJUnit5 { version = Versions.allureJunit }
    //downloadLink = "https://repo1.maven.org/maven2/io/qameta/allure/allure-commandline/${Versions.allureCommandline}/allure-commandline-${Versions.allureCommandline}.zip"
}

jacoco {
    toolVersion = Versions.jacocoVersion
}

// https://docs.gradle.org/current/userguide/task_configuration_avoidance.html
// https://guides.gradle.org/migrating-build-logic-from-groovy-to-kotlin/#configuring-tasks
tasks.getByName<JacocoReport>("jacocoTestReport") {
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }

    // afterEvaluate gibt es nur bei getByName<> ("eager"), nicht bei named<> ("lazy")
    // https://docs.gradle.org/5.0/release-notes.html#configuration-avoidance-api-disallows-common-configuration-errors
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it) { exclude("**/config/**", "**/entity/**") }
        }))
    }

    // https://github.com/gradle/gradle/pull/12626
    dependsOn(tasks.test)
}

// https://ktlint.github.io/#getting-started
// https://android.github.io/kotlin-guides/style.html
// https://kotlinlang.org/docs/reference/coding-conventions.html
// https://www.jetbrains.com/help/idea/code-style-kotlin.html
// https://github.com/android/kotlin-guides/issues/37
// https://github.com/shyiko/ktlint
@Suppress("KDocMissingDocumentation")
val ktlint by tasks.register<JavaExec>("ktlint") {
    group = "verification"

    classpath = ktlintCfg
    main = "com.pinterest.ktlint.Main"
    // https://github.com/pinterest/ktlint/blob/master/ktlint/src/main/kotlin/com/pinterest/ktlint/Main.kt
    args = listOfNotNull(
        "--verbose",
        "--experimental",
        "--relative",
        "--color",
        "--reporter=plain",
        "--reporter=checkstyle,output=$buildDir/reports/ktlint.xml",
        "src/**/*.kt"
    )
}
tasks.check { dependsOn(ktlint) }

/*
tasks {
    withType<Detekt> {
        //this.jvmTarget = "1.8"
        this.jvmTarget = Versions.kotlinJvmTarget
    }
}
*/
detekt {
    @Suppress("UnstableApiUsage")
    input = objects.fileCollection().from(
        "src/main/kotlin",
        "src/test/kotlin"
    )

    buildUponDefaultConfig = true
    failFast = true
    parallel = true
    config = files(project.rootDir.resolve("config/detekt.yaml"))
    reports {
        val reportsDir = "$buildDir/reports"
        xml { destination = file("$reportsDir/detekt.xml") }
        html { destination = file("$reportsDir/detekt.html") }
        txt { enabled = false }
    }
}

// https://github.com/jeremylong/DependencyCheck/blob/master/src/site/markdown/dependency-check-gradle/configuration.md
// https://github.com/jeremylong/DependencyCheck/issues/1732
dependencyCheck {
    scanConfigurations = listOfNotNull("runtimeClasspath")
    suppressionFile = "$projectDir/config/owasp.xml"
    data(closureOf<org.owasp.dependencycheck.gradle.extension.DataExtension> {
        directory = "C:/Zimmermann/owasp-dependency-check"
        username = "dc"
        password = "p"
    })

    analyzedTypes = listOfNotNull("jar")
    analyzers(closureOf<org.owasp.dependencycheck.gradle.extension.AnalyzerExtension> {
        // nicht benutzte Analyzer
        assemblyEnabled = false
        autoconfEnabled = false
        bundleAuditEnabled = false
        cmakeEnabled = false
        cocoapodsEnabled = false
        composerEnabled = false
        golangDepEnabled = false
        golangModEnabled = false
        nodeEnabled = false
        nugetconfEnabled = false
        nuspecEnabled = false
        pyDistributionEnabled = false
        pyPackageEnabled = false
        rubygemsEnabled = false
        swiftEnabled = false

        nodeAudit(closureOf<org.owasp.dependencycheck.gradle.extension.NodeAuditExtension> { enabled = true })
        retirejs(closureOf<org.owasp.dependencycheck.gradle.extension.RetireJSExtension> { enabled = true })
        //ossIndex(closureOf<org.owasp.dependencycheck.gradle.extension.OssIndexExtension> { enabled = true })
    })

    format = org.owasp.dependencycheck.reporting.ReportGenerator.Format.ALL
}

// SVG-Dateien von AsciidoctorPdf fuer Dokka umkopieren
@Suppress("KDocMissingDocumentation")
val copySvg by tasks.register<Copy>("copySvg") {
    from("$buildDir/docs/asciidocPdf")
    include("*.svg")
    into("$buildDir/dokka/${project.name}/images")

    dependsOn("asciidoctorPdf")
}

tasks.dokkaHtml {
    dokkaSourceSets {
        configureEach {
            includes.from("Module.md")
            apiVersion.set("1.4")
            languageVersion.set(apiVersion)
            reportUndocumented.set(true)
            jdkVersion.set(Versions.kotlinJvmTarget.toInt())
            noStdlibLink.set(true)
            noJdkLink.set(true)

            //suppressedFiles.from(
            //    "src/main/kotlin/entity/Kunde.kt",
            //    "src/main/kotlin/security/CustomUser.kt",
            //    "src/main/kotlin/security/CustomUserDetailsService.kt",
            //)
        }
    }

    dependsOn(copySvg)
}

tasks.asciidoctor {
    asciidoctorj {
        setVersion(Versions.asciidoctorj)
        //requires("asciidoctor-diagram")

        modules {
            diagram.use()
            diagram.setVersion(Versions.asciidoctorjDiagram)
        }
    }

    setBaseDir(file("doc"))
    setSourceDir(file("doc"))
    //setOutputDir(file("$buildDir/docs/asciidoc"))
    logDocuments = true

    doLast {
        val separator = System.getProperty("file.separator")
        println("Das Entwicklerhandbuch ist in $buildDir${separator}docs${separator}asciidoc${separator}entwicklerhandbuch.html")
    }
}

tasks.asciidoctorPdf {
    asciidoctorj {
        setVersion(Versions.asciidoctorj)

        modules {
            diagram.use()
            diagram.setVersion(Versions.asciidoctorjDiagram)
            pdf.setVersion(Versions.asciidoctorjPdf)
        }
    }

    setBaseDir(file("doc"))
    setSourceDir(file("doc"))
    attributes(mapOf("pdf-page-size" to "A4"))
    logDocuments = true

    doLast {
        val separator = System.getProperty("file.separator")
        println("Das Entwicklerhandbuch ist in $buildDir${separator}docs${separator}asciidocPdf${separator}entwicklerhandbuch.pdf")
    }
}

// FIXME Wie konfigurieren ???
jig {
    modelPattern = ".+\\.domain\\.(model|type)\\..+"
    outputOmitPrefix = ".+\\.(service|domain\\.(model|type))\\."
}

licenseReport {
    configurations = arrayOf("runtimeClasspath")
}

idea {
    module {
        isDownloadJavadoc = true
    }
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
