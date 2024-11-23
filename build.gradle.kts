plugins {
    id("java")
}

group = "de.21no"
version = "1.0.0-alpha"

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