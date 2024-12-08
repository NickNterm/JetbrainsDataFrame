plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.kotlinx.dataframe") version "0.14.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:dataframe:0.14.1")
}

tasks.test {
    useJUnitPlatform()
}