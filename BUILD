load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "github",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    manifest_entries = [
        "Gerrit-PluginName: its-bugzilla",
        "Gerrit-Module: com.googlesource.gerrit.plugins.its.bugzilla.BugzillaModule",
        "Gerrit-InitStep: com.googlesource.gerrit.plugins.its.bugzilla.InitBugzilla",
        "Gerrit-ReloadMode: reload",
        "Implementation-Title: Bugzilla ITS Plugin",
        "Implementation-URL: https://www.wikimediafoundation.org",
    ],
    deps = [
        "//plugins/its-base",
        "//plugins/its-bugzilla/lib:j2bugzilla",
    ],
)
