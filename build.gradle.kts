import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.10"
    id("com.gradleup.shadow") version "8.3.0"
    id("org.jetbrains.kotlin.kapt") version "1.9.22"
}

group = "org.openredstone"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "sonatype-oss"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "aikar"
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        name = "velocity"
        url = uri("https://nexus.velocitypowered.com/repository/maven-public/")
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "org.mariadb.jdbc", name = "mariadb-java-client", version = "3.5.1")
    implementation(group = "org.danilopianini", name = "khttp", version = "1.6.3")
    implementation(group = "co.aikar", name = "acf-velocity", version = "0.5.1-SNAPSHOT")
    implementation(group = "org.jetbrains.exposed", name = "exposed-core", version = "0.58.0")
    implementation(group = "org.jetbrains.exposed", name = "exposed-jdbc", version = "0.58.0")
    implementation(group = "com.uchuhimo", name = "konf", version = "1.1.2")
    implementation(group = "mysql", name = "mysql-connector-java", version = "8.0.19")
    implementation(group = "com.velocitypowered", name = "velocity-api", version = "3.2.0-SNAPSHOT")
    kapt(group = "com.velocitypowered", name = "velocity-api", version = "3.2.0-SNAPSHOT")
}


tasks.shadowJar {
    relocate("co.aikar.commands", "votore.acf")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "22"
    kotlinOptions.javaParameters = true
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
