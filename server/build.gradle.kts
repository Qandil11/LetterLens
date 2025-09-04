plugins {
    application
    kotlin("jvm") version "1.9.24"
    id("io.ktor.plugin") version "2.3.11"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    // Using EngineMain + application.conf
    mainClass.set("io.ktor.server.netty.EngineMain")
}

// Tests must not grab 8080 in CI
tasks.test {
    environment("PORT", "0")
}
