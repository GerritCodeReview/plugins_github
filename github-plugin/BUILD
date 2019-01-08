load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "github-plugin",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: github-plugin",
        "Gerrit-InitStep: com.googlesource.gerrit.plugins.github.InitGitHub",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = [
        "@github-oauth//jar",
        "@javax-servlet//jar",
        "@axis//jar",
        "@axis-jaxrpc//jar",
        "@mylyn-github//jar",
        "@gson//jar",
        "@httpclient//jar",
        "@mail//jar",
        "@commons-discovery//jar",
        "@velocity//jar",
    ],
)
