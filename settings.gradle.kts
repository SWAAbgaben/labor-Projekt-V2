pluginManagement {
    repositories {
        gradlePluginPortal()
        //maven("https://plugins.gradle.org/m2")
        mavenCentral()
        // detekt
        //jcenter()

        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        //maven("https://dl.bintray.com/kotlin/kotlin-dev") {
        //    mavenContent { snapshotsOnly() }
        //}
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/plugins-release")

        // Snapshots von Spring Framework, Spring Data, Spring Security und Spring Cloud
        //maven("https://repo.spring.io/snapshot")
    }
}

buildCache {
    local {
        directory = "C:/Z/caches"
    }
}

rootProject.name = "labor"
include("src:main")
findProject(":src:main")?.name = "labor.main"
