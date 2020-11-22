load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS_NEVERLINK",
)

java_library(
    name = "github-oauth",
    srcs = glob(["src/main/java/**/*.java"]),
    visibility = ["//visibility:public"],
    deps = PLUGIN_DEPS_NEVERLINK + [
        "@bridge-method-annotation//jar",
        "@bridge-method-injector//jar",
        "@github-api//jar",
        "//lib:servlet-api",
    ],
)
