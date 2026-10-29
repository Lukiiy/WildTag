plugins {
    java
    kotlin("jvm") version "2.3.21"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "me.lukiiy"
version = "1.0"

repositories {
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    implementation(kotlin("stdlib"))
    implementation(files("lib/waytrick0.jar"))
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))

tasks {
    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
        minimize()
    }

    build { dependsOn(shadowJar) }

    jar { enabled = false }

    processResources {
        val props = mapOf("version" to version)

        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") { expand(props) }
    }
}

kotlin { jvmToolchain(25) }