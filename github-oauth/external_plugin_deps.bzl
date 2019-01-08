load("//tools/bzl:maven_jar.bzl", "GERRIT", "MAVEN_CENTRAL", "MAVEN_LOCAL", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "javax-servlet",
        artifact = "javax.servlet-api:javax.servlet:3.0.1",
        sha1 = "6bf0ebb7efd993e222fc1112377b5e92a13b38dd",
    )

    maven_jar(
        name = "slf4j-api",
        artifact = "org.slf4j:slf4j-api:1.7.5",
        sha1 = "6b262da268f8ad9eff941b25503a9198f0a0ac93",
    )

    maven_jar(
        name = "slf4j-log4j12",
        artifact = "org.slf4j:slf4j-log4j12:1.7.5",
        sha1 = "6edffc576ce104ec769d954618764f39f0f0f10d",
    )

    maven_jar(
        name = "github-api",
        artifact = "org.kohsuke:github-api:1.70",
        sha1 = "e07035175f48066d9b25695ea1f515d9e6c39f85",
    )
