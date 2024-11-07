import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.lib)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    compileSdk = 35
    namespace = "com.evolitist.photoview"

    defaultConfig {
        minSdk = 21
    }
}

kotlin {
    explicitApi()
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
}

val androidJavadocs = tasks.register<Javadoc>("androidJavadocs") {
    source = android.sourceSets["main"].java.getSourceFiles()
    classpath += project.files(android.bootClasspath.joinToString(File.pathSeparator))
}

val androidJavadocsJar = tasks.register<Jar>("androidJavadocsJar") {
    dependsOn(androidJavadocs)
    archiveClassifier.set("javadoc")
    from(androidJavadocs.get().destinationDir)
}

val sourceJar = tasks.register<Jar>("sourceJar") {
    from(android.sourceSets["main"].java.srcDirs)
    archiveClassifier.set("sources")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                group = "com.github.evolitist"
                artifactId = "PhotoView"
                version = "1.0.0"

                // Adds javadocs and sources as separate jars.
                artifact(androidJavadocsJar)
                artifact(sourceJar)

                pom {
                    name = "PhotoView"
                    description = "A simple ImageView that support zooming, both by Multi-touch gestures and double-tap."
                    url = "https://github.com/Evolitist/PhotoView"
                    licenses {
                        license {
                            name = "The Apache License, Version 2.0"
                            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    developers {
                        developer {
                            id = "evolitist"
                            name = "Evolitist"
                        }
                    }
                    scm {
                        connection = "scm:git@github.com/Evolitist/PhotoView.git"
                        developerConnection = "scm:git@github.com/Evolitist/PhotoView.git"
                        url = "https://github.com/Evolitist/PhotoView"
                    }
                }
            }
        }
    }
}
