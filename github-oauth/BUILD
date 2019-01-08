load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "github-oauth",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: github-oauth",
    ],
    deps = [
        ":github_oauth__plugin_deps"
    ],
)

java_library(
    name = "github_oauth__plugin_deps",
    visibility = ["//visibility:public"],
    exports = [
        "@bridge-method-injector//jar",
        "@javax-servlet-api//jar",
        "@lombok//jar",
        "@slf4j-api//jar",
        "@slf4j-log4j12//jar",
        "@github-api//jar",
    ],
)

