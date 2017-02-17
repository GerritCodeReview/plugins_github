load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
  maven_jar(
    name = 'github_api',
    artifact = 'org.kohsuke:github-api:1.70',
    sha1 = 'e07035175f48066d9b25695ea1f515d9e6c39f85',
  )
