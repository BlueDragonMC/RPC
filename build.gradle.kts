import com.google.protobuf.gradle.id
import java.text.SimpleDateFormat
import java.util.*

plugins {
    alias(libs.plugins.protobuf)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    `maven-publish`
}

group = "com.bluedragonmc"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)
    implementation(libs.coroutines.core)
    compileOnly(libs.javax.annotation.api)
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val protobufVersion = catalog.findVersion("protobuf").get().requiredVersion
val grpcVersion = catalog.findVersion("grpc").get().requiredVersion
val grpcKotlinVersion = catalog.findVersion("grpc-kotlin").get().requiredVersion

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
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

tasks.processResources {
    from(rootProject.file("LICENSE")) {
        into("META-INF")
    }
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

            pom {
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
            }
        }
    }
}

val javaVersion = catalog.findVersion("jvm").get().requiredVersion.toInt()

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

dokka {
    dokkaSourceSets {
        configureEach {
            suppressGeneratedFiles.set(false)
        }
    }
}
