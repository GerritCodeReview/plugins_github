load(
    "//plugins/github:java_library_without_header_compilation.bzl",
    "java_library_without_header_compilation",
)
load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_DEPS_NEVERLINK", "PLUGIN_TEST_DEPS", "gerrit_plugin")

SOURCES_WITH_LOMBOK_USAGE = [
    "src/main/java/com/googlesource/gerrit/plugins/github/git/GitHubRepository.java",
    "src/main/java/com/googlesource/gerrit/plugins/github/GitHubURL.java",
    "src/main/java/com/googlesource/gerrit/plugins/github/git/GitHubUser.java",
    "src/main/java/com/googlesource/gerrit/plugins/github/group/GitHubGroup.java",
]

gerrit_plugin(
    name = "github-plugin",
    srcs = glob(
        ["src/main/java/**/*.java"],
    ),
    dir_name = "github",
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
        "//plugins/github/github-oauth:github-oauth-lib-neverlink",
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

junit_tests(
    name = "github-plugin_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["github"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":github-plugin__plugin",
        "//javatests/com/google/gerrit/util/http/testutil",
        "//plugins/github/github-oauth:github-oauth-lib",
        "@commons-io//jar",
    ],
)

java_plugin(
    name = "lombok_plugin",
    generates_api = True,
    processor_class = "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
    deps = ["@lombok//jar"],
)

java_library(
    name = "lombok",
    exported_plugins = [":lombok_plugin"],
    neverlink = True,
    exports = ["@lombok//jar"],
)
