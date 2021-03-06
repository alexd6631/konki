import me.champeau.gradle.JMHPluginExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "1.3.21"
    id("me.champeau.gradle.jmh") version "0.4.8"
    `maven-publish`
    id("org.jetbrains.dokka") version "0.9.17"
}

group = "io.monkeypatch"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.2.1")
}

val compileKotlin: KotlinCompile by tasks
val compileTestKotlin: KotlinCompile by tasks
val compileJmhKotlin: KotlinCompile by tasks

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

/*
val jvmTarget = "1.6"
compileKotlin.kotlinOptions.jvmTarget = jvmTarget
compileTestKotlin.kotlinOptions.jvmTarget = jvmTarget
compileJmhKotlin.kotlinOptions.jvmTarget = jvmTarget
*/


configure<JMHPluginExtension> {
    fork = 1
    include = listOf("PVectorFoldBenchmark")
    //iterations = 3
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

val dokka by tasks.getting(DokkaTask::class) {
    outputDirectory = "$buildDir/javadoc"
}