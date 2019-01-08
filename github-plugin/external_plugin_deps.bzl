load("//tools/bzl:maven_jar.bzl", "MAVEN_LOCAL", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "javax-servlet-api",
        artifact = "javax.servlet:javax.servlet-api:3.0.1",
        sha1 = "6bf0ebb7efd993e222fc1112377b5e92a13b38dd",
    )

    maven_jar(
        name = "axis",
        artifact = "axis:axis:1.4",
        sha1 = "94a9ce681a42d0352b3ad22659f67835e560d107",
    )

    maven_jar(
        name = "axis-jaxrpc",
        artifact = "axis:axis-jaxrpc:1.4",
        sha1 = "b393f1f0c0d95b68c86d0b1ab2e687bb71f3c075",
        attach_source = False,
    )

    maven_jar(
        name = "mylyn-github",
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
