import com.google.protobuf.gradle.id
import java.text.SimpleDateFormat
import java.util.*

plugins {
    id("com.google.protobuf") version "0.9.4"
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.dokka-javadoc") version "2.2.0"
    `maven-publish`
}

group = "com.bluedragonmc"

repositories {
    mavenCentral()
}

val grpcKotlinVersion = "1.4.1"
val protoVersion = "4.30.1"
val grpcVersion = "1.71.0"

dependencies {
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("com.google.protobuf:protobuf-java:$protoVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protoVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
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

fun isInCI() = System.getenv("CI") != null

fun getPublishingVersion(): String = if (isInCI()) {
    val commitSha = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()

    val date = SimpleDateFormat("YYYY-MM-dd").format(Date())

    "$date-$commitSha"
} else {
    "dev"
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        if (isInCI()) {
            maven {
                name = "reposilite"
                url = uri("https://reposilite.bluedragonmc.com/releases")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.bluedragonmc"
            artifactId = "rpc"
            version = getPublishingVersion()

            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dokka {
    dokkaSourceSets {
        configureEach {
            suppressGeneratedFiles.set(false)
        }
    }
}
