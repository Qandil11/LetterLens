plugins {
    id("application")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


kotlin { jvmToolchain(17) }

dependencies {
    implementation("io.ktor:ktor-server-netty:2.3.11")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.11")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
    implementation("io.ktor:ktor-server-cors:2.3.11")
    implementation("io.ktor:ktor-server-call-logging:2.3.11")
    implementation("io.ktor:ktor-server-status-pages:2.3.11")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:2.3.11")
    testImplementation("io.ktor:ktor-client-content-negotiation:2.3.11")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
}

application {
    // MUST match your Ktor main class
    mainClass.set("com.qandil.letterlens.server.MainKt")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("server")
    archiveClassifier.set("all")
    mergeServiceFiles()
}

tasks.named<JavaExec>("run") {
    // nicer dev logs
    systemProperty("io.ktor.development", "true")
}
