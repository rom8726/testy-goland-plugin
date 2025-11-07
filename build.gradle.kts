plugins {
    id("org.jetbrains.intellij") version "1.17.1"
    kotlin("jvm") version "2.2.0"
}

// Prevent IntelliJ Gradle plugin from trying to resolve a real IDE during unit tests
val isTestTask = gradle.startParameter.taskNames.any { it.contains("test", ignoreCase = true) }
if (isTestTask) {
    System.setProperty("idea.home.path", project.layout.projectDirectory.dir(".fake-ide").asFile.absolutePath)
}

group = "com.testy"
version = "0.1.0"

repositories {
    mavenCentral()
}

intellij {
    // GoLand 2025.2
    type.set("GO")
    version.set("2025.2")

    plugins.set(listOf("org.jetbrains.plugins.go"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("252.0")
        untilBuild.set("253.*")
    }

    buildPlugin {
        archiveFileName.set("testy-goland-plugin.zip")
    }

    runIde {
        val userHome = System.getProperty("user.home")
        val macIdeContents = file("$userHome/Applications/GoLand.app/Contents")
        if (macIdeContents.exists()) {
            ideDir.set(macIdeContents)
        }
    }

    buildSearchableOptions {
        enabled = false
    }

    test {
        useJUnitPlatform()
    }
}

dependencies {
    implementation("net.sourceforge.plantuml:plantuml:1.2024.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-console:1.11.3")
}

tasks.register<JavaExec>("unitTest") {
    group = "verification"
    description = "Runs unit tests via JUnit ConsoleLauncher"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("org.junit.platform.console.ConsoleLauncher")
    args("--scan-classpath")
}
