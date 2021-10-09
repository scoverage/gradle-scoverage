plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "0.15.0"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.0"
    id("org.jetbrains.dokka") version "1.5.30"
}

repositories {
    mavenCentral()
}

group = "org.scoverage"
description = "gradle-scoverage is a Gradle plugin for calculating code coverage using Scoverage"
if (project.version == "unspecified") {
    version = "7.0.0-SNAPSHOT"
}

val website by extra("http://scoverage.org")
val vcsUrl by extra("https://github.com/scoverage/gradle-scoverage.git")
val scmUrl by extra("scm:git:$vcsUrl")
val sonatypeUser by extra(System.getenv("SONATYPE_USER"))
val sonatypePass by extra(System.getenv("SONATYPE_PASS"))

gradlePlugin {
    plugins {
        create("gradleScoverage") {
            id = "org.scoverage"
            implementationClass = "org.scoverage.ScoveragePlugin"
            displayName = "Gradle Scoverage plugin"
        }
    }
}

pluginBundle {
    website = website
    vcsUrl = vcsUrl
    description = project.description
    tags = listOf("coverage", "scala", "scoverage")
}

apply(plugin = "maven-publish")

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly("org.scoverage:scalac-scoverage-plugin_2.13:1.4.2")
    implementation(group = "commons-io", name = "commons-io", version = "2.6")

    implementation(kotlin("script-runtime"))
    testImplementation(kotlin("test"))

    testImplementation("junit:junit:4.12")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.5.2")

    testImplementation("org.hamcrest:hamcrest:2.2")
}

sourceSets {
    create("functionalTest") {
        java.srcDir(file("src/functionalTest/java"))
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
    create("crossScalaVersionTest") {
        java.srcDir(file("src/crossScalaVersionTest/java"))
        compileClasspath += sourceSets["main"].output + sourceSets["functionalTest"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["functionalTest"].output
    }
}

configurations {
    named("functionalTestImplementation") {
        extendsFrom(configurations["testImplementation"])
    }
    named("functionalTestRuntimeOnly") {
        extendsFrom(configurations["testRuntimeOnly"])
    }

    named("crossScalaVersionTestImplementation") {
        extendsFrom(configurations["testImplementation"])
    }
    named("crossScalaVersionTestRuntimeOnly") {
        extendsFrom(configurations["testRuntimeOnly"])
    }
}

val crossScalaVersionTest by tasks.registering(Test::class) {
    description = "Runs the cross scala version functional test."
    group = "verification"
    testClassesDirs = sourceSets["crossScalaVersionTest"].output
    classpath = sourceSets["crossScalaVersionTest"].runtimeClasspath
    setForkEvery(1) // crucial to run every test in its own JVM

    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = System.getenv("CI") == "true"
    }

    mustRunAfter(tasks["test"])
}
tasks["check"].dependsOn(crossScalaVersionTest)

val functionalTest by tasks.registering(Test::class) {
    description = "Runs the functional tests."
    group = "verification"
    testClassesDirs = sourceSets["functionalTest"].output
    classpath = sourceSets["functionalTest"].runtimeClasspath

    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = System.getenv("CI") == "true"
    }

    systemProperty("failOnWarning", project.hasProperty("failOnWarning"))

    mustRunAfter(crossScalaVersionTest)
}
tasks["check"].dependsOn(functionalTest)

gradlePlugin {
    testSourceSets(sourceSets["functionalTest"], sourceSets["crossScalaVersionTest"])
}

val kotlindocJar by tasks.registering(Jar::class) {
    from(tasks.dokkaHtml.get().outputDirectory)
    archiveClassifier.set("kotlindoc")
    dependsOn(tasks.dokkaHtml)
}

val sourcesJar by tasks.registering(Jar::class) {
    from(sourceSets["main"].allSource)
    archiveClassifier.set("sources")
}

fun propOrDefault(property: String): String {
    if (project.hasProperty(property)) {
        return project.property(property).toString()
    } else {
        return ""
    }
}

configure<PublishingExtension> {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = propOrDefault("sonatypeUser")
                password = propOrDefault("sonatypePass")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("GradleScoverage")
                description.set(description)
                url.set(website)

                scm {
                    url.set(scmUrl)
                    developerConnection.set(scmUrl)
                }

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("maiflai")
                    }
                    developer {
                        id.set("ubourdon")
                    }
                    developer {
                        id.set("D-Roch")
                    }
                    developer {
                        id.set("eyalroth")
                    }
                }
            }
            from(components["java"])
            artifact(kotlindocJar)
            artifact(sourcesJar)
        }
    }
}

if (project.properties.containsKey("signing.keyId")) {
    apply(plugin = "signing")
    configure<SigningExtension> {
        sign(the<PublishingExtension>().publications["mavenJava"])
    }
}

// see https://stackoverflow.com/questions/44679007
val fixIdeaPluginClasspath by tasks.registering {
    doFirst {
        tasks {
            named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
                val ideaClassesPath = project.buildDir.toPath().resolveSibling("out").resolve("production")
                val newClasspath = pluginClasspath.toMutableList()
                newClasspath.add(0, file(ideaClassesPath))
                pluginClasspath.setFrom(newClasspath)
            }
        }
    }
}
tasks["pluginUnderTestMetadata"].mustRunAfter(fixIdeaPluginClasspath)

idea {
    project {
        this as ExtensionAware
        configure<org.jetbrains.gradle.ext.ProjectSettings> {
            this as ExtensionAware
            configure<org.jetbrains.gradle.ext.TaskTriggersConfig> {
                beforeBuild(fixIdeaPluginClasspath, tasks["pluginUnderTestMetadata"])
            }
        }
    }
}
