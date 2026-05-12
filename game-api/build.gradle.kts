plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    jacoco
}

dependencies {
    implementation(project(":game-domain"))
    implementation(project(":game-persistence"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")
    runtimeOnly(project(":game-web-ui"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
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
