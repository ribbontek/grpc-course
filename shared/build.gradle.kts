import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.jpa") version "1.9.23"
    id("com.avast.gradle.docker-compose") version "0.14.3"
    id("nebula.integtest") version "9.6.3"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
    idea
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    integTestImplementation("org.springframework.boot:spring-boot-starter-test")
    integTestRuntimeOnly("org.postgresql:postgresql")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    allOpen {
        annotations(
            "javax.persistence.Entity",
            "javax.persistence.MappedSuperclass",
            "javax.persistence.Embedabble"
        )
    }
}

idea {
    module {
        testSourceDirs.plusAssign(project.sourceSets["integTest"].kotlin.srcDirs)
        testResourceDirs.plusAssign(project.sourceSets["integTest"].resources.srcDirs)
    }
}

dockerCompose.isRequiredBy(tasks.getByName("integrationTest"))

tasks.bootJar {
    enabled = false
}
