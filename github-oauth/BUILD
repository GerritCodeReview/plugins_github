load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "github-oauth",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    manifest_entries = [
        "Gerrit-PluginName: github-oauth",
        "Implementation-Title: Gerrit Code Review - GitHub OAuth login",
        "Implementation-URL: http://www.gerritforge.com",
    ],
    deps = [
        "@github_api//jar",
    ],
)
