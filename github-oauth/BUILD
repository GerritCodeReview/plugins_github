load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "github-oauth",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: github-oauth",
    ],
    deps = [
        "@javax-servlet-api//jar",
        "@slf4j-api//jar",
        "@slf4j-log4j12//jar",
        "@github-api//jar",
        "@lombok//jar",
        "@bridge-method-injector//jar",
    ],
)
