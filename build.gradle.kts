plugins {
    `java-library`
    `maven-publish`
    signing
    checkstyle
    jacoco
    id("com.github.spotbugs") version "4.5.0"
}

group = "de.siegmar"
version = "5.0.0-SNAPSHOT"

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
    api("ch.qos.logback:logback-classic:1.4.5")
    testImplementation(platform("org.junit:junit-bom:5.9.3"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    testImplementation("org.bouncycastle:bcpkix-jdk15on:1.70")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
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
    sign(publishing.publications["maven"])
}
