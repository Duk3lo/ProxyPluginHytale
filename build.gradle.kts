plugins {
    id("java")
}

group = "org.astral"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/api-1.0.1.jar"))

    // 🔹 JetBrains annotations
    compileOnly("org.jetbrains:annotations:24.1.0")

    implementation("org.slf4j:slf4j-api:2.0.13")


    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


tasks.test {
    useJUnitPlatform()
}
