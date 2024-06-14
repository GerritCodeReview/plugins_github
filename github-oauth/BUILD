load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_DEPS_NEVERLINK")

java_binary(
    name = "github-oauth",
    main_class = "Dummy",
    visibility = ["//visibility:public"],
    runtime_deps = [":github-oauth-lib"],
)

java_library(
    name = "github-oauth-lib",
    srcs = glob(["src/main/java/**/*.java"]),
    visibility = ["//visibility:public"],
    deps = PLUGIN_DEPS_NEVERLINK + [
        "//lib:servlet-api",
        "@bridge-method-annotation//jar",
        "@bridge-method-injector//jar",
        "@commons-io//jar",
        "@github-api//jar",
        "@jackson-annotations//jar",
        "@jackson-core//jar",
        "@jackson-databind//jar",
        "@org-ow2-asm-commons//jar",
        "@org-ow2-asm-tree//jar",
        "@org-ow2-asm//jar",
    ],
)

junit_tests(
    name = "github-oauth_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["github"],
    deps = PLUGIN_DEPS + [
        ":github-oauth-lib",
    ],
)
