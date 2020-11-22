load("//tools/bzl:maven_jar.bzl", "maven_jar")
load("@bazel_tools//tools/build_defs/repo:java.bzl", "java_import_external")

def external_plugin_deps():
    maven_jar(
        name = "github-api",
        artifact = "org.kohsuke:github-api:1.116",
        sha1 = "8ac15c8aa5ade37af6144bf3b258069c00d4abfc",
    )

    maven_jar(
        name = "axis",
        artifact = "org.apache.axis:axis:1.4",
        sha1 = "94a9ce681a42d0352b3ad22659f67835e560d107",
        attach_source = False,
    )

    maven_jar(
        name = "axis-jaxrpc",
        artifact = "org.apache.axis:axis-jaxrpc:1.4",
        sha1 = "b393f1f0c0d95b68c86d0b1ab2e687bb71f3c075",
        attach_source = False,
    )

    maven_jar(
        name = "eclipse-mylyn-github",
        artifact = "org.eclipse.mylyn.github:org.eclipse.egit.github.core:1.3.1",
        sha1 = "80e433070d05be089b04414e3e452afafae4753d",
    )

    maven_jar(
        name = "commons-discovery",
        artifact = "commons-discovery:commons-discovery:20040218.194635",
        sha1 = "f7c991b9d4552670c26dc4778c5a8eb9816041e1",
        attach_source = False,
    )

    maven_jar(
        name = "velocity",
        artifact = "org.apache.velocity:velocity:1.7",
        sha1 = "2ceb567b8f3f21118ecdec129fe1271dbc09aa7a",
    )

    maven_jar(
        name = "lombok",
        artifact = "org.projectlombok:lombok:1.18.16",
        sha1 = "06dc192c7f93ec1853f70d59d8a6dcf94eb42866",
    )

    maven_jar(
        name = "bridge-method-injector",
        artifact = "com.infradna.tool:bridge-method-injector:1.18",
        sha1 = "5759fabc12d4e60af62c869ba670484e363765d3",
    )

    maven_jar(
        name = "bridge-method-annotation",
        artifact = "com.infradna.tool:bridge-method-annotation:1.18",
        sha1 = "72c49d5dddf247b7c1019992e40d44f1bc3e71bd",
    )
