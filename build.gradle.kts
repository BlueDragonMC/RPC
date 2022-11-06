import com.google.protobuf.gradle.*

plugins {
    id("com.google.protobuf") version "0.8.19"
    kotlin("jvm") version "1.7.10"
    `maven-publish`
}

group = "com.bluedragonmc"

repositories {
    mavenCentral()
}

val grpcKotlinVersion = "1.3.0"
val protoVersion = "3.21.9"
val grpcVersion = "1.50.2"

dependencies {
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("com.google.protobuf:protobuf-java:$protoVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protoVersion")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protoVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc") { }
                id("grpckt") { }
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.bluedragonmc"
            artifactId = "rpc"
            version = "1.0"

            from(components["java"])
        }
    }
}