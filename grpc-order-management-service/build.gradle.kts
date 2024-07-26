import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.liquibase.gradle") version "2.1.1"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    kotlin("plugin.jpa") version "1.9.23"
    kotlin("plugin.noarg") version "1.9.23"
    id("com.avast.gradle.docker-compose") version "0.14.3"
    id("nebula.integtest") version "9.6.3"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
    id("pl.allegro.tech.build.axion-release") version "1.17.0"
    jacoco
    idea
}

val grpcSBVersion = "3.0.0.RELEASE"

dependencies {
    // shared
    implementation(project(":shared"))
    // gRPC stubs
    implementation(project(":grpc-stubs"))
    // gRPC shared
    implementation(project(":grpc-shared"))

    implementation(kotlin("reflect"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("net.devh:grpc-server-spring-boot-starter:$grpcSBVersion")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("javax.inject:javax.inject:1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.8.0") // needs to match stubs dependency version

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.liquibase:liquibase-core")
    runtimeOnly("org.postgresql:postgresql")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.3")
    implementation("org.hibernate.orm:hibernate-ehcache:6.0.0.Alpha7")
    implementation("org.ehcache:ehcache:3.10.8")
    implementation("org.hibernate.orm:hibernate-jcache:6.5.0.Final")
    implementation("com.google.protobuf:protobuf-java-util:3.23.4")

    implementation("com.amazonaws:aws-java-sdk-cognitoidp:1.12.696")
    implementation("com.nimbusds:nimbus-jose-jwt:9.35")
    implementation("org.springframework.cloud:spring-cloud-starter-aws-messaging:2.2.6.RELEASE")
    implementation("org.springframework:spring-messaging:5.3.24")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.amazonaws:aws-java-sdk-s3:1.12.629")
    implementation("com.amazonaws:aws-java-sdk-ses:1.12.555")

    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    // test libs
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("io.github.serpro69:kotlin-faker:1.14.0")

    // integ test libs
    integTestImplementation("org.springframework.boot:spring-boot-starter-test")
    integTestImplementation("org.springframework.security:spring-security-test")
    integTestImplementation("net.devh:grpc-client-spring-boot-starter:$grpcSBVersion")
    integTestImplementation("org.awaitility:awaitility-kotlin:4.2.0")
    integTestImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    integTestImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")
    integTestImplementation("org.bouncycastle:bcpkix-jdk18on:1.78.1") // JWK Generation: org.bouncycastle.cert.jcajce.JcaX509CertificateHolder

    // documentation libs
    integTestImplementation("net.sourceforge.plantuml:plantuml:1.2023.1")
}

repositories {
    jcenter() // added for swagger2markup
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

tasks.getByName<Jar>("jar") {
    enabled = false
}

tasks.withType<BootJar> {
    this.archiveFileName.set("${archiveBaseName.get()}-final.${archiveExtension.get()}")
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

noArg {
    annotation("com.ribbontek.ordermanagement.context.NoArg")
    invokeInitializers = true
}

idea {
    module {
        testSourceDirs.plusAssign(project.sourceSets["integTest"].kotlin.srcDirs)
        testResourceDirs.plusAssign(project.sourceSets["integTest"].resources.srcDirs)
    }
}

liquibase {
    activities.register("main") {
        val dbUrl by project.extra.properties
        val dbUser by project.extra.properties
        val dbPassword by project.extra.properties

        this.arguments =
            mapOf(
                "logLevel" to "info",
                "changeLogFile" to "src/main/resources/liquibase/db.changelog-master.xml",
                "url" to (project.findProperty("DATABASE_URL")?.toString() ?: dbUrl),
                "username" to (project.findProperty("DATABASE_USERNAME")?.toString() ?: dbUser),
                "password" to (project.findProperty("DATABASE_PASSWORD")?.toString() ?: dbPassword),
                "driver" to "org.postgresql.Driver"
            )
    }
    runList = "main"
}

dockerCompose.isRequiredBy(tasks.getByName("integrationTest"))

val versionFile by tasks.registering {
    doLast {
        mkdir("${project.buildDir}/version")
        file("${project.buildDir}/version/version").writeBytes(scmVersion.undecoratedVersion.toByteArray())
        project.version = scmVersion.undecoratedVersion
    }
}

tasks.build {
    dependsOn(versionFile)
}

scmVersion {
    tag {
        // if no tags exists, this sets the starting position
        initialVersion { _, _ -> "1.0.0" }
    }
    // Example options to pass-through: incrementPatch, incrementMinor, incrementMajor (PredefinedVersionIncrementer)
    val incrementer: String = project.findProperty("release.scope")?.toString() ?: "incrementMinor"
    // Use minor, not patch by default. e.g. 1.0.0 -> 1.1.0
    versionIncrementer(incrementer)
    // Adds branch names to snapshots
    branchVersionCreator.putAll(
        mapOf(
            "feature/.*" to "versionWithBranch",
            "hotfix/.*" to "versionWithBranch"
        )
    )
}

tasks.integrationTest {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    executionData.setFrom(buildDir.path + "/jacoco/test.exec", buildDir.path + "/jacoco/integrationTest.exec")
    reports {
        xml.required = true
        csv.required = false
    }
    dependsOn(tasks.test, tasks.integrationTest)
}

tasks.jacocoTestCoverageVerification {
    executionData.setFrom(buildDir.path + "/jacoco/test.exec", buildDir.path + "/jacoco/integrationTest.exec")
    dependsOn(tasks.test, tasks.integrationTest)
    violationRules {
        rule {
            limit {
                // current coverage is > 50%; should bump up to 85% when that point is reached.
                minimum = BigDecimal(0.5)
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
