plugins {
    id("org.jetbrains.intellij.platform") version "2.16.0"
    kotlin("jvm") version "2.2.0"
}


group = "com.testy"
version = "0.3.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    pluginConfiguration {
        version = "${project.version}"
        ideaVersion {
            sinceBuild = "252.0"
            untilBuild = "261.*"
        }
    }

    pluginVerification {
        ides {
            providers.gradleProperty("verificationIdePath").orNull?.let {
                local(file(it))
            } ?: current()
        }
    }

    buildSearchableOptions = false
}

dependencies {
    intellijPlatform {
        goland("2025.2")
        bundledPlugin("org.jetbrains.plugins.go")
        bundledPlugin("org.jetbrains.plugins.yaml")
    }

    implementation("net.sourceforge.plantuml:plantuml:1.2024.5")
    // YAML parsing
    implementation("org.yaml:snakeyaml:2.2")
    // JSON Schema validation
    implementation("com.networknt:json-schema-validator:1.4.0")
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-console:1.11.3")
}

tasks {
    buildPlugin {
        archiveFileName.set("testy-goland-plugin.zip")
    }

    test {
        // Disable default Gradle test task since IntelliJ Gradle plugin injects IDE args that break pure unit tests
        enabled = false
    }
}

tasks.register<JavaExec>("unitTest") {
    group = "verification"
    description = "Runs unit tests via JUnit ConsoleLauncher"
    classpath = sourceSets.test.get().runtimeClasspath +
        configurations["testCompileClasspath"] +
        configurations["intellijPlatformTestRuntimeClasspath"]
    mainClass.set("org.junit.platform.console.ConsoleLauncher")
    args("--scan-classpath", "--include-classname", ".*Test$")
}
