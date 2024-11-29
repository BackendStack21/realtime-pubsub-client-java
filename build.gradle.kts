import java.io.ByteArrayOutputStream

plugins {
    id("java")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/jakarta.websocket/jakarta.websocket-api
    compileOnly("jakarta.websocket:jakarta.websocket-api:2.2.0")

    // https://mvnrepository.com/artifact/org.glassfish.tyrus.bundles/tyrus-standalone-client
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:2.2.0")

    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.1")

    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.14.2")
}
tasks.test {
    useJUnitPlatform()
}

// Dynamically set the version from the latest Git tag
version = run {
    fun isGitAvailable(): Boolean {
        return try {
            exec {
                commandLine("git", "--version")
                standardOutput = ByteArrayOutputStream()
                errorOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }.exitValue == 0
        } catch (e: Exception) {
            false
        }
    }

    if (isGitAvailable()) {
        try {
            val stdout = ByteArrayOutputStream()
            exec {
                commandLine("git", "describe", "--tags", "--abbrev=0")
                standardOutput = stdout
                errorOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }
            val gitTag = stdout.toString().trim()
            if (gitTag.isNotEmpty()) gitTag else "1.0.0-SNAPSHOT"
        } catch (e: Exception) {
            "1.0.0-SNAPSHOT"
        }
    } else {
        "1.0.0-SNAPSHOT"
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    main {
        java {
            exclude("samples/**")
        }
    }
}

tasks.named<Jar>("jar") {
    exclude("samples/**")
}

tasks.named<Jar>("sourcesJar") {
    exclude("samples/**")
}

tasks.named<Javadoc>("javadoc") {
    exclude("samples/**")
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])

            groupId = "de.n21no.realtime.pubsub"
            artifactId = "core"
            version = project.version.toString()

            pom {
                name.set("Realtime Pub/Sub Client")
                description.set("A Java client for Realtime Pub/Sub")
                url.set("https://github.com/BackendStack21/realtime-pubsub-client-java")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("BackendStack21")
                        name.set("21no.de")
                        email.set("realtime.contact@21no.de")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/BackendStack21/realtime-pubsub-client-java.git")
                    developerConnection.set("scm:git:ssh://git@github.com:BackendStack21/realtime-pubsub-client-java.git")
                    url.set("https://github.com/BackendStack21/realtime-pubsub-client-java")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/BackendStack21/realtime-pubsub-client-java")
        }
    }
}