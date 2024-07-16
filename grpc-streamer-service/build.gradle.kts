import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
    idea
}

val grpcSBVersion = "3.0.0.RELEASE"

dependencies {
    // gRPC stubs
    implementation(project(":grpc-stubs"))

    implementation(kotlin("reflect"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("net.devh:grpc-server-spring-boot-starter:$grpcSBVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.8.0") // needs to match stubs dependency version
    implementation("com.google.protobuf:protobuf-java-util:3.23.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("net.devh:grpc-client-spring-boot-starter:$grpcSBVersion")
    testImplementation("io.github.serpro69:kotlin-faker:1.14.0")
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
