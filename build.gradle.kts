plugins {
    java
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    jacoco
}

group = "com.snakeforged"
version = "0.1.0-SNAPSHOT"
description = "Snake Web: A modern Java Spring Boot web application migration of the classic Snake game"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.5")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.2.5")
    implementation("org.springframework.boot:spring-boot-starter-actuator:3.2.5")

    // Database
    runtimeOnly("com.h2database:h2:2.2.224")

    // Database Migrations
    implementation("org.flywaydb:flyway-core:9.22.3")

    // Logging
    implementation("org.springframework.boot:spring-boot-starter-logging:3.2.5")

    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
}

tasks.test {
    useJUnitPlatform()

    // Enable JaCoCo for coverage reporting
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.bootRun {
    args = listOf("--spring.profiles.active=dev")
}
