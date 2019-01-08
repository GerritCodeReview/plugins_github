load("//tools/bzl:genrule2.bzl", "genrule2")

genrule2(
    name = "all",
    srcs = [
        "//plugins/github/github-oauth",
        "//plugins/github/github-plugin",
    ],
    outs = ["all.zip"],
    cmd = " && ".join([
        "cp $(SRCS) $$TMP",
        "cd $$TMP",
        "zip -qr $$ROOT/$@ .",
    ]),
)

java_library(
    name = "github_classpath_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    #exports = [
    #    "//github-oauth:__plugin",
    #    "//owners-autoassign:owners-autoassign__plugin",
    #    "//owners-common:owners-common__plugin",
    #],
)
