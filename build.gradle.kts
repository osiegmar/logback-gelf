plugins {
    `java-library`
    `maven-publish`
    signing
    pmd
    checkstyle
    jacoco
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.jreleaser)
}

group = "de.siegmar"
version = "6.1.2"

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
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    project {
        gitRootSearch.set(true)
        name.set("Logback GELF")
        description.set("Logback appender for sending GELF messages with zero additional dependencies.")
        authors.set(listOf("Oliver Siegmar"))
        license.set("LGPL-2.1")
        links {
            homepage.set("https://github.com/osiegmar/logback-gelf")
            license.set("https://opensource.org/license/lgpl-2-1")
        }
    }
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepositories.add("build/staging-deploy")
                }
            }
        }
    }
    release {
        github {
            changelog {
                formatted.set(org.jreleaser.model.Active.ALWAYS)
                preset.set("conventional-commits")
                append {
                    enabled.set(true)
                    target.set(file("CHANGELOG.md"))
                    title = "## [{{tagName}}] - {{#f_now}}YYYY-MM-dd{{/f_now}}"
                    content = "{{changelogTitle}}\n{{changelogContent}}"
                }
            }
        }
    }
}
