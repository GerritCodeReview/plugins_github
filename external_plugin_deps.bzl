load("@bazel_tools//tools/build_defs/repo:java.bzl", "java_import_external")
load("//tools/bzl:maven_jar.bzl", "maven_jar")

JENKINS = "JENKINS:"
ECLIPSE_EGIT = "ECLIPSE_EGIT:"

def external_plugin_deps():
    maven_jar(
        name = "github-api",
        artifact = "org.kohsuke:github-api:1.316",
        sha1 = "90ea530f3aeceb46be27b924ae25b4b7b2388c9d",
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
        artifact = "org.eclipse.mylyn.github:org.eclipse.egit.github.core:6.1.0.202203080745-r",
        repository = ECLIPSE_EGIT,
        sha1 = "a0bc7ce9f17e2d41bbfbf08e4bc63c3ae0ec15b7",
        attach_source = False,
    )

    maven_jar(
        name = "commons-discovery",
        artifact = "commons-discovery:commons-discovery:0.5",
        sha1 = "3a8ac816bbe02d2f88523ef22cbf2c4abd71d6a8",
        attach_source = False,
    )

    maven_jar(
        name = "velocity",
        artifact = "org.apache.velocity:velocity-engine-core:2.3",
        sha1 = "e2133b723d0e42be74880d34de6bf6538ea7f915",
    )

    maven_jar(
        name = "lombok",
        artifact = "org.projectlombok:lombok:1.18.32",
        sha1 = "17d46b3e205515e1e8efd3ee4d57ce8018914163",
    )

    maven_jar(
        name = "com-sun-mail",
        artifact = "com.sun.mail:javax.mail:1.6.2",
        sha1 = "935151eb71beff17a2ffac15dd80184a99a0514f",
    )

    maven_jar(
        name = "javax-activation",
        artifact = "javax.activation:activation:1.1",
        sha1 = "e6cb541461c2834bdea3eb920f1884d1eb508b50",
    )

    maven_jar(
        name = "jackson-core",
        artifact = "com.fasterxml.jackson.core:jackson-core:2.15.2",
        sha1 = "a6fe1836469a69b3ff66037c324d75fc66ef137c",
    )

    maven_jar(
        name = "jackson-databind",
        artifact = "com.fasterxml.jackson.core:jackson-databind:2.15.2",
        sha1 = "9353b021f10c307c00328f52090de2bdb4b6ff9c",
    )

    maven_jar(
        name = "jackson-annotations",
        artifact = "com.fasterxml.jackson.core:jackson-annotations:2.15.2",
        sha1 = "4724a65ac8e8d156a24898d50fd5dbd3642870b8",
    )

    maven_jar(
        name = "bridge-method-injector",
        artifact = "com.infradna.tool:bridge-method-injector:1.29",
        repository = JENKINS,
        sha1 = "5b6c616c7a6e04beb4178327d616af4f1bbe88da",
    )

    maven_jar(
        name = "bridge-method-annotation",
        artifact = "com.infradna.tool:bridge-method-annotation:1.29",
        repository = JENKINS,
        sha1 = "55dd67d0578d107697803a95cb9c235a9bd83ec1",
    )
