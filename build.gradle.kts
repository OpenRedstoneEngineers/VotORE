import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
    id("com.github.johnrengelman.shadow") version "2.0.4"
}

group = "org.openredstone"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/exposed")
    maven("https://jitpack.io")
    maven {
        name = "bungeecord-repo"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
    maven {
        name = "exceptionflug"
        url = uri("https://mvn.exceptionflug.de/repository/exceptionflug-public/")
    }
    maven {
        name = "aikar"
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
    implementation(group = "com.github.jkcclemens", name = "khttp", version = "0.1.0")
    implementation(group = "co.aikar", name = "acf-bungee", version = "0.5.0-SNAPSHOT")
    implementation(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.29.1")
    implementation(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.29.1")
    implementation(group = "com.uchuhimo", name = "konf", version = "1.1.2")
    implementation(group = "mysql", name = "mysql-connector-java", version = "8.0.19")
    compileOnly(group = "de.exceptionflug", name = "protocolize-api", version = "1.6.7-SNAPSHOT")
    compileOnly(group = "de.exceptionflug", name = "protocolize-items", version = "1.6.7-SNAPSHOT")
    compileOnly(group = "de.exceptionflug", name = "protocolize-inventory", version = "1.6.7-SNAPSHOT")
    compileOnly(group = "de.exceptionflug", name = "protocolize-world", version = "1.6.7-SNAPSHOT")
    compileOnly(group = "net.md-5", name = "bungeecord-api", version = "1.16-R0.5-SNAPSHOT")
}


tasks.shadowJar {
    relocate("co.aikar.commands", "votore.acf")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.javaParameters = true
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
