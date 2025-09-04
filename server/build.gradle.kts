plugins {
    application
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("io.ktor.plugin") version "2.3.11"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}



dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm:2.3.11")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.11")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.11")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.11")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.11")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.11") // NOTE: artifact uses call-logging spelling
    // If Gradle complains, use the unified module name:
    // implementation("io.ktor:ktor-server-call-logging-jvm:2.3.11")

    // Logging backend (choose one)
    implementation("org.slf4j:slf4j-simple:2.0.13")
    // or: implementation("ch.qos.logback:logback-classic:1.4.14")

    // Tests
    testImplementation("io.ktor:ktor-server-test-host-jvm:2.3.11")
    testImplementation(kotlin("test"))
}

application {
    // Using EngineMain + application.conf
    mainClass.set("io.ktor.server.netty.EngineMain")
}

tasks.test {
    // Avoid port clashes on CI
    environment("PORT", "0")
}

kotlin {
    jvmToolchain(17)
}
