import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "1.3.20"
    id("me.champeau.gradle.jmh") version "0.4.8"
}

group = "io.monkeypatch"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("junit:junit:4.12")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.2.1")
}

val compileKotlin: KotlinCompile by tasks
val compileTestKotlin: KotlinCompile by tasks

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

compileKotlin.kotlinOptions.jvmTarget = "1.8"
compileTestKotlin.kotlinOptions.jvmTarget = "1.8"