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
    // https://mvnrepository.com/artifact/org.mockito/mockito-core
    testImplementation("org.mockito:mockito-core:5.14.2")
}
tasks.test {
    useJUnitPlatform()
}

// Dynamically set the version from the latest Git tag
version = run {
    // Function to check if Git is available
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
            gitTag.ifEmpty {
                "1.0.0-SNAPSHOT" // Default version if no tag is found
            }
        } catch (e: Exception) {
            "1.0.0-SNAPSHOT" // Default version if Git command fails
        }
    } else {
        "1.0.0-SNAPSHOT" // Default version if Git is not available
    }
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            groupId = "de.n21no.realtime.pubsub"
            artifactId = "core"
            version = version
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/BackendStack21/realtime-pubsub-client-java")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
