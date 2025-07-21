plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.1"
    id("org.jetbrains.kotlin.jvm") version "1.8.22" // Changed to 1.8.22 to match IDE requirement
}

group = "com.mohamedbamoh"
version = "1.0.0"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
intellij {
    version.set("2023.3") // Hardcoded version instead of using properties
    type.set("IC")        // Hardcoded type
    plugins.set(listOf("org.intellij.plugins.markdown"))
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        // Include all files from resources folder, not just markmap/**
        from("src/main/resources") {
            include("**/*")
        }
    }

    buildSearchableOptions {
        enabled = false
    }

    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("241.*")
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
            apiVersion = "1.8"    // Changed to 1.8
            languageVersion = "1.8" // Changed to 1.8
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
            apiVersion = "1.8"    // Changed to 1.8
            languageVersion = "1.8" // Changed to 1.8
        }
    }

    compileJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

kotlin {
    jvmToolchain(17)
}