plugins {
    `java-library`
    `maven-publish`
    signing
    pmd
    checkstyle
    jacoco
    alias(libs.plugins.spotbugs)
}

group = "de.siegmar"
version = "6.1.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.logback.classic)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.assertj.core)
    testImplementation(libs.json.unit.assertj)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.bcpkix.jdk18on)
    testImplementation(libs.wiremock)
    testImplementation(libs.awaitility)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

pmd {
    isConsoleOutput = true
    ruleSets = emptyList()
    ruleSetFiles = files("${project.rootDir}/config/pmd/config.xml")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter = file("${project.rootDir}/config/spotbugs/config.xml")
    reports.maybeCreate("xml").required = false
    reports.maybeCreate("html").required = true
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "logback-gelf"
            from(components["java"])

            pom {
                name = "Logback GELF"
                description = "Logback appender for sending GELF messages with zero additional dependencies."
                url = "https://github.com/osiegmar/logback-gelf"
                licenses {
                    license {
                        name = "GNU Lesser General Public License version 2.1"
                        url = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt"
                    }
                }
                scm {
                    url = "https://github.com/osiegmar/logback-gelf"
                    connection = "scm:git:https://github.com/osiegmar/logback-gelf.git"
                }
                developers {
                    developer {
                        id = "osiegmar"
                        name = "Oliver Siegmar"
                        email = "oliver@siegmar.de"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "ossrh"
            credentials(PasswordCredentials::class)
            url = if (version.toString().endsWith("SNAPSHOT")) {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            } else {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
}
