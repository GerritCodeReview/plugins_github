load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "github-plugin",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    manifest_entries = [
        "Gerrit-PluginName: github-plugin",
        "Gerrit-Module: com.googlesource.gerrit.plugins.github.GuiceModule",
        "Gerrit-HttpModule:com.googlesource.gerrit.plugins.github.GuiceHttpModule",
        "Gerrit-InitStep: com.googlesource.gerrit.plugins.github.InitGitHub",
        "Implementation-Title: Gerrit Code Review - GitHub plugin",
        "Implementation-URL: http://www.gerritforge.com",
    ],
)
