load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
)

java_plugin(
    name = "lombok_plugin",
    generates_api = True,
    processor_class = "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
    visibility = ["//visibility:public"],
    deps = ["@lombok//jar"],
)

java_library(
    name = "lombok",
    exported_plugins = [":lombok_plugin"],
    visibility = ["//visibility:public"],
    exports = ["@lombok//jar"],
)

gerrit_plugin(
    name = "github-plugin",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: github-plugin",
        "Gerrit-Module: com.googlesource.gerrit.plugins.github.GuiceModule",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.github.GuiceHttpModule",
        "Gerrit-InitStep: com.googlesource.gerrit.plugins.github.InitGitHub",
        "Implementation-Title: GitHub plugin",
        "Implementation-Vendor: GerritForge",
        "Implementation-URL: http://www.gerritforge.com",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        ":lombok",
        "//plugins/github/github-oauth",
        "@axis-jaxrpc//jar",
        "@axis//jar",
        "@commons-codec//jar",
        "@commons-discovery//jar",
        "@commons-io//jar",
        "@eclipse-mylyn-github//jar",
        "@github-api//jar",
        "@velocity//jar",
    ],
)
