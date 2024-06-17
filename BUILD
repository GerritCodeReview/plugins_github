genrule(
    name = "github",
    srcs = [
        ":github-plugin",
        ":github-plugin-version",
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
    name = "github-plugin-version",
    srcs = [":github-plugin"],
    outs = ["github-plugin.jar-version"],
    cmd = "tar xvf $< META-INF/MANIFEST.MF; cat META-INF/MANIFEST.MF | grep Implementation-Version | cut -d ':' -f 2 | xargs > $@",
)

genrule(
    name = "github-plugin",
    srcs = ["//plugins/github/github-plugin"],
    outs = ["github-plugin.jar"],
    cmd = "cp $< $@",
)
