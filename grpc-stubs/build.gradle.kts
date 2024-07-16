import com.google.protobuf.gradle.id

plugins {
    kotlin("jvm") version "1.9.23"
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    api("io.grpc:grpc-kotlin-stub:1.4.1")
    api("io.grpc:grpc-protobuf:1.62.2")
    api("com.google.protobuf:protobuf-kotlin:3.23.4")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

protobuf {
    protoc {
        artifact = when (osdetector.os) {
            "osx" -> "com.google.protobuf:protoc:3.23.4:osx-x86_64"
            "linux" -> "com.google.protobuf:protoc:3.23.4:linux-x86_64"
            else -> "com.google.protobuf:protoc:3.23.4"
        }
    }
    plugins {
        protoc {
            id("grpc") {
                artifact = when (osdetector.os) {
                    "osx" -> "io.grpc:protoc-gen-grpc-java:1.62.2:osx-x86_64"
                    else -> "io.grpc:protoc-gen-grpc-java:1.62.2"
                }
            }
            id("grpckt") {
                artifact = when (osdetector.os) {
                    in listOf("osx", "linux") -> "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
                    else -> "io.grpc:protoc-gen-grpc-kotlin:1.4.1"
                }
            }
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
                id("grpckt")
            }
            task.builtins {
                id("kotlin")
            }
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "21"
        }
    }
}
