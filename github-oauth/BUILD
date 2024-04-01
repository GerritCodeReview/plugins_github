load(
    "//plugins/github:java_library_without_header_compilation.bzl",
    "java_library_without_header_compilation",
)
load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_DEPS_NEVERLINK")

java_binary(
    name = "github-oauth",
    main_class = "Dummy",
    runtime_deps = [":github-oauth-lib"],
)

java_library(
    name = "github-oauth-lib-neverlink",
    neverlink = True,
    visibility = ["//visibility:public"],
    exports = [":github-oauth-lib"],
)

java_library_without_header_compilation(
    name = "github-oauth-lib",
    dep = ":github-oauth-lib-impl",
    visibility = ["//visibility:public"],
)

java_library(
    name = "github-oauth-lib-impl",
    srcs = glob(["src/main/java/**/*.java"]),
    deps = PLUGIN_DEPS_NEVERLINK + [
        "//lib:servlet-api",
        "//plugins/github:lombok",
        "@bridge-method-annotation//jar",
        "@bridge-method-injector//jar",
        "@github-api//jar",
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
