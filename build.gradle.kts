import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.7"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"

}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.0")


    implementation("org.springframework.boot:spring-boot-starter-webflux") {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.datatype")
        exclude("com.fasterxml.jackson.module")
    }
    implementation("org.springframework.boot:spring-boot-starter-rsocket") {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.datatype")
        exclude("com.fasterxml.jackson.module")
    }


    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
