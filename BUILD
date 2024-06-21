load("//tools/bzl:genrule2.bzl", "genrule2")

genrule(
    name = "github",
    srcs = [
        ":github-plugin",
        ":github-oauth",
    ],
    outs = ["github.zip"],
    cmd = "zip -o $@ $(SRCS)",
)

genrule(
    name = "github-oauth",
    srcs = ["//plugins/github/github-oauth:github-oauth_deploy.jar"],
    outs = ["github-oauth.jar"],
    cmd = "cp $< $@",
)

genrule(
    name = "github-plugin",
    srcs = ["//plugins/github/github-plugin"],
    outs = ["github-plugin.jar"],
    cmd = "cp $< $@",
)
